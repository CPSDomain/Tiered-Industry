package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.FusionControllerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class FusionControllerMenu extends MachineMenu {
	// 0-1 output energy, 2-3 ignition charge, 4 temperature, 5 burn time, 6 FE/t generated (low), 7 formed, 8 coils, 9 FE/t (high)
	public static final int DATA_COUNT = 10;
	public static final int DEUTERIUM_X = 8, TRITIUM_X = 28, EMPTY_X = 48, SLOT_Y = 53;

	private final Container container;

	public FusionControllerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(DATA_COUNT));
	}

	public FusionControllerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.FUSION_CONTROLLER.get(), containerId, data, 3);
		this.container = container;
		addSlot(new Slot(container, FusionControllerBlockEntity.DEUTERIUM_SLOT, DEUTERIUM_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModBlocks.DEUTERIUM_CELL.get());
			}
		});
		addSlot(new Slot(container, FusionControllerBlockEntity.TRITIUM_SLOT, TRITIUM_X, SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModBlocks.TRITIUM_CELL.get());
			}
		});
		addOutputSlot(container, FusionControllerBlockEntity.EMPTY_SLOT, EMPTY_X, SLOT_Y, false);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return FusionControllerBlockEntity.OUTPUT_CAPACITY;
	}

	public long getCharge() {
		return ((long) (data.get(3) & 0xFFFF) << 16) | (data.get(2) & 0xFFFF);
	}

	public int getTemperature() { return data.get(4); }
	public int getBurnTime() { return data.get(5); }
	public int getGenerated() { return ((data.get(9) & 0xFFFF) << 16) | (data.get(6) & 0xFFFF); }
	public boolean isFormed() { return data.get(7) != 0; }
	public int getCoils() { return data.get(8); }

	public int getMaxOutput() {
		return FusionControllerBlockEntity.maxOutputFor(getCoils());
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
