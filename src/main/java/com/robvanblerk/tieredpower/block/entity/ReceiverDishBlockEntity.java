package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.space.SatelliteData;

/**
 * Collects power beamed down by one Solar Satellite in orbit (right-click to claim one of yours): about 100,000 FE/t,
 * day and night, in any weather. Holds 10,000,000 FE and pushes power out of every side.
 */
public class ReceiverDishBlockEntity extends BlockEntity {
	public static final int CAPACITY = 10_000_000, MAX_PUSH = 1_000_000;
	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 0, MAX_PUSH, this::setChanged);
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> energy);
	private int satelliteId = -1, generating;

	public ReceiverDishBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.RECEIVER_DISH.get(), pos, state);
	}

	public int getSatelliteId() { return satelliteId; }
	public int getGenerating() { return generating; }

	private String dim() {
		return level == null ? "" : level.dimension().location().toString();
	}

	public @Nullable SatelliteData.Satellite claim(net.minecraft.world.entity.player.Player player) {
		if (!(level instanceof ServerLevel s)) return null;
		var sat = SatelliteData.get(s.getServer()).claim(dim(), worldPosition, player.getUUID(), "solar");
		satelliteId = sat == null ? -1 : sat.id;
		setChanged();
		return sat;
	}

	public void release() {
		if (level instanceof ServerLevel s) SatelliteData.get(s.getServer()).release(dim(), worldPosition);
		satelliteId = -1;
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ReceiverDishBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		if (level.getGameTime() % 20 == 0) {
			var sat = SatelliteData.get(server.getServer()).forDish(be.dim(), pos);
			be.satelliteId = sat == null ? -1 : sat.id;
		}
		be.generating = 0;
		if (be.satelliteId >= 0) {
			int out = com.robvanblerk.tieredpower.Config.gen(com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.SATELLITE_OUTPUT));
			int add = Math.min(out, be.energy.getMaxEnergyStored() - be.energy.getEnergyStored());
			if (add > 0) { be.energy.addInternal(add); be.generating = add; }
		}
		for (Direction d : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, d), MAX_PUSH);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> c, @Nullable Direction side) {
		if (c == ForgeCapabilities.ENERGY) return cap.cast();
		return super.getCapability(c, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); cap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); cap = LazyOptional.of(() -> energy); }
	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("energy", energy.getEnergyStored()); }
	@Override public void load(CompoundTag tag) { super.load(tag); energy.setEnergy(tag.getInt("energy")); }
}
