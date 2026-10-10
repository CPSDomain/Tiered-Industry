package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.item.CraftingPatternItem;
import com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;
import com.robvanblerk.tieredpower.storage.CraftingPattern;

public class MolecularAssemblerMenu extends MachineMenu {
	public static final int DATA_COUNT = 2;
	private final Container container;

	public MolecularAssemblerMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(MolecularAssemblerBlockEntity.SIZE), new SimpleContainerData(DATA_COUNT));
	}

	public MolecularAssemblerMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.MOLECULAR_ASSEMBLER.get(), id, data, MolecularAssemblerBlockEntity.SIZE);
		this.container = container;
		for (int n = 0; n < MolecularAssemblerBlockEntity.PATTERNS; n++) { // 3 x 3
			addSlot(new Slot(container, n, 62 + (n % 3) * 18, 17 + (n / 3) * 18) {
				@Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof CraftingPatternItem && CraftingPattern.fromStack(s) != null; }
				@Override public int getMaxStackSize() { return 1; }
			});
		}
		addUpgradeSlots(container, 9, -1);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return 0; }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
