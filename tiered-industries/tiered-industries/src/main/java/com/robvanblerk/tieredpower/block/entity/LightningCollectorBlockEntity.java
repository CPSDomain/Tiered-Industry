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

/**
 * Banks lightning: each strike on the vanilla Lightning Rod attached to it (on top or on a side) - or on the collector
 * itself - adds 2,500,000 FE. It feeds that out steadily at up to 20,000 FE/t. Glows briefly after a strike.
 */
public class LightningCollectorBlockEntity extends BlockEntity {
	public static final int CAPACITY = 25_000_000, MAX_PUSH = 20_000;
	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 0, MAX_PUSH, this::setChanged);
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> energy);
	private int generating;

	public LightningCollectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LIGHTNING_COLLECTOR.get(), pos, state);
	}

	public int getGenerating() {
		return generating;
	}

	public static final int PER_STRIKE = 2_500_000;
	private int glow, strikes;

	public int getStrikes() {
		return strikes;
	}

	/** A lightning strike hit our rod (called from LightningEvents). */
	public void strike() {
		if (level == null) return;
		int add = Math.min(energy.getSpace(), Config.gen(com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.LIGHTNING_FE_PER_STRIKE)));
		if (add > 0) energy.addInternal(add);
		strikes++;
		glow = 60;
		BlockState s = getBlockState();
		if (s.hasProperty(com.robvanblerk.tieredpower.block.LightningCollectorBlock.LIT))
			level.setBlock(worldPosition, s.setValue(com.robvanblerk.tieredpower.block.LightningCollectorBlock.LIT, true), 3);
		level.updateNeighbourForOutputSignal(worldPosition, s.getBlock());
		setChanged();
	}

	/** Comparator: how full it is, 0-15. */
	public int comparatorSignal() {
		int stored = energy.getEnergyStored();
		return stored <= 0 ? 0 : Math.max(1, (int) Math.floor(15.0 * stored / CAPACITY));
	}

	private void saveExtra(CompoundTag tag) {
		tag.putInt("strikes", strikes);
	}

	private void loadExtra(CompoundTag tag) {
		strikes = tag.getInt("strikes");
	}

	public static void tick(Level level, BlockPos pos, BlockState state, LightningCollectorBlockEntity be) {
		if (be.glow > 0 && --be.glow == 0 && state.getValue(com.robvanblerk.tieredpower.block.LightningCollectorBlock.LIT))
			level.setBlock(pos, state.setValue(com.robvanblerk.tieredpower.block.LightningCollectorBlock.LIT, false), 3);
		be.generating = 0;
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
