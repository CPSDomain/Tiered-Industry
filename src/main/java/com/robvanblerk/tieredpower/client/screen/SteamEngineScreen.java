package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.SteamEngineBlockEntity;
import com.robvanblerk.tieredpower.menu.SteamEngineMenu;

public class SteamEngineScreen extends MachineScreen<SteamEngineMenu> {
	private static final int STEAM_X = 136;

	public SteamEngineScreen(SteamEngineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawTank(g, x + STEAM_X, y + 18, (float) menu.getSteam() / SteamEngineBlockEntity.TANK, 0xFFDDE4E8);
		text(g, String.format("Making %,d FE/t", menu.getGenerating()), x + 8, y + 22);
		text(g, "1 FE per mB of steam", x + 8, y + 36);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(STEAM_X, 18, 14, 52, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(String.format("Steam: %,d / %,d mB", menu.getSteam(), SteamEngineBlockEntity.TANK)), mouseX, mouseY);
	}
}
