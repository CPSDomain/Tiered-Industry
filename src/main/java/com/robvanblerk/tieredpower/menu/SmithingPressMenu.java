package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.SmithingPressBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class SmithingPressMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 FE/t
	public static final int DATA_COUNT = 5;
	private final Container container;
	private final Inventory inventory;

	public SmithingPressMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(6), new SimpleContainerData(DATA_COUNT));
	}

	public SmithingPressMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.SMITHING_PRESS.get(), id, data, 6);
		this.container = container;
		this.inventory = inv;
		for (int i = 0; i < 3; i++) {
			int slot = i;
			addSlot(new Slot(container, i, 26 + i * 18, 35) {
				@Override public boolean mayPlace(ItemStack s) { return SmithingPressBlockEntity.fitsSlot(inventory.player.level(), slot, s); }
			});
		}
		addOutputSlot(container, SmithingPressBlockEntity.OUTPUT, 120, 35, true);
		addUpgradeSlots(container, 4, 4);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return SmithingPressBlockEntity.CAPACITY; }
	public float progress() { return data.get(3) <= 0 ? 0 : (float) data.get(2) / data.get(3); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
