package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.FusionControllerBlockEntity;
import com.robvanblerk.tieredpower.menu.FusionControllerMenu;

public class FusionControllerScreen extends MachineScreen<FusionControllerMenu> {
	public FusionControllerScreen(FusionControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int temp = menu.getTemperature();
		long charge = menu.getCharge();

		bar(g, x + 8, y + 18, 136, 6, (float) charge / FusionControllerBlockEntity.IGNITION_ENERGY, 0xFFFFD84A);
		bar(g, x + 8, y + 28, 136, 6, (float) temp / FusionControllerBlockEntity.MAX_TEMP, 0xFFE040FB);

		String status;
		if (!menu.isFormed()) status = "Not formed - right-click for info";
		else if (temp > 0 && menu.getBurnTime() > 0) status = String.format("Running %,d FE/t", menu.getGenerated());
		else if (temp > 0) status = "No fuel - cooling!";
		else if (charge < FusionControllerBlockEntity.IGNITION_ENERGY) status = "Charging " + (charge * 100 / FusionControllerBlockEntity.IGNITION_ENERGY) + "%";
		else status = "Ready - add fuel";
		text(g, status, x + 8, y + 38);

		if (menu.isFormed()) {
			text(g, menu.getCoils() + " coils", x + 70, y + 52);
			text(g, String.format("max %,d", menu.getMaxOutput()), x + 70, y + 62);
		}
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
			g.renderTooltip(font, Component.literal(String.format("Ignition: %,d / %,d FE", menu.getCharge(), FusionControllerBlockEntity.IGNITION_ENERGY)), mouseX, mouseY);
		} else if (mx >= 8 && mx < 144 && my >= 28 && my < 34) {
			g.renderTooltip(font, Component.literal("Plasma: " + menu.getTemperature() / 10 + "%"), mouseX, mouseY);
		}
	}
}
