package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.BrineElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;
import com.robvanblerk.tieredpower.registry.ModTags;

public class BrineElectrolyzerMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 water, 5 chlorine, 6 hydrogen, 7 FE/t
	public static final int DATA_COUNT = 8;
	private final Container container;

	public BrineElectrolyzerMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(3), new SimpleContainerData(DATA_COUNT));
	}

	public BrineElectrolyzerMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.BRINE_ELECTROLYZER.get(), id, data, 3);
		this.container = container;
		addSlot(new Slot(container, 0, 30, 35) {
			@Override public boolean mayPlace(ItemStack s) { return s.is(ModTags.SALT); }
		});
		addUpgradeSlots(container, 1, 7);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return BrineElectrolyzerBlockEntity.CAPACITY; }
	public float progress() { return data.get(3) <= 0 ? 0 : (float) data.get(2) / data.get(3); }
	public int getWater() { return data.get(4); }
	public int getChlorine() { return data.get(5); }
	public int getHydrogen() { return data.get(6); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
