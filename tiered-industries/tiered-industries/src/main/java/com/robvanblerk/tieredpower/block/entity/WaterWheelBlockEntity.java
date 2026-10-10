package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Always-on power from moving water: 8 FE/t per side with flowing (not still) water, up to 32 FE/t. */
public class WaterWheelBlockEntity extends BlockEntity {
	public static final int CAPACITY = 20_000, MAX_PUSH = 200;
	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 0, MAX_PUSH, this::setChanged);
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> energy);
	private int generating;

	public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WATER_WHEEL.get(), pos, state);
	}

	public int getGenerating() {
		return generating;
	}

	public static final int PER_SIDE = 8;

	private void saveExtra(CompoundTag tag) {}

	private void loadExtra(CompoundTag tag) {}

	public static void tick(Level level, BlockPos pos, BlockState state, WaterWheelBlockEntity be) {
		if (level.getGameTime() % 20 == 0) {
			int flow = 0;
			for (Direction d : Direction.Plane.HORIZONTAL) {
				FluidState f = level.getFluidState(pos.relative(d));
				if (f.is(FluidTags.WATER) && !f.isSource()) flow += PER_SIDE;
			}
			be.generating = flow;
			boolean turning = flow > 0;
			if (state.hasProperty(com.robvanblerk.tieredpower.block.WaterWheelBlock.LIT) && state.getValue(com.robvanblerk.tieredpower.block.WaterWheelBlock.LIT) != turning)
				level.setBlock(pos, state.setValue(com.robvanblerk.tieredpower.block.WaterWheelBlock.LIT, turning), 3);
		}
		if (be.generating > 0 && be.energy.getSpace() > 0) be.energy.addInternal(Math.min(be.energy.getSpace(), Config.gen(be.generating)));
		for (Direction dir : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), MAX_PUSH);
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
		cap = LazyOptional.of(() -> energy);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
		saveExtra(tag);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		loadExtra(tag);
	}
}
