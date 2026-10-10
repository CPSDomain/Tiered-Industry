package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.BioDigesterBlockEntity;
import com.robvanblerk.tieredpower.menu.BioDigesterMenu;

public class BioDigesterScreen extends MachineScreen<BioDigesterMenu> {
	private static final int M_X = 136;

	public BioDigesterScreen(BioDigesterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 68, y + 35, menu.progress());
		drawTank(g, x + M_X, y + 18, (float) menu.getMethane() / BioDigesterBlockEntity.TANK, 0xFFB8D890);
		text(g, "200 mB methane per item", x + 8, y + 62);
	}

	@Override
	protected String statusText() {
		return "";
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(M_X, 18, 14, 52, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(String.format("Methane: %,d / %,d mB", menu.getMethane(), BioDigesterBlockEntity.TANK)), mouseX, mouseY);
		else if (menu.getCarried().isEmpty() && isHovering(43, 34, 18, 18, mouseX, mouseY) && !menu.slots.get(0).hasItem())
			g.renderTooltip(font, Component.literal("Crops, seeds, saplings, leaves, flowers, rotten flesh..."), mouseX, mouseY);
	}
}
