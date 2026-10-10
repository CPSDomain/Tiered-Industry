package com.robvanblerk.tieredpower.drone;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.client.screen.MachineScreen;

public class DroneStationScreen extends MachineScreen<DroneStationMenu> {
	private static final int BTN_X = 8, BTN_Y = 56, BTN_W = 44, BTN_H = 16;

	public DroneStationScreen(DroneStationMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		int bx = x + BTN_X, by = y + BTN_Y;
		g.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF373737);
		g.fill(bx + 1, by + 1, bx + BTN_W, by + BTN_H, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, 0xFF8B8B8B);
		g.drawCenteredString(font, DroneStationBlockEntity.MODES[Math.min(menu.getMode(), 3)], bx + BTN_W / 2, by + 4, 0xFFFFFF);
		text(g, menu.getWorking() + "/" + menu.getDrones() + " out", x + 62, y + 72);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	private List<Component> modeHelp() {
		int m = menu.getMode();
		String what = switch (m) {
			case DroneStationBlockEntity.HARVEST -> "Harvest ripe crops within 12 blocks (and replant)";
			case DroneStationBlockEntity.COLLECT -> "Pick up dropped items within 12 blocks";
			case DroneStationBlockEntity.FETCH -> "Bring items from the linked inventory here";
			default -> "Take items from here to the linked inventory";
		};
		var lines = new java.util.ArrayList<Component>();
		lines.add(Component.literal("Mode: " + DroneStationBlockEntity.MODES[Math.min(m, 3)]));
		lines.add(Component.literal(what).withStyle(ChatFormatting.GRAY));
		if (m >= DroneStationBlockEntity.FETCH)
			lines.add(Component.literal(menu.isLinked() ? "Linked" : "Not linked - use a Drone Remote").withStyle(menu.isLinked() ? ChatFormatting.GREEN : ChatFormatting.RED));
		lines.add(Component.literal("Click to change").withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) g.renderComponentTooltip(font, modeHelp(), mouseX, mouseY);
	}
}
