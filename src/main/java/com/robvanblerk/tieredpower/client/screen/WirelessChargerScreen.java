package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.WirelessChargerBlockEntity;
import com.robvanblerk.tieredpower.menu.WirelessChargerMenu;

public class WirelessChargerScreen extends MachineScreen<WirelessChargerMenu> {
	public WirelessChargerScreen(WirelessChargerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		text(g, "Range: " + WirelessChargerBlockEntity.RANGE + " blocks", x + 12, y + 22);
		text(g, "Players in range: " + menu.getPlayers(), x + 12, y + 36);
		text(g, String.format("Charging: %,d FE/t", menu.getRate()), x + 12, y + 50);
		text(g, energyText(), x + 12, y + 64);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}
}
