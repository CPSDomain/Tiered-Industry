package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.ChunkLoaderMenu;

public class ChunkLoaderScreen extends MachineScreen<ChunkLoaderMenu> {
	private static final int BTN_X = 40, BTN_Y = 32, BTN_W = 40, BTN_H = 20;

	public ChunkLoaderScreen(ChunkLoaderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected String tierEffect(int tier) {
		int size = com.robvanblerk.tieredpower.block.entity.ChunkLoaderBlockEntity.MAX_RADIUS[Math.min(tier, com.robvanblerk.tieredpower.block.entity.ChunkLoaderBlockEntity.MAX_RADIUS.length - 1)] * 2 + 1;
		return "up to " + size + "x" + size;
	}

	private String sizeText() {
		int size = menu.getRadius() * 2 + 1;
		return size + "x" + size;
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int bx = x + BTN_X, by = y + BTN_Y;
		g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF373737);
		g.fill(bx + 1, by + 1, bx + BTN_W, by + BTN_H, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, 0xFF8B8B8B);
		g.drawCenteredString(font, sizeText(), bx + BTN_W / 2, by + 6, 0xFFFFFF);
		text(g, "chunks", x + BTN_X + BTN_W + 4, y + BTN_Y + 6);
		text(g, menu.isLoading() ? "Loaded" : "Not loading", x + 40, y + 20);
		text(g, menu.getCost() + " FE/t", x + 40, y + 60);
		int max = menu.getMaxRadius() * 2 + 1;
		text(g, "Max " + max + "x" + max, x + 96, y + 60);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button == 1 ? 2 : 1);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			int max = menu.getMaxRadius() * 2 + 1;
			g.renderComponentTooltip(font, java.util.List.of(
					Component.literal("Click: bigger area, right-click: smaller"),
					Component.literal("Up to " + max + "x" + max + " at this tier").withStyle(net.minecraft.ChatFormatting.GRAY),
					Component.literal("Tier Installers raise it: 7x7, 9x9, 11x11, 15x15, 19x19").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), mouseX, mouseY);
		}
	}
}
