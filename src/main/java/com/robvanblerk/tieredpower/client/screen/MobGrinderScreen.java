package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.MobGrinderMenu;

public class MobGrinderScreen extends MachineScreen<MobGrinderMenu> {
	private static final int MODE_X = 30, MODE_Y = 18, XP_X = 30, XP_Y = 50, BTN_W = 62, BTN_H = 14;

	public MobGrinderScreen(MobGrinderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	private void button(GuiGraphics g, int bx, int by, String label, int colour) {
		g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF373737);
		g.fill(bx + 1, by + 1, bx + BTN_W, by + BTN_H, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, colour);
		g.drawCenteredString(font, label, bx + BTN_W / 2, by + 3, 0xFFFFFF);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		button(g, x + MODE_X, y + MODE_Y, menu.isAllMobs() ? "All mobs" : "Hostile", menu.isAllMobs() ? 0xFF8A4F4F : 0xFF4F6A8A);
		text(g, String.format("%,d kills", menu.getKills()), x + 30, y + 36);
		button(g, x + XP_X, y + XP_Y, String.format("XP %,d", menu.getXp()), 0xFF4F8A4F);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (isHovering(MODE_X, MODE_Y, BTN_W, BTN_H, mx, my)) { pressButton(1); return true; }
		if (isHovering(XP_X, XP_Y, BTN_W, BTN_H, mx, my)) { pressButton(2); return true; }
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(MODE_X, MODE_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Targets: hostile mobs only, or all mobs (never players, bosses, named mobs or pets)"), mouseX, mouseY);
		} else if (isHovering(XP_X, XP_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Click to take the stored XP"), mouseX, mouseY);
		}
	}
}
