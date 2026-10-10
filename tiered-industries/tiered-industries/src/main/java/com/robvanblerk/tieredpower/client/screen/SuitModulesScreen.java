package com.robvanblerk.tieredpower.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

import com.robvanblerk.tieredpower.item.powered.ItemEnergy;
import com.robvanblerk.tieredpower.item.powered.SuitModules;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.SuitModulePacket;

/** The Suit Modules key: every module fitted to the Quantum Suit you're wearing, with On/Off and Remove. */
public class SuitModulesScreen extends Screen {
	private static final int W = 260, ROW_H = 20;
	private List<SuitModules.Module> rows = new ArrayList<>();
	private int refresh;

	public SuitModulesScreen() {
		super(Component.literal("Quantum Suit modules"));
	}

	private int left() { return (width - W) / 2; }
	private int top() { return (height - height()) / 2; }
	private int height() { return 40 + Math.max(1, rows.size()) * ROW_H; }

	@Override
	protected void init() {
		rows = new ArrayList<>();
		if (minecraft == null || minecraft.player == null) return;
		for (SuitModules.Module m : SuitModules.Module.values())
			if (SuitModules.installed(minecraft.player.getItemBySlot(m.slot), m)) rows.add(m);
		int y = top() + 24;
		for (SuitModules.Module m : rows) {
			boolean on = SuitModules.switchedOn(minecraft.player.getItemBySlot(m.slot), m);
			addRenderableWidget(Button.builder(Component.literal(on ? "On" : "Off"), b -> send(m, 0)).bounds(left() + W - 112, y, 46, 16).build());
			addRenderableWidget(Button.builder(Component.literal("Remove"), b -> send(m, 1)).bounds(left() + W - 62, y, 54, 16).build());
			y += ROW_H;
		}
	}

	private void send(SuitModules.Module m, int action) {
		ModNetwork.CHANNEL.sendToServer(new SuitModulePacket(m.id, action));
		refresh = 3; // rebuild once the server's change has synced back
	}

	@Override
	public void tick() {
		if (refresh > 0 && --refresh == 0) rebuildWidgets();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int x = left(), y = top();
		GuiDraw.panel(g, x, y, W, height());
		g.drawString(font, title, x + 8, y + 8, 0x404040, false);
		if (rows.isEmpty()) {
			g.drawString(font, "No modules fitted. Wear a Quantum Suit piece", x + 8, y + 26, 0x707070, false);
			g.drawString(font, "and right-click a module to fit it.", x + 8, y + 36, 0x707070, false);
		}
		int ry = y + 24;
		for (SuitModules.Module m : rows) {
			var armor = minecraft.player.getItemBySlot(m.slot);
			boolean charged = ItemEnergy.get(armor) > 0;
			String piece = switch (m.slot) { case HEAD -> "Helmet"; case CHEST -> "Chest"; case LEGS -> "Legs"; default -> "Boots"; };
			g.drawString(font, m.title, x + 8, ry + 4, 0x2050B0, false);
			g.drawString(font, piece + (charged ? "" : " (no charge)"), x + 92, ry + 4, charged ? 0x707070 : 0xAA0000, false);
			ry += ROW_H;
		}
		super.render(g, mouseX, mouseY, partialTick);
		for (int i = 0; i < rows.size(); i++) {
			int rowY = y + 24 + i * ROW_H;
			if (mouseX >= x + 8 && mouseX < x + W - 116 && mouseY >= rowY && mouseY < rowY + 16)
				g.renderTooltip(font, Component.literal(rows.get(i).description), mouseX, mouseY);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
