package com.robvanblerk.tieredpower.industry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.client.screen.MachineScreen;

public class HeavyFurnaceScreen extends MachineScreen<HeavyFurnaceMenu> {
	private static final int TANK_X = 140;

	public HeavyFurnaceScreen(HeavyFurnaceMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + 70, y + 35, menu.progress());
		if (menu.isCokeOven()) drawTank(g, x + TANK_X, y + 18, (float) menu.getCreosote() / HeavyFurnaceBlockEntity.TANK, 0xFF3A2A12);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (menu.isCokeOven() && isHovering(TANK_X, 18, 14, 52, mouseX, mouseY))
			g.renderTooltip(font, font.split(Component.literal(String.format("Creosote Oil: %,d / %,d mB - take it out with pipes or a bucket", menu.getCreosote(), HeavyFurnaceBlockEntity.TANK)), 180), mouseX, mouseY);
		var hint = menu.isCokeOven() ? "Coal (makes Coal Coke) or logs (charcoal)" : null;
		var s0 = menu.slots.get(0);
		if (menu.getCarried().isEmpty() && !s0.hasItem() && isHovering(s0.x - 1, s0.y - 1, 18, 18, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(hint != null ? hint : "Iron ingot"), mouseX, mouseY);
		var s1 = menu.slots.get(1);
		if (!menu.isCokeOven() && menu.getCarried().isEmpty() && !s1.hasItem() && isHovering(s1.x - 1, s1.y - 1, 18, 18, mouseX, mouseY))
			g.renderTooltip(font, Component.literal("Coal Coke"), mouseX, mouseY);
	}
}
