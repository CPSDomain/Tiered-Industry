package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.ItemKey;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * Shows how many of one item the storage network holds, live, on its face. A comparator reads the stock: 0 when there
 * are none, then one more per doubling (1 = 1, 2 = 2, 4 = 3 ... 16,384+ = 15) - put a redstone torch after it for a
 * low-stock alarm.
 */
public class StorageMonitorBlockEntity extends BlockEntity {
	private ItemStack shown = ItemStack.EMPTY;
	private long count = -1; // -1 = not connected to a powered network
	private int ticks;

	public StorageMonitorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STORAGE_MONITOR.get(), pos, state);
	}

	public ItemStack getShown() { return shown; }
	public long getCount() { return count; }

	public void setShown(ItemStack stack) {
		shown = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
		ticks = 9; // refresh on the next tick
		sync();
	}

	public int comparatorSignal() {
		if (shown.isEmpty() || count <= 0) return 0;
		return Math.min(15, 1 + (63 - Long.numberOfLeadingZeros(count)));
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StorageMonitorBlockEntity be) {
		if (++be.ticks % 10 != 0) return;
		long now = -1;
		if (!be.shown.isEmpty()) {
			StorageControllerBlockEntity c = StorageNetwork.findController(level, pos);
			if (c != null && c.isOnline() && c.getNetwork() != null) now = c.getNetwork().count(new ItemKey(be.shown));
		}
		if (now != be.count) {
			int before = be.comparatorSignal();
			be.count = now;
			be.sync();
			if (be.comparatorSignal() != before) level.updateNeighbourForOutputSignal(pos, state.getBlock());
		}
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	private void write(CompoundTag tag) {
		if (!shown.isEmpty()) tag.put("Shown", shown.save(new CompoundTag()));
		tag.putLong("Count", count);
	}

	private void read(CompoundTag tag) {
		shown = tag.contains("Shown") ? ItemStack.of(tag.getCompound("Shown")) : ItemStack.EMPTY;
		count = tag.contains("Count") ? tag.getLong("Count") : -1;
	}

	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); write(tag); }
	@Override public void load(CompoundTag tag) { super.load(tag); read(tag); }

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		write(tag);
		return tag;
	}

	@Override public void handleUpdateTag(CompoundTag tag) { read(tag); }
	@Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
