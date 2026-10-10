package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.SmithingPressMenu;

public class SmithingPressScreen extends MachineScreen<SmithingPressMenu> {
	private static final String[] HINTS = {"Template (e.g. Netherite Upgrade)", "Item to upgrade", "Material (e.g. Netherite Ingot)"};

	public SmithingPressScreen(SmithingPressMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 86, y + 35, menu.progress());
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		for (int i = 0; i < 3; i++)
			if (menu.getCarried().isEmpty() && !menu.slots.get(i).hasItem() && isHovering(25 + i * 18, 34, 18, 18, mouseX, mouseY))
				g.renderTooltip(font, Component.literal(HINTS[i]), mouseX, mouseY);
	}
}
