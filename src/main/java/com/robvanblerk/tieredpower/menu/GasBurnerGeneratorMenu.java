package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.GasBurnerGeneratorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class GasBurnerGeneratorMenu extends MachineMenu {
	// 0-1 energy, 2 hydrogen, 3 oxygen, 4 FE/t
	public static final int DATA_COUNT = 6;

	public GasBurnerGeneratorMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainerData(DATA_COUNT));
	}

	public GasBurnerGeneratorMenu(int id, Inventory inv, ContainerData data) {
		super(ModMenus.GAS_BURNER_GENERATOR.get(), id, data, 0);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return GasBurnerGeneratorBlockEntity.CAPACITY; }
	public int getHydrogen() { return data.get(2); }
	public int getOxygen() { return data.get(3); }
	public int getGenerating() { return data.get(4); }
	public int getMethane() { return data.get(5); }
	@Override public boolean stillValid(Player player) { return true; }
}
