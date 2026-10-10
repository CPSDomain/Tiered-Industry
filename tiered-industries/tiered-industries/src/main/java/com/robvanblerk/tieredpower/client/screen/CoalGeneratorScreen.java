package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.CoalGeneratorBlockEntity;
import com.robvanblerk.tieredpower.menu.CoalGeneratorMenu;

public class CoalGeneratorScreen extends MachineScreen<CoalGeneratorMenu> {
	public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawFlame(g, x + CoalGeneratorMenu.FUEL_X + 1, y + CoalGeneratorMenu.FUEL_Y - 18, menu.getBurnFraction());

		drawEnergyBar(g, x + 156, y + 18, 14, 52);

		// Short lines so they fit beside the energy bar at any GUI scale.
		text(g, menu.isBurning() ? "Making " + CoalGeneratorBlockEntity.outputPerTick() + " FE/t" : "Idle - add fuel", x + 50, y + 22);
		text(g, "Out: " + menu.getOutputRate() + " FE/t", x + 50, y + 36);
		text(g, String.format("Stored: %,d FE", menu.getEnergy()), x + 50, y + 50);
	}
}
