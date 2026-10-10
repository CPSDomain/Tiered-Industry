package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.FluidMixerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class FluidMixerMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 water, 5 lava, 6 FE/t
	public static final int DATA_COUNT = 7;
	private final Container container;

	public FluidMixerMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(4), new SimpleContainerData(DATA_COUNT));
	}

	public FluidMixerMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.FLUID_MIXER.get(), id, data, 4);
		this.container = container;
		addSlot(new Slot(container, 0, 30, 35) {
			@Override
			public boolean mayPlace(ItemStack s) {
				return FluidMixerBlockEntity.isMixable(s);
			}
		});
		addOutputSlot(container, 1, 90, 35, true);
		addUpgradeSlots(container, 2, 6);
		addPlayerInventory(inv);
	}

	@Override
	public long getCapacity() {
		return FluidMixerBlockEntity.CAPACITY;
	}

	public float getProgressFraction() {
		int max = data.get(3);
		return max <= 0 ? 0f : (float) data.get(2) / max;
	}

	public int getWater() { return data.get(4); }
	public int getLava() { return data.get(5); }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
