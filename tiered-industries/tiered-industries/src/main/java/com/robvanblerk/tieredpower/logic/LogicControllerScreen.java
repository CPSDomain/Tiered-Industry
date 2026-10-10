package com.robvanblerk.tieredpower.logic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.client.screen.GuiDraw;
import com.robvanblerk.tieredpower.network.LogicValuePacket;
import com.robvanblerk.tieredpower.network.ModNetwork;

/** Four rule rows: [item] [mode] [< / >] [number] [side] (light). */
public class LogicControllerScreen extends AbstractContainerScreen<LogicControllerMenu> {
	private static final String[] MODES = {"Off", "Item", "Power", "Redstone"};
	private static final String[] SIDES = {"Down", "Up", "North", "South", "West", "East", "All"};
	private final Button[] mode = new Button[4], cmp = new Button[4], side = new Button[4];
	private final EditBox[] value = new EditBox[4];
	private final int[] shownValue = {-1, -1, -1, -1};

	public LogicControllerScreen(LogicControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 206;
		inventoryLabelY = LogicControllerMenu.INV_Y - 11;
	}

	private void press(int id) {
		if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void init() {
		super.init();
		for (int r = 0; r < 4; r++) {
			final int rule = r;
			int y = topPos + LogicControllerMenu.ROW_Y - 1 + r * LogicControllerMenu.ROW_H;
			mode[r] = addRenderableWidget(Button.builder(Component.literal("Off"), b -> press(rule * 10)).bounds(leftPos + 28, y, 46, 18).build());
			cmp[r] = addRenderableWidget(Button.builder(Component.literal("<"), b -> press(rule * 10 + 1)).bounds(leftPos + 76, y, 16, 18).build());
			value[r] = new EditBox(font, leftPos + 94, y + 2, 38, 14, Component.literal("Amount"));
			value[r].setFilter(s -> s.isEmpty() || s.length() <= 9 && s.chars().allMatch(Character::isDigit));
			value[r].setResponder(s -> {
				int v = s.isEmpty() ? 0 : Integer.parseInt(s);
				if (v != menu.threshold(rule)) ModNetwork.CHANNEL.sendToServer(new LogicValuePacket(menu.containerId, rule, v));
			});
			addRenderableWidget(value[r]);
			side[r] = addRenderableWidget(Button.builder(Component.literal("All"), b -> press(rule * 10 + 2)).bounds(leftPos + 134, y, 32, 18).build());
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		for (int r = 0; r < 4; r++) {
			int m = menu.mode(r);
			mode[r].setMessage(Component.literal(MODES[Math.floorMod(m, 4)]));
			boolean usesNumber = m == LogicControllerBlockEntity.MODE_ITEM || m == LogicControllerBlockEntity.MODE_POWER;
			cmp[r].visible = usesNumber;
			value[r].visible = usesNumber;
			cmp[r].setMessage(Component.literal(menu.below(r) ? "<" : ">"));
			side[r].visible = m != LogicControllerBlockEntity.MODE_OFF;
			side[r].setMessage(Component.literal(SIDES[Math.floorMod(menu.side(r), 7)]));
			int t = menu.threshold(r);
			if (!value[r].isFocused() && t != shownValue[r]) { shownValue[r] = t; value[r].setValue(String.valueOf(t)); }
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (var s : menu.slots) GuiDraw.slot(g, leftPos + s.x - 1, topPos + s.y - 1);
		for (int r = 0; r < 4; r++) {
			int st = menu.status(r), x = leftPos + 168, y = topPos + LogicControllerMenu.ROW_Y + 4 + r * LogicControllerMenu.ROW_H;
			int col = menu.mode(r) == LogicControllerBlockEntity.MODE_OFF ? 0xFF555555 : st == LogicControllerBlockEntity.STATUS_TRUE ? 0xFFFF3030 : st == LogicControllerBlockEntity.STATUS_FALSE ? 0xFF501010 : 0xFF806020;
			g.fill(x, y, x + 5, y + 8, col);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, 8, 6, 0x404040, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		for (EditBox b : value) if (b.isFocused() && b.visible) { if (key == 256) onClose(); else b.keyPressed(key, scan, mods); return true; } // typing doesn't close the screen
		return super.keyPressed(key, scan, mods);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
		for (int r = 0; r < 4; r++) {
			int y = LogicControllerMenu.ROW_Y - 1 + r * LogicControllerMenu.ROW_H;
			int m = menu.mode(r);
			if (isHovering(8, y, 18, 18, mouseX, mouseY) && !menu.slots.get(r).hasItem() && menu.getCarried().isEmpty())
				g.renderTooltip(font, Component.literal(m == LogicControllerBlockEntity.MODE_ITEM ? "Click with the item to count in storage" : "Item (for Item mode)"), mouseX, mouseY);
			if (isHovering(166, y, 9, 18, mouseX, mouseY)) {
				String now = switch (m) {
					case LogicControllerBlockEntity.MODE_ITEM -> menu.status(r) == LogicControllerBlockEntity.STATUS_NOT_READY ? "No item set, or no powered storage network" : String.format("In storage: %,d", menu.current(r));
					case LogicControllerBlockEntity.MODE_POWER -> menu.status(r) == LogicControllerBlockEntity.STATUS_NOT_READY ? "No battery touching the controller" : "Batteries: " + menu.current(r) + "%";
					case LogicControllerBlockEntity.MODE_REDSTONE -> menu.current(r) > 0 ? "Getting a redstone signal" : "No redstone signal";
					default -> "Rule switched off";
				};
				g.renderComponentTooltip(font, java.util.List.of(Component.literal(menu.status(r) == LogicControllerBlockEntity.STATUS_TRUE ? "TRUE - signal on" : "false - no signal"), Component.literal(now)), mouseX, mouseY);
			}
			if (isHovering(28, y, 46, 18, mouseX, mouseY))
				g.renderTooltip(font, font.split(Component.literal("Item: count of the item in storage. Power: % full of batteries touching the controller. Redstone: a signal into the controller. Click to change."), 180), mouseX, mouseY);
			if (isHovering(134, y, 32, 18, mouseX, mouseY) && m != LogicControllerBlockEntity.MODE_OFF)
				g.renderTooltip(font, Component.literal("Which side gives the redstone signal when the rule is true"), mouseX, mouseY);
		}
	}
}
