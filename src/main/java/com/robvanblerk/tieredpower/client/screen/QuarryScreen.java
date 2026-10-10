package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.QuarryBlockEntity;
import com.robvanblerk.tieredpower.menu.QuarryMenu;

/** Quarry GUI with a settings panel underneath the buffer. */
public class QuarryScreen extends MachineScreen<QuarryMenu> {
	/** A clickable text button: x, y, width, button id, tooltip. */
	private record Btn(int x, int y, int w, int id, String tip) {}

	private static final int ROW1 = 80, ROW2 = 94, ROW3 = 112;
	private static final Btn[] BUTTONS = {
			new Btn(8, ROW1, 12, 2, "Smaller area"), new Btn(64, ROW1, 12, 3, "Bigger area"),
			new Btn(80, ROW1, 58, 4, "Mine centred on the quarry, or behind it. With a Quarry Planner area set, click to go back to centred"),
			new Btn(142, ROW1, 26, 10, "Restart from the top"),
			new Btn(8, ROW2, 12, 5, "Dig 1 deeper (shift: 10)"), new Btn(64, ROW2, 12, 6, "Stop 1 higher (shift: 10)"),
			new Btn(80, ROW2, 88, 9, "Tool: Silk Touch and Fortune use 50% more power"),
	};

	public QuarryScreen(QuarryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected int extraHeight() {
		return QuarryMenu.EXTRA_HEIGHT;
	}

	private String label(Btn b) {
		return switch (b.id()) {
			case 2, 5 -> "-";
			case 3, 6 -> "+";
			case 4 -> menu.isCustom() ? "Planner area" : menu.isBehind() ? "Behind" : "Centred";
			case 9 -> QuarryBlockEntity.TOOL_MODES[menu.getToolMode()];
			case 10 -> "Reset";
			default -> "";
		};
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		String state = switch (menu.getState()) {
			case 2 -> "Finished";
			case 1 -> "Output full";
			default -> "Mining";
		};
		text(g, state, x + 30, y + 20);
		text(g, "Y: " + menu.getY(), x + 30, y + 32);
		text(g, String.format("%,d", menu.getMined()), x + 30, y + 44);
		text(g, menu.getEnergyCost() + " FE/t", x + 30, y + 60);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);

		// Settings panel
		int size = menu.getRadius() * 2 + 1;
		String area = menu.isCustom() ? menu.getCustomWidth() + "x" + menu.getCustomLength() : size + "x" + size;
		g.drawCenteredString(font, area, x + 42, y + ROW1 + 2, menu.isCustom() ? 0xFFFF80 : 0xFFFFFF);
		g.drawCenteredString(font, "Y " + menu.getMinY(), x + 42, y + ROW2 + 2, 0xFFFFFF);
		text(g, "Void:", x + 30, y + ROW3 + 5);
		for (int i = 0; i < 6; i++) g.blit(WIDGETS, x + QuarryMenu.VOID_X + i * 18 - 1, y + QuarryMenu.VOID_Y - 1, 0, 0, 18, 18);
		for (Btn b : BUTTONS) {
			int bx = x + b.x(), by = y + b.y();
			g.fill(bx, by, bx + b.w(), by + 12, 0xFF373737);
			g.fill(bx + 1, by + 1, bx + b.w(), by + 12, 0xFFFFFFFF);
			g.fill(bx + 1, by + 1, bx + b.w() - 1, by + 11, 0xFF8B8B8B);
			g.drawCenteredString(font, label(b), bx + b.w() / 2, by + 2, 0xFFFFFF);
		}
		// dark backing for the size and depth readouts
		g.fill(x + 21, y + ROW1, x + 63, y + ROW1 + 12, 0x40000000);
		g.fill(x + 21, y + ROW2, x + 63, y + ROW2 + 12, 0x40000000);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (Btn b : BUTTONS) {
			if (isHovering(b.x(), b.y(), b.w(), 12, mouseX, mouseY)) {
				int id = b.id();
				if (hasShiftDown() && (id == 5 || id == 6)) id += 2; // shift = 10 blocks
				pressButton(id);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		for (Btn b : BUTTONS) {
			if (isHovering(b.x(), b.y(), b.w(), 12, mouseX, mouseY)) g.renderTooltip(font, Component.literal(b.tip()), mouseX, mouseY);
		}
		if (isHovering(QuarryMenu.VOID_X - 1, QuarryMenu.VOID_Y - 1, 108, 18, mouseX, mouseY) && menu.getCarried().isEmpty()) {
			g.renderTooltip(font, Component.literal("Void filter: blocks shown here are thrown away (click with an item to set)"), mouseX, mouseY);
		}
	}
}
