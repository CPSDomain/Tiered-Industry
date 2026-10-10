package com.robvanblerk.tieredpower.block.entity;

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

import com.robvanblerk.tieredpower.block.CableBlock;
import com.robvanblerk.tieredpower.energy.CableNetwork;
import com.robvanblerk.tieredpower.energy.CableTier;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * One cable segment. Each segment holds a small buffer (its tier's rate) that generators push into;
 * the actual moving of power is done by the CableNetwork all connected segments belong to.
 */
public class CableBlockEntity extends BlockEntity {
	private final CableTier tier;
	public final ModEnergyStorage energy;
	private LazyOptional<IEnergyStorage> energyCap;
	@Nullable
	private CableNetwork network;

	public CableBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CABLE.get(), pos, state);
		this.tier = state.getBlock() instanceof CableBlock cable ? cable.getTier() : CableTier.COPPER;
		int rate = tier.getTransferRate();
		this.energy = new ModEnergyStorage(rate, rate, rate, this::setChanged);
		this.energyCap = LazyOptional.of(() -> energy);
	}

	public CableTier getTier() {
		return tier;
	}

	public @Nullable CableNetwork getNetwork() {
		return network;
	}

	public void setNetwork(@Nullable CableNetwork network) {
		this.network = network;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CableBlockEntity be) {
		if (be.network == null || !be.network.isValid()) CableNetwork.build(level, be);
		if (be.network != null) be.network.tick(level);

		// Keep the visual connections correct even if a neighbour changed quietly.
		if (level.getGameTime() % 20 == 0) {
			BlockState updated = CableBlock.withConnections(state, level, pos);
			if (updated != state) level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
		}
	}

	/** A cable appeared or disappeared here: neighbouring networks must be rebuilt. */
	private void invalidateNeighbours() {
		if (level == null) return;
		if (network != null) network.invalidate();
		for (Direction dir : Direction.values()) {
			BlockPos next = worldPosition.relative(dir);
			if (!level.isLoaded(next)) continue;
			if (level.getBlockEntity(next) instanceof CableBlockEntity other && other.network != null) {
				other.network.invalidate();
			}
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide()) invalidateNeighbours();
	}

	@Override
	public void setRemoved() {
		if (level != null && !level.isClientSide()) invalidateNeighbours();
		super.setRemoved();
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energyCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		energyCap = LazyOptional.of(() -> energy);
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
