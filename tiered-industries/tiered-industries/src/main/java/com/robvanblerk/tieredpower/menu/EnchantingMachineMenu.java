package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.robvanblerk.tieredpower.block.entity.EnchantingMachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class EnchantingMachineMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 level index, 5 FE/t
	public static final int DATA_COUNT = 7;
	private final Container container;

	public EnchantingMachineMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(5), new SimpleContainerData(DATA_COUNT));
	}

	public EnchantingMachineMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.ENCHANTING_MACHINE.get(), id, data, 5);
		this.container = container;
		addSlot(new Slot(container, 0, 30, 35) {
			@Override
			public boolean mayPlace(ItemStack s) {
				return EnchantingMachineBlockEntity.canEnchant(s);
			}

			@Override
			public int getMaxStackSize() {
				return 64;
			}
		});
		addSlot(new Slot(container, 1, 50, 35) {
			@Override
			public boolean mayPlace(ItemStack s) {
				return s.is(Items.LAPIS_LAZULI);
			}
		});
		addOutputSlot(container, 2, 110, 35, true);
		addUpgradeSlots(container, 3, 5);
		addPlayerInventory(inv);
	}

	@Override
	public long getCapacity() {
		return EnchantingMachineBlockEntity.CAPACITY;
	}

	public float getProgressFraction() {
		int max = data.get(3);
		return max <= 0 ? 0f : (float) data.get(2) / max;
	}

	public int getLevel() {
		return EnchantingMachineBlockEntity.LEVELS[Math.floorMod(data.get(4), EnchantingMachineBlockEntity.LEVELS.length)];
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == 1 && container instanceof EnchantingMachineBlockEntity be) {
			be.cycleLevel();
			return true;
		}
		return super.clickMenuButton(player, id);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	public int getXp() {
		return data.get(6);
	}
}
