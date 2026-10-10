package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.QuarryBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Quarry menu: buffer, upgrades, settings buttons (ids 2-10) and a 6-slot ghost "void filter". Taller than other GUIs. */
public class QuarryMenu extends MachineMenu {
	// 0-1 energy, 2 current Y, 3-4 mined, 5 state, 6 FE/t, 7 radius, 8 behind, 9 min Y, 10 tool mode
	public static final int DATA_COUNT = 14;
	public static final int EXTRA_HEIGHT = 56;
	public static final int GRID_X = 98, GRID_Y = 17;
	public static final int VOID_X = 62, VOID_Y = 112;

	private final Container container;
	private final Container voidFilter;
	private final int ghostStart;

	public QuarryMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(11), new SimpleContainerData(DATA_COUNT), new SimpleContainer(6));
	}

	public QuarryMenu(int containerId, Inventory inventory, Container container, ContainerData data, Container voidFilter) {
		super(ModMenus.QUARRY.get(), containerId, data, 11);
		this.container = container;
		this.voidFilter = voidFilter;
		for (int i = 0; i < QuarryBlockEntity.BUFFER_SLOTS; i++) {
			addOutputSlot(container, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18, false);
		}
		addUpgradeSlots(container, QuarryBlockEntity.BUFFER_SLOTS, 6);
		addPlayerInventory(inventory, EXTRA_HEIGHT);
		ghostStart = slots.size();
		for (int i = 0; i < 6; i++) {
			addSlot(new Slot(voidFilter, i, VOID_X + i * 18, VOID_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}

				@Override
				public boolean mayPickup(Player player) {
					return false;
				}
			});
		}
	}

	public boolean isGhostSlot(int index) {
		return index >= ghostStart && index < ghostStart + 6;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (isGhostSlot(slotId)) {
			ItemStack carried = getCarried();
			voidFilter.setItem(slotId - ghostStart, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
			voidFilter.setChanged();
			broadcastChanges();
			return;
		}
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (isGhostSlot(index)) return ItemStack.EMPTY;
		return super.quickMoveStack(player, index);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id >= 2 && id <= 10 && container instanceof QuarryBlockEntity quarry) {
			quarry.handleButton(id);
			return true;
		}
		return super.clickMenuButton(player, id);
	}

	@Override
	public long getCapacity() {
		return QuarryBlockEntity.CAPACITY;
	}

	public int getY() { return (short) data.get(2); }
	public int getMined() { return ((data.get(4) & 0xFFFF) << 16) | (data.get(3) & 0xFFFF); }
	public int getState() { return data.get(5); }
	public int getRadius() { return data.get(7); }
	public boolean isBehind() { return data.get(8) != 0; }
	public boolean isCustom() { return data.get(11) != 0; }
	public int getCustomWidth() { return data.get(12); }
	public int getCustomLength() { return data.get(13); }
	public int getMinY() { return (short) data.get(9); }
	public int getToolMode() { return Math.floorMod(data.get(10), QuarryBlockEntity.TOOL_MODES.length); }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
