package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.StorageBusMenu;

public class StorageBusScreen extends AbstractContainerScreen<StorageBusMenu> {
	public StorageBusScreen(StorageBusMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 184;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (var s : menu.slots) GuiDraw.slot(g, leftPos + s.x - 1, topPos + s.y - 1);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, 8, 6, 0x404040, false);
		boolean imp = menu.isImport();
		g.drawString(font, "Filter", 124, 20, 0x404040, false);
		g.drawString(font, imp ? "Empty =" : "Set what", 124, 32, 0x606060, false);
		g.drawString(font, imp ? "take all" : "to send", 124, 42, 0x606060, false);
		g.drawString(font, imp ? "Facing block -> storage" : "Storage -> facing block", 8, 74, 0x404040, false);
		g.drawString(font, menu.isOnline() ? "Connected" : "No powered Storage Controller", 8, 84, menu.isOnline() ? 0x207020 : 0xAA0000, false);
		g.drawString(font, playerInventoryTitle, 8, StorageBusMenu.INV_Y - 10, 0x404040, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
		if (menu.getCarried().isEmpty() && mouseX >= leftPos + 61 && mouseX < leftPos + 115 && mouseY >= topPos + 16 && mouseY < topPos + 70
				&& hoveredSlot != null && !hoveredSlot.hasItem())
			g.renderTooltip(font, Component.literal("Click with an item (or a bucket/tank for its fluid)"), mouseX, mouseY);
	}
}
