package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.SteamHammerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class SteamHammerMenu extends MachineMenu {
	// 0-1 energy (unused), 2 progress, 3 max, 4 steam
	public static final int DATA_COUNT = 5;
	private final Container container;
	private final Inventory inventory;

	public SteamHammerMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(4), new SimpleContainerData(DATA_COUNT));
	}

	public SteamHammerMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.STEAM_HAMMER.get(), id, data, 4);
		this.container = container;
		this.inventory = inv;
		addSlot(new Slot(container, 0, 44, 35) {
			@Override public boolean mayPlace(ItemStack s) { return SteamHammerBlockEntity.canProcess(inventory.player.level(), s); }
		});
		addOutputSlot(container, 1, 104, 35, true);
		addUpgradeSlots(container, 2, -1); // runs on steam, no FE cost
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return 0; }

	public float getProgressFraction() {
		int max = data.get(3);
		return max <= 0 ? 0f : (float) data.get(2) / max;
	}

	public int getSteam() { return data.get(4); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
