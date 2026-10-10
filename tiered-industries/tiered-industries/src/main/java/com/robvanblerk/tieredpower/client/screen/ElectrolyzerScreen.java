package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.ElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.menu.ElectrolyzerMenu;

public class ElectrolyzerScreen extends MachineScreen<ElectrolyzerMenu> {
	private static final int WATER_X = 136, TANK_Y = 18, TANK_W = 14, TANK_H = 52;

	public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 62, y + 37, menu.getProgressFraction());

		text(g, String.format("x%.1f  %d FE/t", menu.getSpeedMultiplier(), menu.getEnergyCost()), x + 58, y + 60);

		drawTank(g, x + WATER_X, y + TANK_Y, (float) menu.getWater() / ElectrolyzerBlockEntity.WATER_CAPACITY, 0xFF3F76E4);

		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int tx = leftPos + WATER_X, ty = topPos + TANK_Y;
		if (mouseX >= tx && mouseX < tx + TANK_W && mouseY >= ty && mouseY < ty + TANK_H) {
			g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB", menu.getWater(), ElectrolyzerBlockEntity.WATER_CAPACITY)), mouseX, mouseY);
		}
	}
}
