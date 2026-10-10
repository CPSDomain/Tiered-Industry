package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.BoilerBlockEntity;
import com.robvanblerk.tieredpower.menu.BoilerMenu;

public class BoilerScreen extends MachineScreen<BoilerMenu> {
	private static final int LAVA = 0xFFE5641A;
	private static final int WATER = 0xFF3F76E4;
	private static final int STEAM = 0xFFE8E8E8;
	private static final int TANK_Y = 18, TANK_W = 14, TANK_H = 52;
	private static final int LAVA_X = 116, WATER_X = 136, STEAM_X = 156;

	public BoilerScreen(BoilerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawFlame(g, x + BoilerMenu.FUEL_X + 1, y + BoilerMenu.FUEL_Y - 18, menu.getBurnFraction());

		drawTank(g, x + LAVA_X, y + TANK_Y, menu.getLavaFraction(), LAVA);
		drawTank(g, x + WATER_X, y + TANK_Y, menu.getWaterFraction(), WATER);
		drawTank(g, x + STEAM_X, y + TANK_Y, menu.getSteamFraction(), STEAM);

		text(g, "Heat: " + menu.getHeat() + "%", x + 48, y + 22);
		text(g, "Steam", x + 48, y + 38);
		text(g, menu.getSteamSent() + " mB/t", x + 48, y + 50);
	}

	/** Hover over a tank to see exact amounts. */
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (over(LAVA_X, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("Lava: %,d / %,d mB", menu.getLava(), BoilerBlockEntity.LAVA_CAPACITY)), mouseX, mouseY);
		} else if (over(WATER_X, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB", menu.getWater(), BoilerBlockEntity.WATER_CAPACITY)), mouseX, mouseY);
		} else if (over(STEAM_X, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("Steam: %,d / %,d mB", menu.getSteam(), BoilerBlockEntity.STEAM_CAPACITY)), mouseX, mouseY);
		}
	}

	private boolean over(int tankX, int mouseX, int mouseY) {
		int tx = leftPos + tankX, ty = topPos + TANK_Y;
		return mouseX >= tx && mouseX < tx + TANK_W && mouseY >= ty && mouseY < ty + TANK_H;
	}
}
