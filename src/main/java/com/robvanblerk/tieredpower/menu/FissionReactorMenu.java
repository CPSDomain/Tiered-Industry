package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.FissionReactorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class FissionReactorMenu extends MachineMenu {
	// 0-1 energy, 2 heat, 3 active rods, 4 water, 5 steam, 6 FE/t, 7 scram
	public static final int DATA_COUNT = 8;

	private final Container container;

	public FissionReactorMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(4), new SimpleContainerData(DATA_COUNT));
	}

	public FissionReactorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.FISSION_REACTOR.get(), containerId, data, 4);
		this.container = container;
		for (int i = 0; i < FissionReactorBlockEntity.RODS; i++) {
			addSlot(new Slot(container, i, 8 + (i % 2) * 18, 26 + (i / 2) * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return com.robvanblerk.tieredpower.energy.FuelRods.isFuel(stack);
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return FissionReactorBlockEntity.CAPACITY;
	}

	public int getHeat() { return data.get(2); }
	public int getActiveRods() { return data.get(3); }
	public int getWater() { return data.get(4); }
	public int getSteam() { return data.get(5); }
	public int getGenerating() { return data.get(6); }
	public boolean isScrammed() { return data.get(7) != 0; }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
