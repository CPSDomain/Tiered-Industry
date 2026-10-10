package com.robvanblerk.tieredpower.client.screen;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import com.robvanblerk.tieredpower.energy.PowerInfo;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.PowerHistoryRequestPacket;

/** A Power Monitor's history: generated (green) and used (red) FE/t, battery fill (blue). Refreshes every 5 seconds. */
public class PowerHistoryScreen extends Screen {
	private static final int W = 260, H = 170, GX = 34, GY = 22, GW = 214, GH = 110;
	private final BlockPos pos;
	private boolean longRange;
	private List<long[]> samples;
	private int ticks;
	private Button rangeButton;

	public PowerHistoryScreen(BlockPos pos, boolean longRange, List<long[]> samples) {
		super(Component.literal("Power history"));
		this.pos = pos;
		this.longRange = longRange;
		this.samples = samples;
	}

	public BlockPos pos() { return pos; }

	public void receive(boolean longRange, List<long[]> samples) {
		this.longRange = longRange;
		this.samples = samples;
		if (rangeButton != null) rangeButton.setMessage(Component.literal(longRange ? "Last 2 hours" : "Last 10 minutes"));
	}

	private int left() { return (width - W) / 2; }
	private int top() { return (height - H) / 2; }

	@Override
	protected void init() {
		rangeButton = addRenderableWidget(Button.builder(Component.literal(longRange ? "Last 2 hours" : "Last 10 minutes"),
				b -> ModNetwork.CHANNEL.sendToServer(new PowerHistoryRequestPacket(pos, !longRange))).bounds(left() + W - 108, top() + 4, 100, 14).build());
	}

	@Override
	public void tick() {
		if (++ticks % 100 == 0) ModNetwork.CHANNEL.sendToServer(new PowerHistoryRequestPacket(pos, longRange));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int x = left(), y = top();
		GuiDraw.panel(g, x, y, W, H);
		g.drawString(font, title, x + 8, y + 7, 0x404040, false);
		int gx = x + GX, gy = y + GY;
		g.fill(gx - 1, gy - 1, gx + GW + 1, gy + GH + 1, 0xFF373737);
		g.fill(gx, gy, gx + GW, gy + GH, 0xFF1E2128);
		long max = 1;
		for (long[] s : samples) max = Math.max(max, Math.max(s[0], s[1]));
		for (int i = 1; i < 4; i++) g.fill(gx, gy + GH * i / 4, gx + GW, gy + GH * i / 4 + 1, 0xFF2C313B);
		g.drawString(font, PowerInfo.shortFe(max), x + 4, gy - 4, 0x606060, false);
		g.drawString(font, "0", x + 24, gy + GH - 4, 0x606060, false);
		int n = samples.size();
		if (n == 0) {
			g.drawCenteredString(font, "Collecting data - check back in a few seconds", gx + GW / 2, gy + GH / 2 - 4, 0xAAAAAA);
		} else {
			for (int i = 0; i < n; i++) {
				long[] s = samples.get(i);
				int px = gx + (n == 1 ? GW / 2 : i * (GW - 1) / (n - 1));
				int fillY = gy + GH - 1 - (int) (s[2] * (GH - 1) / 1000);
				int genY = gy + GH - 1 - (int) (s[0] * (GH - 1) / max);
				int useY = gy + GH - 1 - (int) (s[1] * (GH - 1) / max);
				if (i > 0) {
					long[] p = samples.get(i - 1);
					int ppx = gx + (i - 1) * (GW - 1) / (n - 1);
					line(g, ppx, gy + GH - 1 - (int) (p[2] * (GH - 1) / 1000), px, fillY, 0xFF3F76E4);
					line(g, ppx, gy + GH - 1 - (int) (p[1] * (GH - 1) / max), px, useY, 0xFFE05050);
					line(g, ppx, gy + GH - 1 - (int) (p[0] * (GH - 1) / max), px, genY, 0xFF50D060);
				}
			}
		}
		int ly = y + GY + GH + 8;
		g.fill(x + 34, ly + 2, x + 40, ly + 5, 0xFF50D060); g.drawString(font, "Generated", x + 43, ly, 0x404040, false);
		g.fill(x + 104, ly + 2, x + 110, ly + 5, 0xFFE05050); g.drawString(font, "Used", x + 113, ly, 0x404040, false);
		g.fill(x + 150, ly + 2, x + 156, ly + 5, 0xFF3F76E4); g.drawString(font, "Battery fill", x + 159, ly, 0x404040, false);
		g.drawString(font, longRange ? "One point a minute" : "One point every 5 seconds", x + 34, ly + 12, 0x707070, false);
		super.render(g, mouseX, mouseY, partialTick);
		if (n > 1 && mouseX >= gx && mouseX < gx + GW && mouseY >= gy && mouseY < gy + GH) {
			int i = Math.round((mouseX - gx) * (n - 1) / (float) (GW - 1));
			long[] s = samples.get(Math.max(0, Math.min(n - 1, i)));
			int ago = (n - 1 - i) * (longRange ? 60 : 5);
			String when = ago == 0 ? "now" : ago >= 60 ? (ago / 60) + " min ago" : ago + " s ago";
			g.renderComponentTooltip(font, List.of(Component.literal(when), Component.literal("Generated: " + PowerInfo.shortFe(s[0]) + " FE/t"),
					Component.literal("Used: " + PowerInfo.shortFe(s[1]) + " FE/t"), Component.literal("Batteries: " + (s[2] / 10) + "%")), mouseX, mouseY);
		}
	}

	/** A simple 1-pixel line (steps along the longer axis). */
	private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		for (int k = 0; k <= steps; k++) {
			int x = steps == 0 ? x0 : x0 + (x1 - x0) * k / steps, y = steps == 0 ? y0 : y0 + (y1 - y0) * k / steps;
			g.fill(x, y, x + 1, y + 1, colour);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
