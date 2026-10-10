package com.robvanblerk.tieredpower.drone;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.menu.MachineMenu;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class DroneStationMenu extends MachineMenu {
	// 0-1 energy, 2 mode, 3 drones working, 4 linked, 5 drones fitted
	public static final int DATA_COUNT = 6;
	public static final int DRONE_X = 8, DRONE_Y = 17, BUFFER_X = 62, BUFFER_Y = 17;
	private final Container container;

	public DroneStationMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(DroneStationBlockEntity.SIZE), new SimpleContainerData(DATA_COUNT));
	}

	public DroneStationMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.DRONE_STATION.get(), id, data, DroneStationBlockEntity.SIZE);
		this.container = container;
		for (int i = 0; i < DroneStationBlockEntity.DRONE_SLOTS; i++) {
			addSlot(new Slot(container, i, DRONE_X + (i % 2) * 18, DRONE_Y + (i / 2) * 18) {
				@Override public boolean mayPlace(ItemStack s) { return s.is(ModBlocks.UTILITY_DRONE.get()); }
				@Override public int getMaxStackSize() { return 1; }
			});
		}
		for (int i = 0; i < DroneStationBlockEntity.BUFFER_SIZE; i++)
			addSlot(new Slot(container, DroneStationBlockEntity.BUFFER_START + i, BUFFER_X + (i % 5) * 18, BUFFER_Y + (i / 5) * 18));
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return DroneStationBlockEntity.CAPACITY; }
	public int getMode() { return data.get(2); }
	public int getWorking() { return data.get(3); }
	public boolean isLinked() { return data.get(4) != 0; }
	public int getDrones() { return data.get(5); }

	/** Button 1 changes the mode (button 0 is redstone control). */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == 1 && container instanceof DroneStationBlockEntity station) {
			station.cycleMode();
			return true;
		}
		return super.clickMenuButton(player, id);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
