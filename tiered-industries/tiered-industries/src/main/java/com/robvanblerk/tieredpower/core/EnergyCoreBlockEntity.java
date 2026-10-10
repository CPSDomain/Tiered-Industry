package com.robvanblerk.tieredpower.core;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * The Energy Core: a huge energy store (20 billion to 100 trillion FE over five tiers, held as a long). It has no
 * connections of its own - Energy Pylons within 8 blocks move power in and out of it. Keeps its energy when broken.
 */
public class EnergyCoreBlockEntity extends BlockEntity {
	public static final long[] CAPACITY = {20_000_000_000L, 100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L, 100_000_000_000_000L};
	/** FE/t each pylon can move, by tier. */
	public static final int[] PYLON_RATE = {1_000_000, 4_000_000, 16_000_000, 64_000_000, 256_000_000};
	/** Orb colour by tier (blue, green, gold, red, violet). */
	public static final int[] COLOUR = {0x3FA8FF, 0x48E070, 0xFFC230, 0xFF4848, 0xC060FF};
	public static final int MAX_TIER = 5;

	private int tier = 1;
	private long stored, inThisTick, outThisTick, rateIn, rateOut;
	private int lastSignal = -1, lastSyncedFill = -1;

	public EnergyCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ENERGY_CORE.get(), pos, state);
	}

	public int getTier() { return tier; }
	public long getStored() { return stored; }
	public long getCapacity() { return CAPACITY[tier - 1]; }
	public int getPylonRate() { return PYLON_RATE[tier - 1]; }
	public long getRateIn() { return rateIn; }
	public long getRateOut() { return rateOut; }

	/** 0..1 */
	public float fill() {
		return getCapacity() <= 0 ? 0 : (float) ((double) stored / getCapacity());
	}

	public boolean upgrade() {
		if (tier >= MAX_TIER) return false;
		tier++;
		sync();
		return true;
	}

	public int insert(int amount, boolean simulate) {
		int n = (int) Math.min(amount, getCapacity() - stored);
		if (n <= 0) return 0;
		if (!simulate) { stored += n; inThisTick += n; setChanged(); }
		return n;
	}

	public int extract(int amount, boolean simulate) {
		int n = (int) Math.min(amount, stored);
		if (n <= 0) return 0;
		if (!simulate) { stored -= n; outThisTick += n; setChanged(); }
		return n;
	}

	public int getComparatorSignal() {
		return stored <= 0 ? 0 : 1 + (int) (fill() * 14);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, EnergyCoreBlockEntity be) {
		be.rateIn = be.inThisTick;
		be.rateOut = be.outThisTick;
		be.inThisTick = 0;
		be.outThisTick = 0;
		int signal = be.getComparatorSignal();
		if (signal != be.lastSignal) {
			be.lastSignal = signal;
			level.updateNeighbourForOutputSignal(pos, state.getBlock());
		}
		int fill = (int) (be.fill() * 200); // tell clients when the orb visibly changes
		if (fill != be.lastSyncedFill && level.getGameTime() % 10 == 0) {
			be.lastSyncedFill = fill;
			be.sync();
		}
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("tier", tier);
		tag.putLong("stored", stored);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		tier = Math.max(1, Math.min(MAX_TIER, tag.contains("tier") ? tag.getInt("tier") : 1));
		stored = Math.max(0, Math.min(getCapacity(), tag.getLong("stored")));
	}

	@Override public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }
	@Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	@Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1); }
}
