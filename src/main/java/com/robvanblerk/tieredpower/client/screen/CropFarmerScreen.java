package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.CropFarmerMenu;

public class CropFarmerScreen extends MachineScreen<CropFarmerMenu> {
	public CropFarmerScreen(CropFarmerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		text(g, "Seeds", x + 52, y + 20);
		text(g, "Harvested", x + 52, y + 34);
		text(g, String.format("%,d", menu.getHarvested()), x + 52, y + 44);
		text(g, menu.getEnergyCost() + " FE/t", x + 52, y + 60);
		if (menu.getGrowthCharge() > 0) text(g, "Grow " + menu.getGrowthCharge(), x + 8, y + 72);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}
}
