package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.CropFarmerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class CropFarmerMenu extends MachineMenu {
	// 0-1 energy, 2 harvested, 3 FE/t, 4 growth charge left
	public static final int DATA_COUNT = 5;
	public static final int SEED_X = 30, GRID_X = 98, TOP_Y = 17, FERT_X = 8, FERT_Y = 53;

	private final Container container;

	public CropFarmerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(15), new SimpleContainerData(DATA_COUNT));
	}

	public CropFarmerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.CROP_FARMER.get(), containerId, data, 15);
		this.container = container;
		for (int i = 0; i < CropFarmerBlockEntity.SEED_SLOTS; i++) {
			addSlot(new Slot(container, i, SEED_X, TOP_Y + i * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return CropFarmerBlockEntity.isPlantable(stack);
				}
			});
		}
		for (int i = 0; i < 9; i++) {
			addOutputSlot(container, CropFarmerBlockEntity.OUTPUT_START + i, GRID_X + (i % 3) * 18, TOP_Y + (i / 3) * 18, false);
		}
		addUpgradeSlots(container, 12, 3);
		Slot fert = addSlot(new Slot(container, CropFarmerBlockEntity.FERTILIZER_SLOT, FERT_X, FERT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return CropFarmerBlockEntity.isFertilizer(stack);
			}
		});
		setGhostItem(fert.index, new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.FERTILIZER.get()));
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return CropFarmerBlockEntity.CAPACITY;
	}

	public int getHarvested() { return data.get(2) & 0xFFFF; }
	public int getGrowthCharge() { return data.get(4); }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
