package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.WindTurbineBlockEntity;
import com.robvanblerk.tieredpower.menu.WindTurbineMenu;

public class WindTurbineScreen extends MachineScreen<WindTurbineMenu> {
	public WindTurbineScreen(WindTurbineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		text(g, String.format("Making %,d FE/t", menu.getGenerating()), x + 8, y + 20);
		int y0 = menu.getY();
		String height = y0 <= WindTurbineBlockEntity.MIN_Y ? "too low (build above Y " + WindTurbineBlockEntity.MIN_Y + ")"
				: Math.min(100, (y0 - WindTurbineBlockEntity.MIN_Y) * 100 / (WindTurbineBlockEntity.FULL_Y - WindTurbineBlockEntity.MIN_Y)) + "%";
		text(g, "Height Y " + y0 + ": " + height, x + 8, y + 32);
		text(g, "Open space: " + menu.getClearance() + "%", x + 8, y + 44);
		text(g, "Weather: " + switch (menu.getWeather()) { case 2 -> "storm x2"; case 1 -> "rain x1.5"; default -> "clear"; }, x + 8, y + 56);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}
}
