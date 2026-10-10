package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.EnchantingMachineBlockEntity;
import com.robvanblerk.tieredpower.menu.EnchantingMachineMenu;

public class EnchantingMachineScreen extends MachineScreen<EnchantingMachineMenu> {
	private static final int BTN_X = 30, BTN_Y = 56, BTN_W = 56, BTN_H = 14;

	public EnchantingMachineScreen(EnchantingMachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 74, y + 35, menu.getProgressFraction());
		int bx = x + BTN_X, by = y + BTN_Y;
		g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF373737);
		g.fill(bx + 1, by + 1, bx + BTN_W, by + BTN_H, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, 0xFF6A4F8A);
		g.drawCenteredString(font, "Level " + menu.getLevel(), bx + BTN_W / 2, by + 3, 0xFFFFFF);
		text(g, EnchantingMachineBlockEntity.lapisFor(menu.getLevel()) + " lapis", x + 92, y + 59);
		drawTank(g, x + 139, y + 18, (float) menu.getXp() / EnchantingMachineBlockEntity.XP_TANK, 0xFF8EF04A);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mx, my)) {
			pressButton(1);
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(139, 18, 14, 52, mouseX, mouseY)) {
			int need = EnchantingMachineBlockEntity.xpCostFor(menu.getLevel());
			g.renderTooltip(font, font.split(Component.literal(String.format("Liquid Experience: %,d / %,d mB. With %,d mB or more it enchants on XP instead of power, twice as fast.",
					menu.getXp(), EnchantingMachineBlockEntity.XP_TANK, need)), 180), mouseX, mouseY);
		}
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Enchantment level (click to change) - like the enchanting table at that level"), mouseX, mouseY);
		}
	}
}
