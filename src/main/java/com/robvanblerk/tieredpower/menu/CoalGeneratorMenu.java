package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.CoalGeneratorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class CoalGeneratorMenu extends MachineMenu {
	public static final int DATA_COUNT = 5; // energy low, energy high, burn time, burn time total, output last tick
	public static final int FUEL_X = 26, FUEL_Y = 53;

	private final Container container;

	/** Client-side constructor, used when the GUI opens on the player's screen. */
	public CoalGeneratorMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	/** Server-side constructor, connected to the real block entity. */
	public CoalGeneratorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.COAL_GENERATOR.get(), containerId, data, 1);
		this.container = container;
		addSlot(new Slot(container, 0, FUEL_X, FUEL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return CoalGeneratorBlockEntity.burnTimeOf(stack) > 0;
			}
		});
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return CoalGeneratorBlockEntity.CAPACITY;
	}

	public float getBurnFraction() {
		int total = data.get(3);
		return total <= 0 ? 0f : (float) data.get(2) / total;
	}

	/** FE per tick currently being sent out to cables/machines. */
	public int getOutputRate() {
		return data.get(4);
	}

	public boolean isBurning() {
		return data.get(2) > 0;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
