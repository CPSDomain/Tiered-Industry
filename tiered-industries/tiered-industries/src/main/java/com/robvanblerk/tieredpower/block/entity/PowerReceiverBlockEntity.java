package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

/**
 * Receives power from the Power Transmitter it's linked to (with a Power Linker) and pushes it into cables and machines
 * next to it. Works across dimensions with a 10% loss; both ends' chunks must be loaded.
 */
public class PowerReceiverBlockEntity extends BlockEntity {
	public static final int CAPACITY = 1_000_000, RATE = 64_000, CROSS_DIMENSION_LOSS_PERCENT = 10;
	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 0, RATE, this::setChanged);
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> energy);
	private @Nullable BlockPos linkPos;
	private @Nullable ResourceKey<Level> linkDim;
	private int status; // 0 not linked, 1 receiving, 2 transmitter not loaded / gone
	private int received;

	public PowerReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POWER_RECEIVER.get(), pos, state);
	}

	public void link(ResourceKey<Level> dim, BlockPos pos) {
		linkDim = dim;
		linkPos = pos;
		setChanged();
	}

	public @Nullable BlockPos getLinkPos() { return linkPos; }
	public @Nullable ResourceKey<Level> getLinkDim() { return linkDim; }
	public int getStatus() { return status; }
	public int getReceived() { return received; }

	public static void tick(Level level, BlockPos pos, BlockState state, PowerReceiverBlockEntity be) {
		be.received = 0;
		if (be.linkPos == null || be.linkDim == null || !(level instanceof ServerLevel server)) {
			be.status = 0;
		} else {
			ServerLevel there = server.getServer().getLevel(be.linkDim);
			if (there == null || !there.isLoaded(be.linkPos) || !(there.getBlockEntity(be.linkPos) instanceof PowerTransmitterBlockEntity t)) {
				be.status = 2;
			} else {
				be.status = 1;
				boolean crossDim = there != level;
				int space = be.energy.getMaxEnergyStored() - be.energy.getEnergyStored();
				int want = Math.min(RATE, crossDim ? space * 100 / (100 - CROSS_DIMENSION_LOSS_PERCENT) : space);
				int got = t.take(want);
				int arrive = crossDim ? got * (100 - CROSS_DIMENSION_LOSS_PERCENT) / 100 : got;
				if (arrive > 0) be.energy.addInternal(arrive);
				be.received = arrive;
			}
		}
		for (Direction d : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, d), RATE);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> c, @Nullable Direction side) {
		if (c == ForgeCapabilities.ENERGY) return cap.cast();
		return super.getCapability(c, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); cap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); cap = LazyOptional.of(() -> energy); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
		if (linkPos != null && linkDim != null) {
			tag.putLong("linkPos", linkPos.asLong());
			tag.putString("linkDim", linkDim.location().toString());
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		ResourceLocation dim = tag.contains("linkPos") ? ResourceLocation.tryParse(tag.getString("linkDim")) : null;
		if (dim != null) {
			linkPos = BlockPos.of(tag.getLong("linkPos"));
			linkDim = ResourceKey.create(Registries.DIMENSION, dim);
		}
	}
}
