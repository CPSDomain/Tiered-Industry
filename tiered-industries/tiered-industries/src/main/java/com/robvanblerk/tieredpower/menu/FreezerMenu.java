package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.robvanblerk.tieredpower.block.entity.FreezerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class FreezerMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 fluid amount, 5 fluid (0 none, 1 water, 2 lava), 6 FE/t
	public static final int DATA_COUNT = 7;
	public static final int INPUT_X = 44, INPUT_Y = 35, OUTPUT_X = 104, OUTPUT_Y = 35;

	private final Container container;

	public FreezerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(4), new SimpleContainerData(DATA_COUNT));
	}

	public FreezerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.FREEZER.get(), containerId, data, 4);
		this.container = container;
		addSlot(new Slot(container, FreezerBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(Items.ICE) || stack.is(Items.PACKED_ICE);
			}
		});
		addOutputSlot(container, FreezerBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, true);
		addUpgradeSlots(container, 2, 6);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return FreezerBlockEntity.CAPACITY;
	}

	public float getProgressFraction() {
		int max = data.get(3);
		return max <= 0 ? 0f : (float) data.get(2) / max;
	}

	public int getFluidAmount() { return data.get(4); }
	public int getFluidKind() { return data.get(5); }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
