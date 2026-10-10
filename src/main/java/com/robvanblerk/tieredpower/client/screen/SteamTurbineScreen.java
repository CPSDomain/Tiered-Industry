package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.SteamTurbineMenu;

public class SteamTurbineScreen extends MachineScreen<SteamTurbineMenu> {
	public SteamTurbineScreen(SteamTurbineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		// Rotor speed bar
		int bx = x + 8, by = y + 20, bw = 136, bh = 10;
		g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, DARK);
		g.fill(bx, by, bx + bw, by + bh, 0xFF2A2A2A);
		g.fill(bx, by, bx + bw * menu.getSpeed() / 100, by + bh, 0xFF7FD4FF);

		drawEnergyBar(g, x + 156, y + 18, 14, 52);

		text(g, "Rotor: " + menu.getSpeed() + "%", x + 8, y + 36);
		text(g, "Making " + menu.getGenerated() + " FE/t", x + 8, y + 48);
		text(g, String.format("Stored: %,d FE", menu.getEnergy()), x + 8, y + 60);
	}
}
