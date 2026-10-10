package com.robvanblerk.tieredpower.quantum;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.client.screen.GuiDraw;

/** Quantum Chest window: drawn without a texture (vanilla-style panel and slots). */
public class QuantumChestScreen extends AbstractContainerScreen<QuantumChestMenu> {
	private static final int BTN_W = 52, BTN_H = 12, BTN_X = 176 - 7 - BTN_W, BTN_Y = 4;

	public QuantumChestScreen(QuantumChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = QuantumChestMenu.HEIGHT;
		inventoryLabelY = QuantumChestMenu.INV_Y - 11;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
		if (!menu.isMain() && isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(menu.isReceive()
					? "Receive: the main chest sends this chest the filtered items. Click for Send."
					: "Send: items put in this chest go to the main chest. Click for Receive."), mouseX, mouseY);
		} else if (!menu.isMain() && hoveredSlot == null && isHovering(8, QuantumChestMenu.FILTER_Y, 162, 18, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(menu.isReceive()
					? "Filter: the items this chest receives (needs at least one). Click with an item to set, empty hand to clear."
					: "Filter: only these items are sent (empty = send everything)."), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		GuiDraw.panel(g, x, y, imageWidth, imageHeight);
		if (!menu.isMain()) {
			for (int i = 0; i < QuantumChestMenu.FILTER; i++) {
				int sx = x + 7 + i * 18, sy = y + QuantumChestMenu.FILTER_Y - 1;
				GuiDraw.slot(g, sx, sy);
				g.fill(sx + 1, sy + 1, sx + 17, sy + 17, menu.isReceive() && menu.filterEmpty() ? 0x60C03030 : 0x405A2D8A); // ghost slots: purple, red = Receive needs a filter
			}
		}
		for (int row = 0; row < 6; row++)
			for (int col = 0; col < 9; col++) GuiDraw.slot(g, x + 7 + col * 18, y + QuantumChestMenu.CHEST_Y - 1 + row * 18);
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) GuiDraw.slot(g, x + 7 + col * 18, y + QuantumChestMenu.INV_Y - 1 + row * 18);
		for (int col = 0; col < 9; col++) GuiDraw.slot(g, x + 7 + col * 18, y + QuantumChestMenu.INV_Y + 57);
		if (!menu.isMain()) {
			int bx = x + BTN_X, by = y + BTN_Y;
			g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF373737);
			g.fill(bx + 1, by + 1, bx + BTN_W, by + BTN_H, 0xFFFFFFFF);
			g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, menu.isReceive() ? 0xFF2D6E8A : 0xFF6A3D9A);
			g.drawCenteredString(font, menu.isReceive() ? "Receive" : "Send", bx + BTN_W / 2, by + 2, 0xFFFFFF);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
		g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
		if (menu.isMain()) {
			int n = menu.sending() + menu.receiving();
			String line = n == 0 ? "No chests linked yet" : n + " linked: " + menu.sending() + " sending, " + menu.receiving() + " receiving";
			g.drawString(font, line, 8, QuantumChestMenu.FILTER_Y + 5, 0x5A2D8A, false);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!menu.isMain() && isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, QuantumChestMenu.BUTTON_MODE);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
