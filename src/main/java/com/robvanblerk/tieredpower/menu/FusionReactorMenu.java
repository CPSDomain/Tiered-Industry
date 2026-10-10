package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.FusionReactorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class FusionReactorMenu extends MachineMenu {
	// 0-1 output energy, 2-3 ignition charge, 4 temperature (0-1000), 5 burn time left, 6 FE generated last tick
	public static final int DATA_COUNT = 7;
	public static final int DEUTERIUM_X = 8, TRITIUM_X = 28, EMPTY_X = 48, SLOT_Y = 53;

	private final Container container;

	public FusionReactorMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(DATA_COUNT));
	}

	public FusionReactorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.FUSION_REACTOR.get(), containerId, data, 3);
		this.container = container;
		addSlot(new Slot(container, FusionReactorBlockEntity.DEUTERIUM_SLOT, DEUTERIUM_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModBlocks.DEUTERIUM_CELL.get());
			}
		});
		addSlot(new Slot(container, FusionReactorBlockEntity.TRITIUM_SLOT, TRITIUM_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModBlocks.TRITIUM_CELL.get());
			}
		});
		addOutputSlot(container, FusionReactorBlockEntity.EMPTY_SLOT, EMPTY_X, SLOT_Y, false);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return FusionReactorBlockEntity.OUTPUT_CAPACITY;
	}

	public long getCharge() {
		return ((long) (data.get(3) & 0xFFFF) << 16) | (data.get(2) & 0xFFFF);
	}

	public int getTemperature() { return data.get(4); }
	public int getBurnTime() { return data.get(5); }
	public int getGenerated() { return data.get(6); }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
