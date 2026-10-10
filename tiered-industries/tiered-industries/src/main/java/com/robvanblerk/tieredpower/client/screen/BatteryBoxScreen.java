package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.BatteryBoxMenu;

public class BatteryBoxScreen extends MachineScreen<BatteryBoxMenu> {
	public BatteryBoxScreen(BatteryBoxMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected boolean hasEnergyBar() {
		return false; // uses its own horizontal bar
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int bx = x + 12, by = y + 22, bw = 152, bh = 16;
		g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, DARK);
		g.fill(bx, by, bx + bw, by + bh, ENERGY_EMPTY);
		long cap = Math.max(1, menu.getCapacity());
		int filled = (int) (bw * Math.min(1.0, (double) menu.getEnergy() / cap));
		g.fill(bx, by, bx + filled, by + bh, ENERGY);
		// quarter marks
		for (int i = 1; i < 4; i++) g.fill(bx + bw * i / 4, by, bx + bw * i / 4 + 1, by + bh, 0x40000000);

		text(g, energyText(), x + 12, y + 44);
		text(g, String.format("In: %,d FE/t", menu.getRateIn()), x + 12, y + 56);
		text(g, String.format("Out: %,d FE/t", menu.getRateOut()), x + 92, y + 56);
	}
}
