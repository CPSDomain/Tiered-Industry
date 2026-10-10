package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.BankControllerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class BankControllerMenu extends MachineMenu {
	// 0-3 stored (64-bit), 4-7 capacity (64-bit), 8-9 FE/t in, 10-11 FE/t out, 12 formed, 13 cells
	public static final int DATA_COUNT = 14;

	private final ContainerLevelAccess access;

	public BankControllerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
	}

	public BankControllerMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
		super(ModMenus.BANK_CONTROLLER.get(), containerId, data, 0);
		this.access = access;
		addPlayerInventory(inventory);
	}

	private long joined(int first, int parts) {
		long value = 0;
		for (int i = parts - 1; i >= 0; i--) value = (value << 16) | (data.get(first + i) & 0xFFFFL);
		return value;
	}

	public long getStored() { return joined(0, 4); }
	public long getBankCapacity() { return joined(4, 4); }
	public long getRateIn() { return joined(8, 2); }
	public long getRateOut() { return joined(10, 2); }
	public boolean isFormed() { return data.get(12) != 0; }
	public int getCells() { return data.get(13); }

	@Override
	public long getCapacity() {
		return 0; // no standard energy bar; the screen draws its own
	}

	@Override
	public boolean stillValid(Player player) {
		return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof BankControllerBlockEntity
				&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
	}
}
