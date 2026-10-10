package com.robvanblerk.tieredpower.industry;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.client.screen.MachineScreen;

public class BioRefineryScreen extends MachineScreen<BioRefineryMenu> {
	private static final int OIL_X = 72, ETH_X = 92, BIO_X = 128;

	public BioRefineryScreen(BioRefineryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = BioRefineryBlockEntity.TANK;
		drawArrow(g, x + 46, y + 35, menu.progress());
		drawTank(g, x + OIL_X, y + 18, (float) menu.get(2) / t, 0xFFD8C040);
		drawTank(g, x + ETH_X, y + 18, (float) menu.get(3) / t, 0xFFE8E8D0);
		drawTank(g, x + BIO_X, y + 18, (float) menu.get(4) / t, 0xFFB89020);
		text(g, "+", x + 109, y + 38);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int t = BioRefineryBlockEntity.TANK;
		String[] names = {"Plant Oil (from seeds)", "Ethanol (from sugary crops)", "Biodiesel (pushed out to pipes)"};
		int[] xs = {OIL_X, ETH_X, BIO_X};
		for (int i = 0; i < 3; i++)
			if (isHovering(xs[i], 18, 14, 52, mouseX, mouseY))
				g.renderTooltip(font, Component.literal(String.format("%s: %,d / %,d mB", names[i], menu.get(2 + i), t)), mouseX, mouseY);
		var s0 = menu.slots.get(0);
		if (menu.getCarried().isEmpty() && !s0.hasItem() && isHovering(s0.x - 1, s0.y - 1, 18, 18, mouseX, mouseY))
			g.renderTooltip(font, font.split(Component.literal("Seeds or sunflowers (oil), or sugar, sugar cane, wheat, potatoes, apples, berries, honey... (ethanol)"), 180), mouseX, mouseY);
	}
}
