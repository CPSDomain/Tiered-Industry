package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.menu.StockKeeperMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.ItemKey;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * Keeps up to 9 items in stock: every few seconds, if storage has fewer than the target and nothing is already being
 * made, it starts a crafting job for the difference.
 */
public class StockKeeperBlockEntity extends BlockEntity implements MenuProvider {
	public static final int SLOTS = 9, CHECK_INTERVAL = 100, MAX_AMOUNT = 1_000_000;
	public static final int STATUS_IDLE = 0, STATUS_STOCKED = 1, STATUS_CRAFTING = 2, STATUS_CANT = 3, STATUS_OFFLINE = 4;

	private final SimpleContainer items = new SimpleContainer(SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			StockKeeperBlockEntity.this.setChanged();
		}
	};
	private final int[] amounts = new int[SLOTS];
	private final int[] status = new int[SLOTS];
	private int ticks;

	/** 0-17: each amount as two 16-bit halves (menus sync shorts), 18-26: status per slot. */
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			if (i < 18) return (amounts[i / 2] >>> (i % 2 == 0 ? 0 : 16)) & 0xFFFF;
			return status[i - 18];
		}

		@Override public void set(int i, int v) {}
		@Override public int getCount() { return 27; }
	};

	public StockKeeperBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STOCK_KEEPER.get(), pos, state);
	}

	public SimpleContainer items() { return items; }
	public ContainerData data() { return data; }

	public void setAmount(int slot, int amount) {
		if (slot < 0 || slot >= SLOTS) return;
		amounts[slot] = Math.max(0, Math.min(MAX_AMOUNT, amount));
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, StockKeeperBlockEntity be) {
		if (++be.ticks % CHECK_INTERVAL != 0) return;
		StorageControllerBlockEntity c = StorageNetwork.findController(level, pos);
		StorageNetwork network = c == null ? null : c.getNetwork();
		for (int i = 0; i < SLOTS; i++) {
			ItemStack want = be.items.getItem(i);
			if (want.isEmpty() || be.amounts[i] <= 0) {
				be.status[i] = STATUS_IDLE;
				continue;
			}
			if (network == null) {
				be.status[i] = STATUS_OFFLINE;
				continue;
			}
			ItemKey key = new ItemKey(want);
			if (c.hasJobFor(key)) {
				be.status[i] = STATUS_CRAFTING;
				continue;
			}
			long have = network.count(key);
			if (have >= be.amounts[i]) {
				be.status[i] = STATUS_STOCKED;
				continue;
			}
			String result = c.startJob(key, be.amounts[i] - have, null);
			be.status[i] = result.startsWith("Crafting") ? STATUS_CRAFTING : STATUS_CANT;
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ListTag list = new ListTag();
		for (int i = 0; i < SLOTS; i++) {
			CompoundTag t = items.getItem(i).save(new CompoundTag());
			t.putInt("Keep", amounts[i]);
			list.add(t);
		}
		tag.put("Stock", list);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		ListTag list = tag.getList("Stock", Tag.TAG_COMPOUND);
		for (int i = 0; i < SLOTS; i++) {
			CompoundTag t = i < list.size() ? list.getCompound(i) : new CompoundTag();
			items.setItem(i, ItemStack.of(t));
			amounts[i] = t.getInt("Keep");
		}
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.stock_keeper");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new StockKeeperMenu(id, inv, this);
	}
}
