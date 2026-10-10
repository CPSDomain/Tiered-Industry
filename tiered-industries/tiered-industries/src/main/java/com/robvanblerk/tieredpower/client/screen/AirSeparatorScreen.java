package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.AirSeparatorBlockEntity;
import com.robvanblerk.tieredpower.menu.AirSeparatorMenu;

public class AirSeparatorScreen extends MachineScreen<AirSeparatorMenu> {
	private static final int N_X = 96, O_X = 122;

	public AirSeparatorScreen(AirSeparatorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = AirSeparatorBlockEntity.TANK;
		drawTank(g, x + N_X, y + 18, (float) menu.getNitrogen() / t, 0xFFDCE4F0);
		drawTank(g, x + O_X, y + 18, (float) menu.getOxygen() / t, 0xFF8FC8FF);
		text(g, "N2", x + N_X + 2, y + 72);
		text(g, "O2", x + O_X + 2, y + 72);
		text(g, "Air -> N2 + O2", x + 30, y + 22);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int t = AirSeparatorBlockEntity.TANK;
		if (isHovering(N_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Nitrogen: %,d / %,d mB", menu.getNitrogen(), t)), mouseX, mouseY);
		else if (isHovering(O_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Oxygen: %,d / %,d mB", menu.getOxygen(), t)), mouseX, mouseY);
	}
}
