package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.StormCallerBlockEntity;
import com.robvanblerk.tieredpower.menu.StormCallerMenu;

public class StormCallerScreen extends MachineScreen<StormCallerMenu> {
	private static final String[] STATUS = {"Ready", "Charging...", "Needs a Storm Charge", "Storm in progress", "No weather here", "Off (redstone)"};

	public StormCallerScreen(StormCallerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int st = menu.getStatus();
		text(g, STATUS[Math.max(0, Math.min(STATUS.length - 1, st))], x + 8, y + 22);
		text(g, "4M FE + 1 charge = 5 min storm", x + 8, y + 58);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (menu.getCarried().isEmpty() && isHovering(79, 34, 18, 18, mouseX, mouseY) && !menu.slots.get(0).hasItem())
			g.renderTooltip(font, Component.literal("Storm Charge goes here"), mouseX, mouseY);
	}
}
