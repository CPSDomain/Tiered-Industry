package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.ParticleColliderBlockEntity;
import com.robvanblerk.tieredpower.menu.ParticleColliderMenu;

public class ParticleColliderScreen extends MachineScreen<ParticleColliderMenu> {
	private static final int D_X = 30, T_X = 50, A_X = 120;

	public ParticleColliderScreen(ParticleColliderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = ParticleColliderBlockEntity.TANK;
		drawTank(g, x + D_X, y + 18, (float) menu.getDeuterium() / t, 0xFFC8E0FF);
		drawTank(g, x + T_X, y + 18, (float) menu.getTritium() / t, 0xFFB8FFC8);
		drawTank(g, x + A_X, y + 18, (float) menu.getAntimatter() / t, 0xFFFF60D0);
		text(g, "D", x + D_X + 4, y + 72);
		text(g, "T", x + T_X + 4, y + 72);
		text(g, "AM", x + A_X + 1, y + 72);
		text(g, "->", x + 84, y + 38);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int t = ParticleColliderBlockEntity.TANK;
		if (isHovering(D_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Deuterium: %,d / %,d mB (10 per mB of antimatter)", menu.getDeuterium(), t)), mouseX, mouseY);
		else if (isHovering(T_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Tritium: %,d / %,d mB (10 per mB of antimatter)", menu.getTritium(), t)), mouseX, mouseY);
		else if (isHovering(A_X, 18, 14, 52, mouseX, mouseY)) g.renderTooltip(font, Component.literal(String.format("Antimatter: %,d / %,d mB", menu.getAntimatter(), t)), mouseX, mouseY);
	}
}
