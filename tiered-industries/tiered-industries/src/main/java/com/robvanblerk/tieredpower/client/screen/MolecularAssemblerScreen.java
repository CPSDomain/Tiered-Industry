package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.MolecularAssemblerMenu;
import com.robvanblerk.tieredpower.storage.CraftingPattern;

public class MolecularAssemblerScreen extends MachineScreen<MolecularAssemblerMenu> {
	public MolecularAssemblerScreen(MolecularAssemblerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int n = 0;
		for (int i = 0; i < com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity.PATTERNS; i++)
			if (CraftingPattern.fromStack(menu.slots.get(i).getItem()) != null) n++;
		String count = n + " / 9 patterns";
		text(g, count, x + imageWidth - 8 - font.width(count), y + imageHeight - 94);
	}

	@Override
	protected String statusText() {
		int ops = 1 + Math.min(4, speedCount());
		return ops + " operation" + (ops == 1 ? "" : "s") + " per cycle";
	}

	private int speedCount() {
		return menu.slots.get(9).getItem().getCount();
	}
}
