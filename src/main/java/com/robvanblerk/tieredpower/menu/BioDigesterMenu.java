package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.registry.ModMenus;
import com.robvanblerk.tieredpower.registry.ModTags;

public class BioDigesterMenu extends MachineMenu {
	// 0-1 energy (unused), 2 progress, 3 max, 4 methane
	public static final int DATA_COUNT = 5;
	private final Container container;

	public BioDigesterMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	public BioDigesterMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.BIO_DIGESTER.get(), id, data, 1);
		this.container = container;
		addSlot(new Slot(container, 0, 44, 35) {
			@Override public boolean mayPlace(ItemStack s) { return s.is(ModTags.BIOMASS); }
		});
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return 0; }
	public float progress() { return data.get(3) <= 0 ? 0 : (float) data.get(2) / data.get(3); }
	public int getMethane() { return data.get(4); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
