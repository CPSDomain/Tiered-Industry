package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.SpawnerControllerMenu;

public class SpawnerControllerScreen extends MachineScreen<SpawnerControllerMenu> {
	public SpawnerControllerScreen(SpawnerControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		if (!menu.hasSpawner()) {
			text(g, "No spawner next to it", x + 30, y + 22);
			text(g, "Place it touching a", x + 30, y + 36);
			text(g, "mob spawner", x + 30, y + 46);
		} else {
			text(g, menu.isBoosting() ? "Spawner boosted" : "Spawner normal", x + 30, y + 22);
			text(g, menu.isBoosting() ? "No player needed" : "Needs power", x + 30, y + 36);
			text(g, statusText(), x + 30, y + 60);
		}
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}
}
