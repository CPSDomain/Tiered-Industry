package com.robvanblerk.tieredpower.spatial;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.client.screen.MachineScreen;

public class SpatialProjectorScreen extends MachineScreen<SpatialProjectorMenu> {
	private static final int BTN_X = 72, BTN_Y = 33, BTN_W = 60, BTN_H = 20;

	public SpatialProjectorScreen(SpatialProjectorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int n = menu.getSize();
		text(g, n == 0 ? "Insert a Spatial Cell" : "Area: " + n + "x" + n + "x" + n + " above", x + 8, y + 20);
		int bx = x + BTN_X, by = y + BTN_Y;
		boolean active = n > 0;
		g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF373737);
		g.fill(bx + 1, by + 1, bx + BTN_W, by + BTN_H, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, active ? 0xFF6A4F8A : 0xFF8B8B8B);
		g.drawCenteredString(font, menu.isCellFull() ? "Deploy" : "Capture", bx + BTN_W / 2, by + 6, active ? 0xFFFFFF : 0xA0A0A0);
		text(g, "40 FE per block", x + 72, y + 58);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (menu.getSize() > 0 && isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(menu.isCellFull() ? "Put the captured space back down above the projector (the area must be empty)"
					: "Capture every block in the outlined area into the cell"), mouseX, mouseY);
	}
}
