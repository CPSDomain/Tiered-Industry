package com.robvanblerk.tieredpower.factory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;

import com.robvanblerk.tieredpower.menu.MachineMenu;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class DigitalFactoryMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 cycle length, 4 FE/t, 5 mode, 6 cells, 7 water, 8 operations this cycle
	public static final int DATA_COUNT = 9;
	public static final int EXTRA = 40, BUTTON_MODE = 70;
	public static final int IN_X = 8, OUT_X = 98, SLOT_Y = 18;
	private final Container container;

	public DigitalFactoryMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(DigitalFactoryBlockEntity.INPUTS + DigitalFactoryBlockEntity.OUTPUTS + 2), new SimpleContainerData(DATA_COUNT));
	}

	public DigitalFactoryMenu(int id, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.DIGITAL_FACTORY.get(), id, data, DigitalFactoryBlockEntity.INPUTS + DigitalFactoryBlockEntity.OUTPUTS + 2);
		this.container = container;
		for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++)
			addSlot(new Slot(container, r * 3 + c, IN_X + c * 18, SLOT_Y + r * 18));
		for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++)
			addOutputSlot(container, DigitalFactoryBlockEntity.OUTPUT_START + r * 3 + c, OUT_X + c * 18, SLOT_Y + r * 18, false);
		addUpgradeSlots(container, DigitalFactoryBlockEntity.INPUTS + DigitalFactoryBlockEntity.OUTPUTS, 4);
		addPlayerInventory(inventory, EXTRA);
	}

	@Override public long getCapacity() { return DigitalFactoryBlockEntity.CAPACITY; }
	public float getProgressFraction() { int m = data.get(3); return m <= 0 ? 0 : (float) data.get(2) / m; }
	public DigitalFactoryBlockEntity.Mode getMode() { return DigitalFactoryBlockEntity.Mode.values()[Math.max(0, Math.min(3, data.get(5)))]; }
	public int getCells() { return data.get(6); }
	public int getWater() { return data.get(7); }
	public int getActive() { return data.get(8); }
	public int getCostPerTick() { return data.get(4); }

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == BUTTON_MODE && container instanceof DigitalFactoryBlockEntity f) {
			f.cycleMode();
			return true;
		}
		return super.clickMenuButton(player, id);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
