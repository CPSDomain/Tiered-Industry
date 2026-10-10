package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.GasElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.menu.GasElectrolyzerMenu;

public class GasElectrolyzerScreen extends MachineScreen<GasElectrolyzerMenu> {
	private static final int WATER_X = 76, H_X = 96, O_X = 116;

	public GasElectrolyzerScreen(GasElectrolyzerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = GasElectrolyzerBlockEntity.TANK;
		drawTank(g, x + WATER_X, y + 18, (float) menu.getWater() / t, 0xFF3F76E4);
		drawTank(g, x + H_X, y + 18, (float) menu.getHydrogen() / t, 0xFFE8EEFF);
		drawTank(g, x + O_X, y + 18, (float) menu.getOxygen() / t, 0xFF8FC8FF);
		text(g, "H2", x + H_X + 2, y + 72);
		text(g, "O2", x + O_X + 2, y + 72);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int t = GasElectrolyzerBlockEntity.TANK;
		if (isHovering(WATER_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB", menu.getWater(), t)), mouseX, mouseY);
		else if (isHovering(H_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Hydrogen: %,d / %,d mB", menu.getHydrogen(), t)), mouseX, mouseY);
		else if (isHovering(O_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Oxygen: %,d / %,d mB", menu.getOxygen(), t)), mouseX, mouseY);
		else if (isHovering(29, 34, 18, 18, mouseX, mouseY) && menu.getCarried().isEmpty() && !menu.slots.get(0).hasItem())
			g.renderTooltip(font, Component.literal("Hydrogen Jetpack goes here to refuel"), mouseX, mouseY);
	}
}
