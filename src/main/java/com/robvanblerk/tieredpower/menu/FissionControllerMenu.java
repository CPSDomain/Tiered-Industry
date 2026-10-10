package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class FissionControllerMenu extends MachineMenu {
	// 0-1 energy, 2 heat, 3-4 FE/t, 5 formed, 6 assemblies, 7 channels, 8 efficiency %, 9 rods stored, 10 water/10, 11 scram, 12 water blocks inside
	public static final int DATA_COUNT = 13;

	private final Container container;

	public FissionControllerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
	}

	public FissionControllerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.FISSION_CONTROLLER.get(), containerId, data, 2);
		this.container = container;
		addSlot(new Slot(container, 0, 8, 26) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return com.robvanblerk.tieredpower.energy.FuelRods.isFuel(stack);
			}
		});
		addOutputSlot(container, 1, 8, 50, false);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return FissionControllerBlockEntity.CAPACITY;
	}

	public int getHeat() { return data.get(2); }
	public int getGenerating() { return ((data.get(4) & 0xFFFF) << 16) | (data.get(3) & 0xFFFF); }
	public boolean isFormed() { return data.get(5) != 0; }
	public int getAssemblies() { return data.get(6); }
	public int getChannels() { return data.get(7); }
	public int getEfficiency() { return data.get(8); }
	public int getRods() { return data.get(9); }
	public int getWater() { return data.get(10) * 10; }
	public boolean isScrammed() { return data.get(11) != 0; }
	public int getWaterBlocks() { return data.get(12); }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
