package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.FissionReactorBlockEntity;
import com.robvanblerk.tieredpower.menu.FissionReactorMenu;

public class FissionReactorScreen extends MachineScreen<FissionReactorMenu> {
	private static final int HEAT_X = 48, BAR_Y = 18, WATER_X = 136;

	public FissionReactorScreen(FissionReactorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		// Heat bar: green up to the 50% sweet spot, orange above, red near the limit.
		float heat = (float) menu.getHeat() / FissionReactorBlockEntity.MAX_HEAT;
		int colour = heat < 0.5f ? 0xFF4FB84F : heat < 0.85f ? 0xFFE8A030 : 0xFFE03A2E;
		drawTank(g, x + HEAT_X, y + BAR_Y, heat, colour);
		g.fill(x + HEAT_X - 2, y + BAR_Y + 26, x + HEAT_X + 16, y + BAR_Y + 27, 0xFFFFFFFF); // 50% mark

		String status = menu.isScrammed() ? "SCRAM!" : menu.getActiveRods() > 0 ? "Running" : "Idle";
		text(g, status, x + 68, y + 20);
		text(g, "Heat " + menu.getHeat() / 10 + "%", x + 68, y + 32);
		text(g, String.format("%,d FE/t", menu.getGenerating()), x + 68, y + 44);
		text(g, menu.getActiveRods() + " rods", x + 68, y + 56);

		drawTank(g, x + WATER_X, y + BAR_Y, (float) menu.getWater() / FissionReactorBlockEntity.WATER_CAPACITY, 0xFF3F76E4);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(HEAT_X, BAR_Y, 14, 52, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Heat: full power from 50%; water cooling holds it there. SCRAM at 100%."), mouseX, mouseY);
		} else if (isHovering(WATER_X, BAR_Y, 14, 52, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("Water: %,d mB  |  Steam out: %,d mB", menu.getWater(), menu.getSteam())), mouseX, mouseY);
		}
	}
}
