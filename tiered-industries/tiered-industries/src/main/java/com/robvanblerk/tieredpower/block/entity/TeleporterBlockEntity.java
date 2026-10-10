package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * A Teleporter pad, linked to another pad with the Teleporter Linker. Crouch on it to travel to the other pad.
 * Costs 1,000 FE + 20 FE per block in the same dimension, or 50,000 FE to another dimension (from this pad's energy).
 */
public class TeleporterBlockEntity extends BlockEntity {
	public static final int CAPACITY = 1_000_000, MAX_INPUT = 32_000;
	public static final int BASE_COST = 1_000, COST_PER_BLOCK = 20, CROSS_DIMENSION_COST = 50_000, COOLDOWN = 40;

	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, MAX_INPUT, 0, this::setChanged);
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> energy);
	@Nullable
	private BlockPos targetPos;
	@Nullable
	private ResourceLocation targetDim;
	private String name = "";

	public String getName() {
		return name.isEmpty() ? "Pad at " + worldPosition.toShortString() : name;
	}

	public void setName(String newName) {
		name = newName.length() > 32 ? newName.substring(0, 32) : newName;
		setChanged();
		register();
	}

	/** Adds/updates this pad in the world's list of pads. */
	public void register() {
		if (level instanceof ServerLevel server) {
			com.robvanblerk.tieredpower.multiblock.TeleporterRegistry.get(server.getServer()).put(server.dimension().location(), worldPosition, getName());
		}
	}

	public void unregister() {
		if (level instanceof ServerLevel server) {
			com.robvanblerk.tieredpower.multiblock.TeleporterRegistry.get(server.getServer()).remove(server.dimension().location(), worldPosition);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		register();
	}

	/** Server tick: crouching players standing on the pad travel. (Vanilla doesn't send "stepped on" while crouching.) */
	public static void tick(Level level, BlockPos pos, BlockState state, TeleporterBlockEntity be) {
		if (level.getGameTime() % 4 != 0) return;
		net.minecraft.world.phys.AABB top = new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1.2, pos.getZ() + 1);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, top)) {
			if (player.isShiftKeyDown()) be.tryTeleport(player);
		}
	}

	public TeleporterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TELEPORTER.get(), pos, state);
	}

	public void unlink() {
		targetDim = null;
		targetPos = null;
		setChanged();
	}

	public void link(ResourceLocation dim, BlockPos pos) {
		targetDim = dim;
		targetPos = pos;
		setChanged();
	}

	public boolean isLinked() {
		return targetPos != null && targetDim != null;
	}

	public @Nullable BlockPos getTargetPos() { return targetPos; }
	public @Nullable ResourceLocation getTargetDim() { return targetDim; }

	public int costTo(Level from) {
		if (!isLinked()) return 0;
		if (!from.dimension().location().equals(targetDim)) return CROSS_DIMENSION_COST;
		return BASE_COST + (int) Math.round(Math.sqrt(worldPosition.distSqr(targetPos))) * COST_PER_BLOCK;
	}

	/** Called when a player crouches on the pad. */
	public void tryTeleport(ServerPlayer player) {
		long now = player.level().getGameTime();
		CompoundTag data = player.getPersistentData();
		if (now - data.getLong("tieredpower_tp") < COOLDOWN) return;
		data.putLong("tieredpower_tp", now);

		if (!isLinked()) {
			player.displayClientMessage(Component.literal("This pad isn't linked - use a Teleporter Linker on two pads"), true);
			return;
		}
		ServerLevel target = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, targetDim));
		if (target == null) {
			player.displayClientMessage(Component.literal("The linked dimension doesn't exist any more"), true);
			return;
		}
		target.getChunk(targetPos); // make sure the destination is loaded
		if (!(target.getBlockEntity(targetPos) instanceof TeleporterBlockEntity)) {
			player.displayClientMessage(Component.literal("The linked pad is gone"), true);
			return;
		}
		int cost = costTo(player.level());
		if (energy.getEnergyStored() < cost) {
			player.displayClientMessage(Component.literal(String.format("Not enough power: needs %,d FE, has %,d FE", cost, energy.getEnergyStored())), true);
			return;
		}
		energy.removeInternal(cost);
		player.level().playSound(null, worldPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8f, 1.2f);
		player.teleportTo(target, targetPos.getX() + 0.5, targetPos.getY() + 0.25, targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
		player.getPersistentData().putLong("tieredpower_tp", target.getGameTime());
		target.playSound(null, targetPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8f, 1.2f);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
		if (capability == ForgeCapabilities.ENERGY) return cap.cast();
		return super.getCapability(capability, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		cap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		cap = LazyOptional.of(() -> energy);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
		tag.putString("name", name);
		if (isLinked()) {
			tag.put("target", NbtUtils.writeBlockPos(targetPos));
			tag.putString("targetDim", targetDim.toString());
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		name = tag.getString("name");
		if (tag.contains("target")) {
			targetPos = NbtUtils.readBlockPos(tag.getCompound("target"));
			targetDim = ResourceLocation.tryParse(tag.getString("targetDim"));
		}
	}
}
