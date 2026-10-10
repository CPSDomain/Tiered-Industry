package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.FluidMixerBlockEntity;
import com.robvanblerk.tieredpower.menu.FluidMixerMenu;

public class FluidMixerScreen extends MachineScreen<FluidMixerMenu> {
	private static final int LAVA_X = 116, WATER_X = 136;

	public FluidMixerScreen(FluidMixerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 54, y + 35, menu.getProgressFraction());
		drawTank(g, x + LAVA_X, y + 18, (float) menu.getLava() / FluidMixerBlockEntity.TANK, 0xFFE5641A);
		drawTank(g, x + WATER_X, y + 18, (float) menu.getWater() / FluidMixerBlockEntity.TANK, 0xFF3F76E4);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
		text(g, statusText(), x + 8, y + 62);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(LAVA_X, 18, 14, 52, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("Lava: %,d / %,d mB", menu.getLava(), FluidMixerBlockEntity.TANK)), mouseX, mouseY);
		} else if (isHovering(WATER_X, 18, 14, 52, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB", menu.getWater(), FluidMixerBlockEntity.TANK)), mouseX, mouseY);
		} else if (isHovering(29, 34, 18, 18, mouseX, mouseY) && menu.getCarried().isEmpty() && !menu.slots.get(0).hasItem()) {
			g.renderTooltip(font, Component.literal("Concrete powder or dirt - or leave empty to make obsidian from lava + water"), mouseX, mouseY);
		}
	}
}
