package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.ChemicalWasherBlockEntity;
import com.robvanblerk.tieredpower.menu.ChemicalWasherMenu;

public class ChemicalWasherScreen extends MachineScreen<ChemicalWasherMenu> {
	private static final int W_X = 112, CL_X = 132;

	public ChemicalWasherScreen(ChemicalWasherMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = ChemicalWasherBlockEntity.TANK;
		drawArrow(g, x + 52, y + 35, menu.progress());
		drawTank(g, x + W_X, y + 18, (float) menu.getWater() / t, 0xFF3F76E4);
		drawTank(g, x + CL_X, y + 18, (float) menu.getChlorine() / t, 0xFFD8E870);
		text(g, "Cl", x + CL_X + 3, y + 72);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int t = ChemicalWasherBlockEntity.TANK;
		if (isHovering(W_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB (500 per ore)", menu.getWater(), t)), mouseX, mouseY);
		else if (isHovering(CL_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Chlorine: %,d / %,d mB (100 per ore)", menu.getChlorine(), t)), mouseX, mouseY);
		else if (menu.getCarried().isEmpty() && isHovering(29, 34, 18, 18, mouseX, mouseY) && !menu.slots.get(0).hasItem())
			g.renderTooltip(font, Component.literal("Ore or raw ore: 4 dust each"), mouseX, mouseY);
	}
}
