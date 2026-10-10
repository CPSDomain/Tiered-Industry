package com.robvanblerk.tieredpower.logic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.logic.MachineStatusDisplayBlockEntity.Status;

/** A list of nearby machines, problems first, with a summary line and a filter. */
public class MachineStatusScreen extends AbstractContainerScreen<MachineStatusMenu> {
	private static final int ROWS = 13, ROW_H = 12, LIST_Y = 40;
	private int scroll;
	/** -1 all, otherwise only one Status. */
	private int filter = -1;

	public MachineStatusScreen(MachineStatusMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 280;
		imageHeight = 210;
	}

	private record Row(Status status, String name, int x, int y, int z, String tier) {}

	private List<Row> all() {
		List<Row> out = new ArrayList<>();
		for (String l : menu.lines()) {
			String[] p = l.split("\\|", -1);
			if (p.length < 6) continue;
			try {
				Status s = Status.values()[Integer.parseInt(p[0])];
				out.add(new Row(s, p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]), p[5]));
			} catch (RuntimeException ignored) {}
		}
		return out;
	}

	private List<Row> shown() {
		List<Row> rows = all();
		if (filter >= 0) rows.removeIf(r -> r.status().ordinal() != filter);
		scroll = Math.max(0, Math.min(scroll, rows.size() - ROWS));
		return rows;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E2126);
		g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF2B2F36);
		g.fill(x + 6, y + LIST_Y - 2, x + imageWidth - 6, y + LIST_Y + ROWS * ROW_H + 2, 0xFF1A1C20);
		// filter chips
		int cx = x + 6;
		for (int i = -1; i < Status.values().length; i++) {
			String label = chipLabel(i);
			int w = font.width(label) + 8;
			boolean on = filter == i;
			g.fill(cx, y + 22, cx + w, y + 34, on ? 0xFF5A6070 : 0xFF3A3E46);
			cx += w + 3;
		}
	}

	private String chipLabel(int i) {
		List<Row> rows = all();
		if (i < 0) return "All " + rows.size();
		Status s = Status.values()[i];
		long n = rows.stream().filter(r -> r.status() == s).count();
		return s.label.split(" ")[0] + " " + n;
	}

	private int chipAt(double mx, double my) {
		if (my < topPos + 22 || my >= topPos + 34) return -2;
		int cx = leftPos + 6;
		for (int i = -1; i < Status.values().length; i++) {
			int w = font.width(chipLabel(i)) + 8;
			if (mx >= cx && mx < cx + w) return i;
			cx += w + 3;
		}
		return -2;
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		List<Row> rows = all();
		long bad = rows.stream().filter(r -> r.status() == Status.NO_POWER || r.status() == Status.BLOCKED).count();
		g.drawString(font, "Machine Status", 8, 8, 0xFFD8B070, false);
		String sum = rows.isEmpty() ? "No machines within " + MachineStatusDisplayBlockEntity.RANGE + " blocks"
				: rows.size() + " machines, " + (bad == 0 ? "all fine" : bad + " need attention");
		g.drawString(font, sum, imageWidth - 8 - font.width(sum), 8, bad == 0 ? 0x80E080 : 0xFF8060, false);
		int cx = 6;
		for (int i = -1; i < Status.values().length; i++) {
			String label = chipLabel(i);
			int w = font.width(label) + 8;
			g.drawString(font, label, cx + 4, 24, i < 0 ? 0xFFFFFF : Status.values()[i].colour, false);
			cx += w + 3;
		}
		List<Row> shown = shown();
		for (int i = 0; i < ROWS && scroll + i < shown.size(); i++) {
			Row r = shown.get(scroll + i);
			int ry = LIST_Y + i * ROW_H + 2;
			g.fill(10, ry + 1, 16, ry + 7, 0xFF000000 | r.status().colour);
			String name = r.tier().isEmpty() ? r.name() : r.name() + " (" + r.tier() + ")";
			if (font.width(name) > 130) name = font.plainSubstrByWidth(name, 126) + "..";
			g.drawString(font, name, 20, ry, 0xFFFFFF, false);
			g.drawString(font, r.status().label, 156, ry, r.status().colour, false);
			String pos = r.x() + " " + r.y() + " " + r.z();
			g.drawString(font, pos, imageWidth - 10 - font.width(pos), ry, 0x909090, false);
		}
		if (shown.size() > ROWS) {
			String more = (scroll + 1) + "-" + Math.min(shown.size(), scroll + ROWS) + " of " + shown.size() + " (scroll)";
			g.drawString(font, more, imageWidth - 8 - font.width(more), imageHeight - 12, 0x909090, false);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		int chip = chipAt(mx, my);
		if (chip != -2) {
			filter = filter == chip ? -1 : chip;
			scroll = 0;
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		scroll -= (int) Math.signum(delta) * 3;
		shown();
		return true;
	}
}
