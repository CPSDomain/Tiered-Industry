package com.robvanblerk.tieredpower.greenhouse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Sprinkler (sprinkler = true): uses 20 mB of water a second to keep farmland below it wet and give the plants a 15%
 * chance of an extra growth tick each second, with a splash of water.
 * Grow Lamp (sprinkler = false): uses 40 FE/t to give full light and a 10% chance of extra growth each second.
 * Both cover a 9x9 area up to 6 blocks below them.
 */
public class GreenhouseBlockEntity extends BlockEntity {
	public static final int WATER_PER_SECOND = 20, TANK = 4_000, LAMP_FE_PER_TICK = 40, LAMP_CAPACITY = 20_000;
	private final boolean sprinkler;
	public final FluidTank water = new FluidTank(TANK, f -> f.getFluid().isSame(Fluids.WATER)) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	public final ModEnergyStorage energy = new ModEnergyStorage(LAMP_CAPACITY, 1_000, 0, this::setChanged);
	private LazyOptional<IFluidHandler> waterCap = LazyOptional.of(() -> water);
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
	private int plants;
	private boolean running;

	public GreenhouseBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GREENHOUSE.get(), pos, state);
		this.sprinkler = state.getBlock() == com.robvanblerk.tieredpower.registry.ModBlocks.SPRINKLER.get();
	}

	public boolean isSprinkler() { return sprinkler; }
	public int getPlants() { return plants; }
	public boolean isRunning() { return running; }

	/** A sprinkler with water in it (running, or about to). */
	public boolean hasWater() { return sprinkler && (running || water.getFluidAmount() > 0); }

	public static void tick(Level level, BlockPos pos, BlockState state, GreenhouseBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		if (be.sprinkler) {
			if (level.getGameTime() % 20 != 0) return;
			be.running = be.water.getFluidAmount() >= WATER_PER_SECOND;
			if (!be.running) return;
			be.water.drain(WATER_PER_SECOND, IFluidHandler.FluidAction.EXECUTE);
			GrowthBoost.hydrate(server, pos);
			be.plants = GrowthBoost.boost(server, pos, 0.15f);
			server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() - 0.2, pos.getZ() + 0.5, 30, 1.8, 0.1, 1.8, 0.2);
		} else {
			boolean on = be.energy.getEnergyStored() >= LAMP_FE_PER_TICK;
			if (on) be.energy.removeInternal(LAMP_FE_PER_TICK);
			if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != on)
				level.setBlock(pos, state.setValue(BlockStateProperties.LIT, on), Block.UPDATE_ALL);
			be.running = on;
			if (on && level.getGameTime() % 20 == 0) be.plants = GrowthBoost.boost(server, pos, 0.10f);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (sprinkler && cap == ForgeCapabilities.FLUID_HANDLER) return waterCap.cast();
		if (!sprinkler && cap == ForgeCapabilities.ENERGY) return energyCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); waterCap.invalidate(); energyCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); waterCap = LazyOptional.of(() -> water); energyCap = LazyOptional.of(() -> energy); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("water", water.writeToNBT(new CompoundTag()));
		tag.putInt("energy", energy.getEnergyStored());
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		water.readFromNBT(tag.getCompound("water"));
		energy.setEnergy(tag.getInt("energy"));
	}
}
