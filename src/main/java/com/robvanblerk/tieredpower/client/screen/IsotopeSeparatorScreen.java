package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.IsotopeSeparatorBlockEntity;
import com.robvanblerk.tieredpower.menu.IsotopeSeparatorMenu;

public class IsotopeSeparatorScreen extends MachineScreen<IsotopeSeparatorMenu> {
	private static final int WATER_X = 96, D_X = 122;

	public IsotopeSeparatorScreen(IsotopeSeparatorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = IsotopeSeparatorBlockEntity.TANK;
		drawTank(g, x + WATER_X, y + 18, (float) menu.getWater() / t, 0xFF3F76E4);
		drawTank(g, x + D_X, y + 18, (float) menu.getDeuterium() / t, 0xFFC8E0FF);
		text(g, "D", x + D_X + 4, y + 72);
		text(g, "50 water -> 1 D", x + 30, y + 22);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int t = IsotopeSeparatorBlockEntity.TANK;
		if (isHovering(WATER_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB", menu.getWater(), t)), mouseX, mouseY);
		else if (isHovering(D_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Deuterium: %,d / %,d mB", menu.getDeuterium(), t)), mouseX, mouseY);
	}
}
