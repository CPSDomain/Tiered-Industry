package com.robvanblerk.tieredpower.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.energy.PowerInfo;
import com.robvanblerk.tieredpower.network.CraftPlanPacket;
import com.robvanblerk.tieredpower.network.CraftRequestPacket;
import com.robvanblerk.tieredpower.network.ModNetwork;

/**
 * "How many do you want?" for an autocraft. Shows the plan from the server - what comes from storage, what gets
 * crafted, and anything missing - and a Start button when it's all possible. Esc goes back to the terminal.
 */
public class CraftRequestScreen extends Screen {
	private static final int W = 220, H = 226, LIST_ROWS = 7, ROW_H = 16, LIST_Y = 78, LIST_H = LIST_ROWS * ROW_H + 4;
	private final Screen parent;
	private final int containerId;
	private final ItemStack item;

	private EditBox amount;
	private Button start;
	private List<CraftPlanPacket.Line> lines = List.of();
	private String message = "Working it out...";
	private boolean planned, possible;
	private String plannedFor = "";
	private int scroll;

	public CraftRequestScreen(Screen parent, int containerId, ItemStack item) {
		super(Component.literal("Craft"));
		this.parent = parent;
		this.containerId = containerId;
		this.item = item.copyWithCount(1);
	}

	private int left() { return (width - W) / 2; }
	private int top() { return (height - H) / 2; }

	private long amountValue() {
		try {
			return Math.max(1, Math.min(1_000_000, Long.parseLong(amount.getValue().trim())));
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private void setAmount(long n) {
		amount.setValue(String.valueOf(Math.max(1, Math.min(1_000_000, n))));
		requestPlan();
	}

	private void requestPlan() {
		planned = false;
		message = "Working it out...";
		plannedFor = amount.getValue();
		ModNetwork.CHANNEL.sendToServer(new CraftRequestPacket(containerId, item, amountValue(), false));
	}

	@Override
	protected void init() {
		int x = left(), y = top();
		amount = new EditBox(font, x + 60, y + 28, 60, 14, Component.literal("Amount"));
		amount.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
		amount.setMaxLength(7);
		if (amount.getValue().isEmpty()) amount.setValue("1");
		addRenderableWidget(amount);
		setInitialFocus(amount);
		int bx = x + 8;
		for (long step : new long[]{-64, -10, -1}) {
			addRenderableWidget(Button.builder(Component.literal(String.valueOf(step)), b -> setAmount(amountValue() + step)).bounds(bx, y + 46, 30, 14).build());
			bx += 32;
		}
		for (long step : new long[]{1, 10, 64}) {
			addRenderableWidget(Button.builder(Component.literal("+" + step), b -> setAmount(amountValue() + step)).bounds(bx, y + 46, 30, 14).build());
			bx += 32;
		}
		addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(x + 8, y + H - 22, 60, 16).build());
		start = addRenderableWidget(Button.builder(Component.literal("Start"), b -> {
			if (planned && possible) ModNetwork.CHANNEL.sendToServer(new CraftRequestPacket(containerId, item, amountValue(), true));
		}).bounds(x + W - 68, y + H - 22, 60, 16).build());
		requestPlan();
	}

	/** The server's answer. */
	public void receive(CraftPlanPacket plan) {
		if (plan.started()) {
			onClose(); // back to the terminal - the chat says what was queued
			return;
		}
		lines = plan.lines();
		message = plan.message();
		possible = plan.message().equals("Ready to craft");
		planned = true;
		scroll = 0;
	}

	@Override
	public void tick() {
		amount.tick();
		if (!amount.getValue().equals(plannedFor) && !amount.getValue().isEmpty()) requestPlan(); // typed a new number
		start.active = planned && possible;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		scroll = (int) Math.max(0, Math.min(Math.max(0, lines.size() - LIST_ROWS), scroll - Math.signum(delta)));
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int x = left(), y = top();
		GuiDraw.panel(g, x, y, W, H);
		g.renderItem(item, x + 8, y + 7);
		g.drawString(font, "Craft " + item.getHoverName().getString(), x + 28, y + 11, 0x404040, false);
		g.drawString(font, "Amount", x + 8, y + 31, 0x404040, false);
		String shown = message;
		int room = W - 16 - (lines.size() > LIST_ROWS ? font.width("00-00 of 00") + 8 : 0);
		if (font.width(shown) > room) shown = font.plainSubstrByWidth(shown, room - 8) + "...";
		g.drawString(font, shown, x + 8, y + 64, possible ? 0x207020 : (planned ? 0xAA0000 : 0x606060), false);
		// The plan, in its own inset box that stops above the buttons (drawing is clipped to it).
		int bx = x + 7, by = y + LIST_Y, bw = W - 14;
		g.fill(bx, by, bx + bw, by + LIST_H, 0xFF373737);
		g.fill(bx + 1, by + 1, bx + bw, by + LIST_H, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + bw - 1, by + LIST_H - 1, 0xFFB4B4B4);
		if (lines.size() > LIST_ROWS) {
			String range = (scroll + 1) + "-" + Math.min(lines.size(), scroll + LIST_ROWS) + " of " + lines.size();
			g.drawString(font, range, x + W - 8 - font.width(range), y + 64, 0x707070, false);
			int track = LIST_H - 4, thumb = Math.max(10, track * LIST_ROWS / lines.size());
			int ty = by + 2 + (track - thumb) * scroll / Math.max(1, lines.size() - LIST_ROWS);
			g.fill(bx + bw - 5, by + 2, bx + bw - 2, by + LIST_H - 2, 0xFF8B8B8B);
			g.fill(bx + bw - 5, ty, bx + bw - 2, ty + thumb, 0xFF505050);
		}
		g.enableScissor(bx + 1, by + 1, bx + bw - 1, by + LIST_H - 1);
		int ly = by + 2;
		for (int i = scroll; i < lines.size() && i < scroll + LIST_ROWS; i++) {
			CraftPlanPacket.Line l = lines.get(i);
			var fluid = com.robvanblerk.tieredpower.item.FluidDropItem.fluid(l.item());
			if (fluid.isEmpty()) g.renderItem(l.item(), x + 10, ly); else GuiDraw.fluid(g, fluid, x + 10, ly);
			String what = switch (l.kind()) {
				case CraftPlanPacket.CRAFTED -> "craft";
				case CraftPlanPacket.MISSING -> "MISSING";
				default -> "from storage";
			};
			int colour = l.kind() == CraftPlanPacket.MISSING ? 0xAA0000 : l.kind() == CraftPlanPacket.CRAFTED ? 0x2050B0 : 0x404040;
			String name = l.item().getHoverName().getString();
			if (font.width(name) > 100) name = font.plainSubstrByWidth(name, 94) + "...";
			String amount = fluid.isEmpty() ? PowerInfo.shortFe(l.count()) : GuiDraw.fluidAmount(l.count());
			g.drawString(font, amount + " " + name, x + 30, ly + 4, colour, false);
			g.drawString(font, what, x + W - 16 - font.width(what), ly + 4, colour, false);
			ly += ROW_H;
		}
		g.disableScissor();
		super.render(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (key == 257 || key == 335) { // enter
			if (planned && possible) ModNetwork.CHANNEL.sendToServer(new CraftRequestPacket(containerId, item, amountValue(), true));
			return true;
		}
		return super.keyPressed(key, scan, modifiers);
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
