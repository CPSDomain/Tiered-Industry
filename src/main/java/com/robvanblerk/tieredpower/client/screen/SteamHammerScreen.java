package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.SteamHammerBlockEntity;
import com.robvanblerk.tieredpower.menu.SteamHammerMenu;

public class SteamHammerScreen extends MachineScreen<SteamHammerMenu> {
	private static final int STEAM_X = 136;

	public SteamHammerScreen(SteamHammerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 68, y + 35, menu.getProgressFraction());
		drawTank(g, x + STEAM_X, y + 18, (float) menu.getSteam() / SteamHammerBlockEntity.TANK, 0xFFDDE4E8);
		text(g, "Runs on steam", x + 44, y + 62);
	}

	@Override
	protected String statusText() {
		return String.format("x%.1f speed  %d mB/t steam", menu.getSpeedMultiplier(), Math.round(SteamHammerBlockEntity.STEAM_PER_TICK * menu.getSpeedMultiplier()));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(STEAM_X, 18, 14, 52, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(String.format("Steam: %,d / %,d mB (20 mB/t while working)", menu.getSteam(), SteamHammerBlockEntity.TANK)), mouseX, mouseY);
	}
}
