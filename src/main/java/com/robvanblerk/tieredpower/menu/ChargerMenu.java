package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.ChargerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class ChargerMenu extends MachineMenu {
	public static final int DATA_COUNT = 4; // energy low, energy high, item charge %, FE/t into item
	public static final int INPUT_X = 44, INPUT_Y = 35, OUTPUT_X = 104, OUTPUT_Y = 35;

	private final Container container;

	public ChargerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
	}

	public ChargerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.CHARGER.get(), containerId, data, 2);
		this.container = container;
		addSlot(new Slot(container, ChargerBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return ChargerBlockEntity.isChargeable(stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addOutputSlot(container, ChargerBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, true);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return ChargerBlockEntity.CAPACITY;
	}

	public int getItemCharge() { return data.get(2); }
	public int getRate() { return data.get(3); }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
