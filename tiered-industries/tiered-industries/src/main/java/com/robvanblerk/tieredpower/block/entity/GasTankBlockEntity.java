package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** A pressure tank for one gas: 64,000 mB, pipes in and out on any side; keeps its gas when broken. */
public class GasTankBlockEntity extends BlockEntity {
	public static final int CAPACITY = 64_000;
	public final FluidTank tank = new FluidTank(CAPACITY, f -> f.getFluid().getFluidType().isLighterThanAir()) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private LazyOptional<IFluidHandler> cap = LazyOptional.of(() -> tank);

	public GasTankBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GAS_TANK.get(), pos, state);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> c, @Nullable Direction side) {
		if (c == ForgeCapabilities.FLUID_HANDLER) return cap.cast();
		return super.getCapability(c, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); cap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); cap = LazyOptional.of(() -> tank); }
	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("tank", tank.writeToNBT(new CompoundTag())); }
	@Override public void load(CompoundTag tag) { super.load(tag); tank.readFromNBT(tag.getCompound("tank")); }
}
