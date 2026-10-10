package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.OxygenFurnaceBlockEntity;
import com.robvanblerk.tieredpower.menu.OxygenFurnaceMenu;

public class OxygenFurnaceScreen extends MachineScreen<OxygenFurnaceMenu> {
	private static final int O_X = 136;

	public OxygenFurnaceScreen(OxygenFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 68, y + 35, menu.progress());
		drawTank(g, x + O_X, y + 18, (float) menu.getOxygen() / OxygenFurnaceBlockEntity.TANK, 0xFF8FC8FF);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(O_X, 18, 14, 52, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(String.format("Oxygen: %,d / %,d mB (100 mB per steel ingot)", menu.getOxygen(), OxygenFurnaceBlockEntity.TANK)), mouseX, mouseY);
	}
}
