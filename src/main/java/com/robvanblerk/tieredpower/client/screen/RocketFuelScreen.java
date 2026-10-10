package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.RocketFuelMenu;

/** Cryogenic Condenser and Fuel Refinery: two input tanks, an arrow, an output tank. */
public class RocketFuelScreen extends MachineScreen<RocketFuelMenu> {
	private static final int A_X = 30, B_X = 50, OUT_X = 120;

	public RocketFuelScreen(RocketFuelMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	private String[] labels() {
		if (menu.isRefinery()) return new String[]{"Liquid Methane", "Liquid Oxygen", "Rocket Fuel"};
		String gas = switch (menu.get(5)) { case 1 -> "Methane"; case 2 -> "Oxygen"; default -> "Gas (methane or oxygen)"; };
		String liquid = switch (menu.get(6)) { case 1 -> "Liquid Methane"; case 2 -> "Liquid Oxygen"; default -> "Liquid"; };
		return new String[]{gas, "Nitrogen (coolant)", liquid};
	}

	private int[] colours() {
		if (menu.isRefinery()) return new int[]{0xFF9FD07A, 0xFF7FB8FF, 0xFFE8B040};
		int liquid = menu.get(6) == 1 ? 0xFF9FD07A : 0xFF7FB8FF;
		return new int[]{menu.get(5) == 1 ? 0xFFB8D890 : 0xFF9FD8FF, 0xFFDCE4F0, liquid};
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int t = menu.tankSize();
		int[] c = colours();
		drawTank(g, x + A_X, y + 18, (float) menu.get(2) / t, c[0]);
		drawTank(g, x + B_X, y + 18, (float) menu.get(3) / t, c[1]);
		drawTank(g, x + OUT_X, y + 18, (float) menu.get(4) / t, c[2]);
		text(g, "->", x + 86, y + 38);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		String[] l = labels();
		int t = menu.tankSize();
		int[] xs = {A_X, B_X, OUT_X};
		for (int i = 0; i < 3; i++)
			if (isHovering(xs[i], 18, 14, 52, mouseX, mouseY))
				g.renderTooltip(font, Component.literal(String.format("%s: %,d / %,d mB", l[i], menu.get(2 + i), t)), mouseX, mouseY);
	}
}
