package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Unlimited power for testing: pushes as much as its neighbours will take, every tick, and never runs dry. */
public class CreativeEnergyCellBlockEntity extends BlockEntity {
	private final IEnergyStorage infinite = new IEnergyStorage() {
		@Override public int receiveEnergy(int max, boolean sim) { return 0; }
		@Override public int extractEnergy(int max, boolean sim) { return max; }
		@Override public int getEnergyStored() { return Integer.MAX_VALUE; }
		@Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
		@Override public boolean canExtract() { return true; }
		@Override public boolean canReceive() { return false; }
	};
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> infinite);

	public CreativeEnergyCellBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CREATIVE_ENERGY_CELL.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CreativeEnergyCellBlockEntity be) {
		for (Direction dir : Direction.values()) {
			IEnergyStorage target = EnergyUtil.neighbour(level, pos, dir);
			if (target != null && target.canReceive()) target.receiveEnergy(Integer.MAX_VALUE, false);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> c, @Nullable Direction side) {
		if (c == ForgeCapabilities.ENERGY) return cap.cast();
		return super.getCapability(c, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		cap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		cap = LazyOptional.of(() -> infinite);
	}
}
