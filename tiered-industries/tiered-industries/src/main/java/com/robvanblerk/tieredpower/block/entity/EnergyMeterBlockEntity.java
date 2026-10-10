package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.block.EnergyMeterBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Sits in a cable line: power goes in the left side and out the right (looking at the screen). Shows FE/t and a running
 * total on its screen. A redstone signal stops the flow, so it doubles as a power switch. Comparator: stronger the more flows.
 */
public class EnergyMeterBlockEntity extends BlockEntity {
	public static final int BUFFER = 1_000_000, AVERAGE_TICKS = 20;

	private final ModEnergyStorage buffer = new ModEnergyStorage(BUFFER, BUFFER, BUFFER, this::setChanged);
	private long counted, total;
	private int flow, peak, ticks;
	private boolean blocked;

	/** The input face: takes power in, never gives it out. */
	private final IEnergyStorage input = new IEnergyStorage() {
		@Override public int receiveEnergy(int max, boolean sim) { return blocked ? 0 : buffer.receiveEnergy(max, sim); }
		@Override public int extractEnergy(int max, boolean sim) { return 0; }
		@Override public int getEnergyStored() { return buffer.getEnergyStored(); }
		@Override public int getMaxEnergyStored() { return BUFFER; }
		@Override public boolean canExtract() { return false; }
		@Override public boolean canReceive() { return true; }
	};

	/** The output face: cable networks can pull from it; everything taken is counted. */
	private final IEnergyStorage output = new IEnergyStorage() {
		@Override public int receiveEnergy(int max, boolean sim) { return 0; }
		@Override public int extractEnergy(int max, boolean sim) { return blocked ? 0 : take(max, sim); }
		@Override public int getEnergyStored() { return buffer.getEnergyStored(); }
		@Override public int getMaxEnergyStored() { return BUFFER; }
		@Override public boolean canExtract() { return true; }
		@Override public boolean canReceive() { return false; }
	};

	private LazyOptional<IEnergyStorage> inputCap = LazyOptional.of(() -> input);
	private LazyOptional<IEnergyStorage> outputCap = LazyOptional.of(() -> output);

	public EnergyMeterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ENERGY_METER.get(), pos, state);
	}

	private int take(int max, boolean simulate) {
		int n = Math.min(max, buffer.getEnergyStored());
		if (!simulate && n > 0) {
			buffer.removeInternal(n);
			counted += n;
		}
		return n;
	}

	public int getFlow() { return flow; }
	public long getTotal() { return total; }
	public int getPeak() { return peak; }
	public boolean isBlocked() { return blocked; }

	public void resetCounters() {
		total = 0;
		peak = 0;
		setChanged();
		sync();
	}

	/** Comparator: 0 when nothing flows, then about 2 steps per 10x more power (10 FE/t = 3, 1k = 7, 100k = 11). */
	public int comparatorSignal() {
		if (flow <= 0) return 0;
		return Math.min(15, 1 + (int) Math.floor(Math.log10(flow) * 2));
	}

	public static void tick(Level level, BlockPos pos, BlockState state, EnergyMeterBlockEntity be) {
		boolean nowBlocked = level.hasNeighborSignal(pos);
		if (nowBlocked != be.blocked) {
			be.blocked = nowBlocked;
			be.sync();
		}
		if (!be.blocked && be.buffer.getEnergyStored() > 0) {
			Direction out = EnergyMeterBlock.outputSide(state);
			IEnergyStorage target = EnergyUtil.neighbour(level, pos, out);
			if (target != null && target.canReceive()) {
				int accepted = target.receiveEnergy(be.buffer.getEnergyStored(), false);
				be.take(accepted, false);
			}
		}
		if (++be.ticks >= AVERAGE_TICKS) {
			int oldSignal = be.comparatorSignal();
			int newFlow = (int) Math.min(Integer.MAX_VALUE, be.counted / AVERAGE_TICKS);
			be.total += be.counted;
			be.counted = 0;
			be.ticks = 0;
			be.peak = Math.max(be.peak, newFlow);
			boolean changed = newFlow != be.flow;
			be.flow = newFlow;
			be.setChanged();
			if (changed || be.total > 0) be.sync();
			if (be.comparatorSignal() != oldSignal) level.updateNeighbourForOutputSignal(pos, state.getBlock());
		}
	}

	private void sync() {
		if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY && side != null) {
			if (side == EnergyMeterBlock.inputSide(getBlockState())) return inputCap.cast();
			if (side == EnergyMeterBlock.outputSide(getBlockState())) return outputCap.cast();
			return LazyOptional.empty();
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		inputCap.invalidate();
		outputCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		inputCap = LazyOptional.of(() -> input);
		outputCap = LazyOptional.of(() -> output);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", buffer.getEnergyStored());
		writeDisplay(tag);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		buffer.setEnergy(tag.getInt("energy"));
		readDisplay(tag);
	}

	private void writeDisplay(CompoundTag tag) {
		tag.putLong("total", total);
		tag.putInt("flow", flow);
		tag.putInt("peak", peak);
		tag.putBoolean("blocked", blocked);
	}

	private void readDisplay(CompoundTag tag) {
		total = tag.getLong("total");
		flow = tag.getInt("flow");
		peak = tag.getInt("peak");
		blocked = tag.getBoolean("blocked");
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		writeDisplay(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		readDisplay(tag);
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
