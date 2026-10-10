package com.robvanblerk.tieredpower.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Energy Pylon: finds the nearest Energy Core within 8 blocks by itself. An Input Pylon takes power from cables and puts
 * it in the core; an Output Pylon takes it out and pushes it into everything touching it. Each moves up to the core
 * tier's pylon rate per tick, with sparks drifting to the core while it works.
 */
public class EnergyPylonBlockEntity extends BlockEntity {
	public static final int RANGE = 8;
	private final boolean input;
	private @Nullable BlockPos corePos;
	private long budgetTick = -1;
	private int budget, movedThisTick, moved, search;

	private final IEnergyStorage proxy = new IEnergyStorage() {
		private int allowance() {
			EnergyCoreBlockEntity c = core();
			if (c == null || level == null) return 0;
			if (budgetTick != level.getGameTime()) { budgetTick = level.getGameTime(); budget = c.getPylonRate(); moved = movedThisTick; movedThisTick = 0; }
			return budget;
		}

		@Override
		public int receiveEnergy(int max, boolean simulate) {
			if (!input) return 0;
			EnergyCoreBlockEntity c = core();
			int n = c == null ? 0 : c.insert(Math.min(max, allowance()), simulate);
			if (!simulate && n > 0) { budget -= n; movedThisTick += n; }
			return n;
		}

		@Override
		public int extractEnergy(int max, boolean simulate) {
			if (input) return 0;
			EnergyCoreBlockEntity c = core();
			int n = c == null ? 0 : c.extract(Math.min(max, allowance()), simulate);
			if (!simulate && n > 0) { budget -= n; movedThisTick += n; }
			return n;
		}

		@Override public int getEnergyStored() { EnergyCoreBlockEntity c = core(); return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, c.getStored()); }
		@Override public int getMaxEnergyStored() { EnergyCoreBlockEntity c = core(); return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, c.getCapacity()); }
		@Override public boolean canExtract() { return !input && core() != null; }
		@Override public boolean canReceive() { return input && core() != null; }
	};
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> proxy);

	public EnergyPylonBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ENERGY_PYLON.get(), pos, state);
		this.input = state.getBlock() == com.robvanblerk.tieredpower.registry.ModBlocks.INPUT_PYLON.get();
	}

	public boolean isInput() { return input; }
	public int getMoved() { return moved; }

	public @Nullable EnergyCoreBlockEntity core() {
		if (corePos == null || level == null) return null;
		return level.getBlockEntity(corePos) instanceof EnergyCoreBlockEntity c ? c : null;
	}

	private void findCore(Level level) {
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RANGE, -RANGE, -RANGE), worldPosition.offset(RANGE, RANGE, RANGE))) {
			if (!(level.getBlockEntity(p) instanceof EnergyCoreBlockEntity)) continue;
			double d = p.distSqr(worldPosition);
			if (d < bestD) { bestD = d; best = p.immutable(); }
		}
		corePos = best;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, EnergyPylonBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		if (be.core() == null && be.search-- <= 0) { be.findCore(level); be.search = 40; }
		EnergyCoreBlockEntity c = be.core();
		if (c == null) return;
		if (!be.input) {
			for (Direction d : Direction.values()) {
				var n = level.getBlockEntity(pos.relative(d));
				if (n instanceof EnergyPylonBlockEntity || n instanceof EnergyCoreBlockEntity) continue;
				EnergyUtil.move(be.proxy, EnergyUtil.neighbour(level, pos, d), c.getPylonRate());
			}
		}
		// sparks along the link while energy is moving
		if (level.getGameTime() % 10 == 0 && (be.moved > 0 || be.movedThisTick > 0)) {
			Vec3 a = Vec3.atCenterOf(pos), b = Vec3.atCenterOf(be.corePos);
			if (!be.input) { Vec3 t = a; a = b; b = t; }
			Vec3 step = b.subtract(a);
			for (int i = 1; i < 6; i++) {
				Vec3 p = a.add(step.scale(i / 6.0));
				server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
			}
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> c, @Nullable Direction side) {
		if (c == ForgeCapabilities.ENERGY) return cap.cast();
		return super.getCapability(c, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); cap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); cap = LazyOptional.of(() -> proxy); }
}
