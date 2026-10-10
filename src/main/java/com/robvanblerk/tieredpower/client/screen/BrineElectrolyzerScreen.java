package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.BrineElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.menu.BrineElectrolyzerMenu;

public class BrineElectrolyzerScreen extends MachineScreen<BrineElectrolyzerMenu> {
	private static final int W_X = 76, CL_X = 106, H_X = 126;

	public BrineElectrolyzerScreen(BrineElectrolyzerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = BrineElectrolyzerBlockEntity.TANK;
		drawArrow(g, x + 50, y + 35, menu.progress());
		drawTank(g, x + W_X, y + 18, (float) menu.getWater() / t, 0xFF3F76E4);
		drawTank(g, x + CL_X, y + 18, (float) menu.getChlorine() / t, 0xFFD8E870);
		drawTank(g, x + H_X, y + 18, (float) menu.getHydrogen() / t, 0xFFE8EEFF);
		text(g, "Cl", x + CL_X + 3, y + 72);
		text(g, "H2", x + H_X + 2, y + 72);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int t = BrineElectrolyzerBlockEntity.TANK;
		if (isHovering(W_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB", menu.getWater(), t)), mouseX, mouseY);
		else if (isHovering(CL_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Chlorine: %,d / %,d mB", menu.getChlorine(), t)), mouseX, mouseY);
		else if (isHovering(H_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Hydrogen: %,d / %,d mB", menu.getHydrogen(), t)), mouseX, mouseY);
		else if (menu.getCarried().isEmpty() && isHovering(29, 34, 18, 18, mouseX, mouseY) && !menu.slots.get(0).hasItem())
			g.renderTooltip(font, Component.literal("Salt goes here"), mouseX, mouseY);
	}
}
