package com.robvanblerk.tieredpower.turbine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.client.screen.MachineScreen;

public class TurbineControllerScreen extends MachineScreen<TurbineControllerMenu> {
	public TurbineControllerScreen(TurbineControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	private void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int colour) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, DARK);
		g.fill(x, y, x + w, y + h, ENERGY_EMPTY);
		g.fill(x, y, x + (int) (w * Math.max(0, Math.min(1, frac))), y + h, colour);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		if (!menu.isFormed()) {
			text(g, "Not formed - right-click the", x + 8, y + 22);
			text(g, "controller to see what's missing", x + 8, y + 32);
			return;
		}
		bar(g, x + 8, y + 18, 160, 8, (float) menu.getSteam() / Math.max(1, menu.getSteamCapacity()), 0xFFC8C8D0);
		text(g, String.format("Steam %,d / %,d mB", menu.getSteam(), menu.getSteamCapacity()), x + 8, y + 29);
		bar(g, x + 8, y + 41, 160, 8, menu.getEnergyFraction(), ENERGY);
		text(g, String.format("%,d FE/t", menu.getGenerated()), x + 8, y + 52);
		text(g, String.format("%,d mB/t  x%.2f", menu.getFlow(), menu.getEfficiency()), x + 80, y + 52);
		text(g, menu.getRotors() + " rotors, water " + String.format("%,d", menu.getWater()) + " mB", x + 8, y + 62);
	}
}
