package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.FusionReactorBlockEntity;
import com.robvanblerk.tieredpower.menu.FusionReactorMenu;

public class FusionReactorScreen extends MachineScreen<FusionReactorMenu> {
	public FusionReactorScreen(FusionReactorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int temp = menu.getTemperature();
		long charge = menu.getCharge();

		// Ignition charge bar (yellow) and plasma temperature bar (magenta)
		bar(g, x + 8, y + 18, 136, 6, (float) charge / FusionReactorBlockEntity.IGNITION_ENERGY, 0xFFFFD84A);
		bar(g, x + 8, y + 28, 136, 6, (float) temp / FusionReactorBlockEntity.MAX_TEMP, 0xFFE040FB);

		String status;
		if (temp > 0 && menu.getBurnTime() > 0) status = "Running " + menu.getGenerated() + " FE/t";
		else if (temp > 0) status = "No fuel - cooling!";
		else if (charge < FusionReactorBlockEntity.IGNITION_ENERGY) status = "Charging " + (charge * 100 / FusionReactorBlockEntity.IGNITION_ENERGY) + "%";
		else status = "Ready - add fuel";
		text(g, status, x + 8, y + 38);

		text(g, "D + T  ->  empty", x + 70, y + 57);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	private void bar(GuiGraphics g, int bx, int by, int bw, int bh, float fraction, int colour) {
		g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, DARK);
		g.fill(bx, by, bx + bw, by + bh, 0xFF2A2A2A);
		g.fill(bx, by, bx + (int) (bw * Math.min(1f, fraction)), by + bh, colour);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		int mx = mouseX - leftPos, my = mouseY - topPos;
		if (mx >= 8 && mx < 144 && my >= 18 && my < 24) {
			g.renderTooltip(font, Component.literal(String.format("Ignition: %,d / %,d FE", menu.getCharge(), FusionReactorBlockEntity.IGNITION_ENERGY)), mouseX, mouseY);
		} else if (mx >= 8 && mx < 144 && my >= 28 && my < 34) {
			g.renderTooltip(font, Component.literal("Plasma: " + menu.getTemperature() / 10 + "%"), mouseX, mouseY);
		}
	}
}
