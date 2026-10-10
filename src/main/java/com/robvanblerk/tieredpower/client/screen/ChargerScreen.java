package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.ChargerMenu;

public class ChargerScreen extends MachineScreen<ChargerMenu> {
	public ChargerScreen(ChargerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		// Item charge bar between the slots
		int ax = x + 66, ay = y + 38, aw = 30, ah = 10;
		g.fill(ax - 1, ay - 1, ax + aw + 1, ay + ah + 1, DARK);
		g.fill(ax, ay, ax + aw, ay + ah, 0xFF2A2A2A);
		g.fill(ax, ay, ax + aw * Math.min(100, menu.getItemCharge()) / 100, ay + ah, 0xFF5FD35F);

		text(g, menu.getRate() > 0 ? "Charging " + menu.getItemCharge() + "%" : menu.getItemCharge() >= 100 ? "Fully charged" : "Insert item", x + 8, y + 20);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
		text(g, energyText(), x + 8, y + 60);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		// Holding an item over the input slot that won't go in: say why.
		var carried = menu.getCarried();
		if (!carried.isEmpty() && isHovering(com.robvanblerk.tieredpower.menu.ChargerMenu.INPUT_X - 1, com.robvanblerk.tieredpower.menu.ChargerMenu.INPUT_Y - 1, 18, 18, mouseX, mouseY)) {
			String why = com.robvanblerk.tieredpower.block.entity.ChargerBlockEntity.whyNot(carried);
			if (why != null) g.renderTooltip(font, Component.literal(why).withStyle(net.minecraft.ChatFormatting.RED), mouseX, mouseY);
		}
	}
}
