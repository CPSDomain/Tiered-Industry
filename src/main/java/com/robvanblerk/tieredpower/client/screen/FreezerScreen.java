package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.FreezerBlockEntity;
import com.robvanblerk.tieredpower.menu.FreezerMenu;

public class FreezerScreen extends MachineScreen<FreezerMenu> {
	private static final int TANK_X = 136, TANK_Y = 18;

	public FreezerScreen(FreezerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 68, y + 35, menu.getProgressFraction());
		int colour = menu.getFluidKind() == 2 ? 0xFFE5641A : 0xFF3F76E4;
		drawTank(g, x + TANK_X, y + TANK_Y, (float) menu.getFluidAmount() / FreezerBlockEntity.TANK_CAPACITY, colour);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
		text(g, statusText(), x + 8, y + 62);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(TANK_X, TANK_Y, 14, 52, mouseX, mouseY)) {
			String fluid = menu.getFluidKind() == 2 ? "Lava" : menu.getFluidKind() == 1 ? "Water" : "Empty";
			g.renderTooltip(font, Component.literal(String.format("%s: %,d / %,d mB", fluid, menu.getFluidAmount(), FreezerBlockEntity.TANK_CAPACITY)), mouseX, mouseY);
		}
	}
}
