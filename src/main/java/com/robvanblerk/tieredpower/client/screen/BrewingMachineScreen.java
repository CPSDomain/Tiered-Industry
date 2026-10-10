package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.BrewingMachineBlockEntity;
import com.robvanblerk.tieredpower.menu.BrewingMachineMenu;

public class BrewingMachineScreen extends MachineScreen<BrewingMachineMenu> {
	private static final int WATER_X = 20;

	public BrewingMachineScreen(BrewingMachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawTank(g, x + WATER_X, y + 18, (float) menu.getWater() / BrewingMachineBlockEntity.TANK, 0xFF3F76E4);
		drawArrow(g, x + 90, y + 34, menu.progress());
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(WATER_X, 18, 14, 52, mouseX, mouseY))
			g.renderTooltip(font, font.split(Component.literal(String.format("Water: %,d / %,d mB - fills glass bottles (333 mB each). Pipe it in or right-click with a bucket.",
					menu.getWater(), BrewingMachineBlockEntity.TANK)), 180), mouseX, mouseY);
		for (int i = 0; i < 4; i++) {
			var s = menu.slots.get(i);
			if (menu.getCarried().isEmpty() && !s.hasItem() && isHovering(s.x - 1, s.y - 1, 18, 18, mouseX, mouseY))
				g.renderTooltip(font, Component.literal(i < 3 ? "Bottle: glass bottle, water or a potion" : "Ingredient (nether wart, sugar...)"), mouseX, mouseY);
		}
	}
}
