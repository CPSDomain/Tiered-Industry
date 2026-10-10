package com.robvanblerk.tieredpower.turbine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.menu.MachineMenu;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class TurbineControllerMenu extends MachineMenu {
	// 0 formed, 1 rotors, 2-3 steam, 4-5 steam capacity, 6+13 flow, 7-8 FE/t, 9 energy (per mille), 10 efficiency x100, 11-12 water
	public static final int DATA_COUNT = 14;
	private final ContainerLevelAccess access;

	public TurbineControllerMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
	}

	public TurbineControllerMenu(int id, Inventory inv, ContainerData data, ContainerLevelAccess access) {
		super(ModMenus.TURBINE_CONTROLLER.get(), id, data, 0);
		this.access = access;
		addPlayerInventory(inv);
	}

	private int joined(int lo, int hi) { return (data.get(lo) & 0xFFFF) | ((data.get(hi) & 0xFFFF) << 16); }

	public boolean isFormed() { return data.get(0) != 0; }
	public int getRotors() { return data.get(1); }
	public int getSteam() { return joined(2, 3); }
	public int getSteamCapacity() { return joined(4, 5); }
	public int getFlow() { return joined(6, 13); }
	public int getGenerated() { return joined(7, 8); }
	public float getEnergyFraction() { return data.get(9) / 1000f; }
	public float getEfficiency() { return data.get(10) / 100f; }
	public int getWater() { return joined(11, 12); }

	@Override public long getCapacity() { return 0; }

	@Override
	public boolean stillValid(Player player) {
		return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity
				&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
	}
}
