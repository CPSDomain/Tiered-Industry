package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.AutoCrafterMenu;

public class AutoCrafterScreen extends MachineScreen<AutoCrafterMenu> {
	public AutoCrafterScreen(AutoCrafterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		// Pattern grid (ghost slots) - drawn slightly blue so it's clear they're a pattern, not storage.
		for (int i = 0; i < 9; i++) {
			int sx = x + AutoCrafterMenu.GRID_X + (i % 3) * 18 - 1, sy = y + AutoCrafterMenu.GRID_Y + (i / 3) * 18 - 1;
			g.blit(WIDGETS, sx, sy, 0, 0, 18, 18);
			g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x303060C0);
		}
		drawArrow(g, x + 88, y + 35, menu.getProgressFraction());
		String status = switch (menu.getStatus()) {
			case 3 -> "Crafting";
			case 2 -> "Output full";
			case 1 -> "Need items";
			default -> "No recipe";
		};
		text(g, status, x + 88, y + 58);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(AutoCrafterMenu.GRID_X - 1, AutoCrafterMenu.GRID_Y - 1, 54, 54, mouseX, mouseY) && menu.getCarried().isEmpty()) {
			g.renderTooltip(font, Component.literal("Pattern: click with an item to set, empty hand to clear"), mouseX, mouseY);
		}
	}
}
