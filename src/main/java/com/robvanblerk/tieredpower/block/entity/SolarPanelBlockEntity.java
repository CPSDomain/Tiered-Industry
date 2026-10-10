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

import com.robvanblerk.tieredpower.block.SolarPanelBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class SolarPanelBlockEntity extends BlockEntity {
	private final int output;
	public final ModEnergyStorage energy;
	private LazyOptional<IEnergyStorage> cap;
	private int generating;

	public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
		this.output = state.getBlock() instanceof SolarPanelBlock b ? b.getOutput() : 12;
		this.energy = new ModEnergyStorage(output * 200, 0, output * 4, this::setChanged);
		this.cap = LazyOptional.of(() -> energy);
	}

	public int getGenerating() {
		return generating;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
		if (level.getGameTime() % 20 == 0) {
			BlockPos above = pos.above();
			if (!level.dimensionType().hasSkyLight() || !level.isDay() || !level.canSeeSky(above)) be.generating = 0;
			else be.generating = level.isRainingAt(above) || level.isRaining() ? be.output / 2 : be.output;
		}
		if (be.generating > 0) be.energy.addInternal(com.robvanblerk.tieredpower.Config.gen(be.generating));
		for (Direction dir : Direction.values()) {
			if (dir == Direction.UP || be.energy.getEnergyStored() <= 0) continue;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), be.output * 4);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
		if (capability == ForgeCapabilities.ENERGY && side != Direction.UP) return cap.cast();
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
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
	}
}
