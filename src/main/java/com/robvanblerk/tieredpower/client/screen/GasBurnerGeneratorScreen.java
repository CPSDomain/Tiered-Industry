package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.GasBurnerGeneratorBlockEntity;
import com.robvanblerk.tieredpower.menu.GasBurnerGeneratorMenu;

public class GasBurnerGeneratorScreen extends MachineScreen<GasBurnerGeneratorMenu> {
	private static final int M_X = 96, H_X = 116, O_X = 136;

	public GasBurnerGeneratorScreen(GasBurnerGeneratorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = GasBurnerGeneratorBlockEntity.TANK;
		drawTank(g, x + M_X, y + 18, (float) menu.getMethane() / t, 0xFFB8D890);
		drawTank(g, x + H_X, y + 18, (float) menu.getHydrogen() / t, 0xFFE8EEFF);
		drawTank(g, x + O_X, y + 18, (float) menu.getOxygen() / t, 0xFF8FC8FF);
		text(g, String.format("Making %,d FE/t", menu.getGenerating()), x + 8, y + 22);
		text(g, menu.getOxygen() > 0 ? "Oxygen boost: on" : "Add oxygen: +30%", x + 8, y + 36);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int t = GasBurnerGeneratorBlockEntity.TANK;
		if (isHovering(H_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Hydrogen: %,d / %,d mB", menu.getHydrogen(), t)), mouseX, mouseY);
		else if (isHovering(M_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Methane: %,d / %,d mB", menu.getMethane(), t)), mouseX, mouseY);
		else if (isHovering(O_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Oxygen: %,d / %,d mB", menu.getOxygen(), t)), mouseX, mouseY);
	}
}
