package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.SaltEvaporatorBlockEntity;
import com.robvanblerk.tieredpower.menu.SaltEvaporatorMenu;

public class SaltEvaporatorScreen extends MachineScreen<SaltEvaporatorMenu> {
	private static final int W_X = 44;

	public SaltEvaporatorScreen(SaltEvaporatorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawTank(g, x + W_X, y + 18, (float) menu.getWater() / SaltEvaporatorBlockEntity.TANK, 0xFF3F76E4);
		drawArrow(g, x + 68, y + 35, menu.progress());
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(W_X, 18, 14, 52, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB (1,000 mB per salt)", menu.getWater(), SaltEvaporatorBlockEntity.TANK)), mouseX, mouseY);
	}
}
