package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
 * Sends power wirelessly to every Power Receiver linked to it, any distance and any dimension. Takes FE from cables on
 * any side; hands out up to 64,000 FE/t in total, shared between its receivers.
 */
public class PowerTransmitterBlockEntity extends BlockEntity {
	public static final int CAPACITY = 1_000_000, RATE = 64_000;
	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, RATE, 0, this::setChanged);
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> energy);
	private long budgetTick = -1;
	private int budget, sentLastTick, sentThisTick, receivers, receiversThisTick;

	public PowerTransmitterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POWER_TRANSMITTER.get(), pos, state);
	}

	/** A receiver asks for up to 'want' FE this tick; returns what it gets (shared out of the 64,000 FE/t budget). */
	public int take(int want) {
		if (level == null) return 0;
		long now = level.getGameTime();
		if (budgetTick != now) {
			budgetTick = now;
			sentLastTick = sentThisTick;
			receivers = receiversThisTick;
			sentThisTick = 0;
			receiversThisTick = 0;
			budget = com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.TRANSMITTER_RATE);
			if (energy.getMaxReceive() < budget) energy.setMaxReceive(budget);
		}
		receiversThisTick++;
		int n = Math.min(want, Math.min(budget, energy.getEnergyStored()));
		if (n <= 0) return 0;
		energy.removeInternal(n);
		budget -= n;
		sentThisTick += n;
		return n;
	}

	public int getSentPerTick() { return sentLastTick; }
	public int getReceivers() { return receivers; }

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> c, @Nullable Direction side) {
		if (c == ForgeCapabilities.ENERGY) return cap.cast();
		return super.getCapability(c, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); cap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); cap = LazyOptional.of(() -> energy); }
	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("energy", energy.getEnergyStored()); }
	@Override public void load(CompoundTag tag) { super.load(tag); energy.setEnergy(tag.getInt("energy")); }

	public static void tick(Level level, BlockPos pos, BlockState state, PowerTransmitterBlockEntity be) {
		if (be.budgetTick < level.getGameTime() - 1) { // nobody asked last tick: show 0
			be.sentLastTick = 0;
			be.receivers = 0;
		}
	}
}
