package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.LaunchControllerBlockEntity;
import com.robvanblerk.tieredpower.menu.LaunchControllerMenu;

/** Rocket and payload slots, the fuel gauge, a checklist, the countdown and the Launch button. */
public class LaunchControllerScreen extends AbstractContainerScreen<LaunchControllerMenu> {
	private static final int FUEL_X = 52, FUEL_Y = 26, FUEL_W = 12, FUEL_H = 48;
	private Button launch;

	public LaunchControllerScreen(LaunchControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 180;
		inventoryLabelY = 87;
	}

	@Override
	protected void init() {
		super.init();
		launch = addRenderableWidget(Button.builder(Component.literal("LAUNCH"), b -> {
			if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LaunchControllerMenu.BUTTON_LAUNCH);
		}).bounds(leftPos + 112, topPos + 62, 56, 18).build());
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (var s : menu.slots) GuiDraw.slot(g, leftPos + s.x - 1, topPos + s.y - 1);
		int x = leftPos + FUEL_X, y = topPos + FUEL_Y;
		g.fill(x - 1, y - 1, x + FUEL_W + 1, y + FUEL_H + 1, 0xFF373737);
		g.fill(x, y, x + FUEL_W, y + FUEL_H, 0xFF1E2128);
		int h = (int) ((long) menu.getFuel() * FUEL_H / LaunchControllerBlockEntity.FUEL_NEEDED);
		g.fill(x, y + FUEL_H - h, x + FUEL_W, y + FUEL_H, 0xFFE8B040);
	}

	private void check(GuiGraphics g, String label, boolean ok, int y) {
		g.drawString(font, (ok ? "\u2714 " : "\u2716 ") + label, 72, y, ok ? 0x207020 : 0xAA0000, false);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, 8, 6, 0x404040, false);
		g.drawString(font, "Rocket", 8, 20, 0x606060, false);
		g.drawString(font, "Payload", 8, 74, 0x606060, false);
		int s = menu.getStructure();
		check(g, s == LaunchControllerBlockEntity.NO_PAD ? "3x3 Launch Pad" : s == LaunchControllerBlockEntity.NO_TOWER ? "Launch Tower" : "Launch site", s == LaunchControllerBlockEntity.OK, 20);
		check(g, "Rocket on pad", menu.slots.get(0).hasItem(), 30);
		check(g, "Payload", menu.slots.get(1).hasItem(), 40);
		check(g, "Fuel", menu.getFuel() >= LaunchControllerBlockEntity.FUEL_NEEDED, 50);
		check(g, "Clear sky", menu.isSkyClear(), 60);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
		String orbit = menu.getSatellites() + " in orbit, " + menu.getClaimed() + " on dishes";
		g.drawString(font, orbit, imageWidth - 8 - font.width(orbit), inventoryLabelY, 0x606060, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int cd = menu.getCountdown();
		launch.active = cd < 0;
		launch.setMessage(Component.literal(cd >= 0 ? "T-" + cd : "LAUNCH"));
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(FUEL_X, FUEL_Y, FUEL_W, FUEL_H, mouseX, mouseY))
			g.renderTooltip(font, Component.literal(String.format("Rocket Fuel: %,d / %,d mB (pipe it in)", menu.getFuel(), LaunchControllerBlockEntity.FUEL_NEEDED)), mouseX, mouseY);
		renderTooltip(g, mouseX, mouseY);
	}
}
