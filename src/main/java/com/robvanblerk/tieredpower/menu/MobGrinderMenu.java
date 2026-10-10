package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.MobGrinderBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class MobGrinderMenu extends MachineMenu {
	// 0-1 energy, 2 all mobs, 3-4 stored xp, 5 kills, 6 FE/t
	public static final int DATA_COUNT = 7;
	private final Container container;

	public MobGrinderMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(11), new SimpleContainerData(DATA_COUNT));
	}

	public MobGrinderMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.MOB_GRINDER.get(), id, data, 11);
		this.container = container;
		for (int i = 0; i < MobGrinderBlockEntity.BUFFER; i++) addOutputSlot(container, i, 98 + (i % 3) * 18, 17 + (i / 3) * 18, false);
		addUpgradeSlots(container, MobGrinderBlockEntity.BUFFER, 6);
		addPlayerInventory(inv);
	}

	@Override
	public long getCapacity() {
		return MobGrinderBlockEntity.CAPACITY;
	}

	public boolean isAllMobs() { return data.get(2) != 0; }
	public int getXp() { return ((data.get(4) & 0xFFFF) << 16) | (data.get(3) & 0xFFFF); }
	public int getKills() { return data.get(5) & 0xFFFF; }

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (container instanceof MobGrinderBlockEntity be) {
			if (id == 1) { be.toggleMode(); return true; }
			if (id == 2) { be.giveXp(player); return true; }
		}
		return super.clickMenuButton(player, id);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
