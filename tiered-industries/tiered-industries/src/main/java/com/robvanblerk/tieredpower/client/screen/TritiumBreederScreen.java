package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.TritiumBreederBlockEntity;
import com.robvanblerk.tieredpower.menu.TritiumBreederMenu;

public class TritiumBreederScreen extends MachineScreen<TritiumBreederMenu> {
	private static final int T_X = 136;

	public TritiumBreederScreen(TritiumBreederMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 68, y + 35, menu.progress());
		drawTank(g, x + T_X, y + 18, (float) menu.getTritium() / TritiumBreederBlockEntity.TANK, 0xFFB8FFC8);
		text(g, menu.reactorRunning() ? "Reactor running" : "Needs a running reactor", x + 8, y + 62);
	}

	@Override
	protected String statusText() {
		return "";
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(T_X, 18, 14, 52, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(String.format("Tritium: %,d / %,d mB", menu.getTritium(), TritiumBreederBlockEntity.TANK)), mouseX, mouseY);
		else if (menu.getCarried().isEmpty() && isHovering(43, 34, 18, 18, mouseX, mouseY) && !menu.slots.get(0).hasItem())
			g.renderTooltip(font, Component.literal("Lithium ingots go here"), mouseX, mouseY);
	}
}
