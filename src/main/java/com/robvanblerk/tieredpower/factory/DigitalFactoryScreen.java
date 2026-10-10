package com.robvanblerk.tieredpower.factory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.client.screen.MachineScreen;

public class DigitalFactoryScreen extends MachineScreen<DigitalFactoryMenu> {
	private static final int ARROW_X = 66, ARROW_Y = 36, BTN_X = 8, BTN_Y = 74, BTN_W = 82, BTN_H = 14, BAR_X = 96, BAR_Y = 76, BAR_W = 74, BAR_H = 10;

	public DigitalFactoryScreen(DigitalFactoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected int extraHeight() {
		return DigitalFactoryMenu.EXTRA;
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + ARROW_X, y + ARROW_Y, menu.getProgressFraction());
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
		int bx = x + BTN_X, by = y + BTN_Y;
		g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF373737);
		g.fill(bx + 1, by + 1, bx + BTN_W, by + BTN_H, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, 0xFF8B8B8B);
		g.drawCenteredString(font, menu.getMode().label, bx + BTN_W / 2, by + 3, 0xFFFFFF);
		int cells = menu.getCells();
		text(g, cells == 0 ? "No Factory Cells attached" : cells * DigitalFactoryBlockEntity.OPS_PER_CELL + " at once (" + cells + (cells == 1 ? " cell)" : " cells)"), x + 8, y + 92);
		text(g, menu.getActive() > 0 ? menu.getActive() + " in this cycle, " + menu.getCostPerTick() + " FE/t" : statusText(), x + 8, y + 102);
		if (menu.getMode().usesWater()) {
			g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFF373737);
			g.fill(x + BAR_X + 1, y + BAR_Y + 1, x + BAR_X + BAR_W - 1, y + BAR_Y + BAR_H - 1, TANK_EMPTY);
			int w = (int) ((BAR_W - 2) * Math.min(1f, (float) menu.getWater() / DigitalFactoryBlockEntity.TANK));
			if (w > 0) g.fill(x + BAR_X + 1, y + BAR_Y + 1, x + BAR_X + 1 + w, y + BAR_Y + BAR_H - 1, 0xFF3F76E4);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY))
			g.renderComponentTooltip(font, java.util.List.of(Component.literal("Mode (click to change)"),
					Component.literal("Smelt: furnace recipes").withStyle(net.minecraft.ChatFormatting.GRAY),
					Component.literal("Crush: like the Pulverizer (2 dust per ore)").withStyle(net.minecraft.ChatFormatting.GRAY),
					Component.literal("Wash: like the Ore Purifier (3 dust per ore, water)").withStyle(net.minecraft.ChatFormatting.GRAY),
					Component.literal("Ore to Ingots: washes, then smelts (3 ingots per ore, water)").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
		if (menu.getMode().usesWater() && isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB (%d mB per ore)", menu.getWater(), DigitalFactoryBlockEntity.TANK, DigitalFactoryBlockEntity.WATER_PER_OP)), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			pressButton(DigitalFactoryMenu.BUTTON_MODE);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
