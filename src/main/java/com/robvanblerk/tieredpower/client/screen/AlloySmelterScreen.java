package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.AlloySmelterMenu;

public class AlloySmelterScreen extends MachineScreen<AlloySmelterMenu> {
	public AlloySmelterScreen(AlloySmelterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 78, y + 35, menu.getProgressFraction());
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
		text(g, statusText(), x + 8, y + 60);
	}
}
