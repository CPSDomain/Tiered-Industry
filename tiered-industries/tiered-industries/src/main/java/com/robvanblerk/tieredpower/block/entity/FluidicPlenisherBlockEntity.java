package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Fills the space below and around it with the fluid piped in: one source block every half second (1,000 mB and 100 FE
 * each), spreading outwards and downwards up to 32 blocks away - never above itself. Works with any fluid that can exist
 * as a block (water, lava, and modded liquids).
 */
public class FluidicPlenisherBlockEntity extends BlockEntity {
	public static final int CAPACITY = 50_000, MAX_INPUT = 2_000, PER_BLOCK = 100, TANK = 16_000, RANGE = 32, SEARCH = 4_096;
	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, MAX_INPUT, 0, this::setChanged);
	public final FluidTank tank = new FluidTank(TANK, f -> f.getFluid().getFluidType().canConvertToSource(f) || f.getFluid().defaultFluidState().createLegacyBlock().getBlock() != net.minecraft.world.level.block.Blocks.AIR) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
	private int ticks, placed;
	private boolean done;

	public FluidicPlenisherBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUIDIC_PLENISHER.get(), pos, state);
	}

	public int getPlaced() { return placed; }
	public boolean isDone() { return done; }

	private boolean fillable(Level level, BlockPos p) {
		BlockState s = level.getBlockState(p);
		return (s.isAir() || s.canBeReplaced()) && s.getFluidState().isEmpty();
	}

	/** The nearest empty spot reachable through open space below/around the plenisher, or null. */
	private @Nullable BlockPos next(Level level) {
		BlockPos start = worldPosition.below();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		while (!queue.isEmpty() && seen.size() < SEARCH) {
			BlockPos p = queue.poll();
			if (!seen.add(p) || p.getY() > start.getY() || p.distManhattan(start) > RANGE || !level.isLoaded(p)) continue;
			BlockState s = level.getBlockState(p);
			boolean open = s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
			if (!open) continue;
			if (fillable(level, p)) return p;
			for (Direction d : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) queue.add(p.relative(d));
		}
		return null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluidicPlenisherBlockEntity be) {
		if (++be.ticks % 10 != 0) return;
		FluidStack f = be.tank.getFluid();
		if (f.getAmount() < 1_000 || be.energy.getEnergyStored() < PER_BLOCK) return;
		BlockState fluidBlock = f.getFluid().defaultFluidState().createLegacyBlock();
		if (fluidBlock.isAir()) return;
		BlockPos target = be.next(level);
		be.done = target == null;
		if (target == null) return;
		level.setBlock(target, fluidBlock, Block.UPDATE_ALL);
		be.tank.drain(1_000, IFluidHandler.FluidAction.EXECUTE);
		be.energy.removeInternal(PER_BLOCK);
		be.placed++;
		be.setChanged();
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); energyCap.invalidate(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); energyCap = LazyOptional.of(() -> energy); fluidCap = LazyOptional.of(() -> tank); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
		tag.put("tank", tank.writeToNBT(new CompoundTag()));
		tag.putInt("placed", placed);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		tank.readFromNBT(tag.getCompound("tank"));
		placed = tag.getInt("placed");
	}
}
