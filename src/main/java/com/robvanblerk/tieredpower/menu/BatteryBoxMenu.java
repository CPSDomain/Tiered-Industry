package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.BatteryBoxBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class BatteryBoxMenu extends MachineMenu {
	// 0-1 energy, 2-3 capacity, 4-5 FE/t in, 6-7 FE/t out
	public static final int DATA_COUNT = 8;

	private final ContainerLevelAccess access;

	public BatteryBoxMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
	}

	public BatteryBoxMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
		super(ModMenus.BATTERY_BOX.get(), containerId, data, 0);
		this.access = access;
		addPlayerInventory(inventory);
	}

	private int joined(int low) {
		return ((data.get(low + 1) & 0xFFFF) << 16) | (data.get(low) & 0xFFFF);
	}

	@Override
	public long getCapacity() {
		return joined(2);
	}

	public int getRateIn() { return joined(4); }
	public int getRateOut() { return joined(6); }

	@Override
	public boolean stillValid(Player player) {
		return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof BatteryBoxBlockEntity
				&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
	}
}
