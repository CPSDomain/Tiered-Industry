package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.BankControllerMenu;

public class BankControllerScreen extends MachineScreen<BankControllerMenu> {
	public BankControllerScreen(BankControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	/** 1234 -> "1.23 K", 5,600,000 -> "5.60 M", etc. */
	static String shortFe(long fe) {
		String[] units = {"", " K", " M", " G", " T", " P"};
		double v = fe;
		int u = 0;
		while (v >= 1000 && u < units.length - 1) {
			v /= 1000;
			u++;
		}
		return u == 0 ? String.format("%d", fe) : String.format("%.2f%s", v, units[u]);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int bx = x + 8, by = y + 20, bw = 160, bh = 16;
		g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, DARK);
		g.fill(bx, by, bx + bw, by + bh, ENERGY_EMPTY);
		long cap = Math.max(1, menu.getBankCapacity());
		int filled = (int) (bw * Math.min(1.0, (double) menu.getStored() / cap));
		g.fill(bx, by, bx + filled, by + bh, ENERGY);
		for (int i = 1; i < 4; i++) g.fill(bx + bw * i / 4, by, bx + bw * i / 4 + 1, by + bh, 0x40000000);

		if (!menu.isFormed()) {
			text(g, "Not formed - right-click for info", x + 8, y + 42);
			return;
		}
		double pct = 100.0 * menu.getStored() / cap;
		text(g, shortFe(menu.getStored()) + "FE / " + shortFe(menu.getBankCapacity()) + "FE", x + 8, y + 40);
		text(g, String.format("%.1f%%", pct), x + 136, y + 40);
		text(g, "In:  " + shortFe(menu.getRateIn()) + "FE/t", x + 8, y + 52);
		text(g, "Out: " + shortFe(menu.getRateOut()) + "FE/t", x + 8, y + 62);
		text(g, menu.getCells() + " cells", x + 120, y + 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(8, 20, 160, 16, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("%,d / %,d FE", menu.getStored(), menu.getBankCapacity())), mouseX, mouseY);
		}
	}
}
