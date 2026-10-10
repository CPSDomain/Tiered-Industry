package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.ItemFilterMenu;

public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterMenu> {
	private static final int BTN_X = 124, BTN_Y = 24, BTN_W = 44, BTN_H = 18, MODE_Y = 46;

	public ItemFilterScreen(ItemFilterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(menu.isWhitelist() ? "Only matching items pass" : "Everything EXCEPT matching items passes"), mouseX, mouseY);
		} else if (isHovering(BTN_X, MODE_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			String tip = switch (menu.getMode()) {
				case 1 -> "Tag: matches anything sharing a tag with these items (e.g. one raw ore = all raw ores)";
				case 2 -> "Mod: matches anything from the same mod as these items";
				default -> "Item: matches exactly these items";
			};
			g.renderTooltip(font, Component.literal(tip + " (click to change)"), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		g.blit(MachineScreen.BASE, x, y, 0, 0, imageWidth, imageHeight);
		for (int i = 0; i < 9; i++) {
			g.blit(MachineScreen.WIDGETS, x + ItemFilterMenu.GRID_X + (i % 3) * 18 - 1, y + ItemFilterMenu.GRID_Y + (i / 3) * 18 - 1, 0, 0, 18, 18);
		}
		int bx = x + BTN_X, by = y + BTN_Y;
		g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF373737);
		g.fill(bx + 1, by + 1, bx + BTN_W, by + BTN_H, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, menu.isWhitelist() ? 0xFF4F8A4F : 0xFF8A4F4F);
		g.drawCenteredString(font, menu.isWhitelist() ? "Allow" : "Block", bx + BTN_W / 2, by + 5, 0xFFFFFF);
		int my = y + MODE_Y;
		g.fill(bx, my, bx + BTN_W, my + BTN_H, 0xFF373737);
		g.fill(bx + 1, my + 1, bx + BTN_W, my + BTN_H, 0xFFFFFFFF);
		g.fill(bx + 1, my + 1, bx + BTN_W - 1, my + BTN_H - 1, 0xFF4F5F8A);
		g.drawCenteredString(font, com.robvanblerk.tieredpower.item.ItemFilterItem.MODES[menu.getMode()], bx + BTN_W / 2, my + 5, 0xFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isHovering(BTN_X, MODE_Y, BTN_W, BTN_H, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
