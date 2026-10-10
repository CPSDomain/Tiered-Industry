package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.MatrixControllerBlockEntity;
import com.robvanblerk.tieredpower.menu.MatrixControllerMenu;

public class MatrixControllerScreen extends AbstractContainerScreen<MatrixControllerMenu> {
	public MatrixControllerScreen(MatrixControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 180;
		inventoryLabelY = MatrixControllerMenu.INV_Y - 11;
	}

	private void press(int id) {
		if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.literal("<"), b -> press(MatrixControllerMenu.BUTTON_PREV)).bounds(leftPos + 112, topPos + 4, 14, 11).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> press(MatrixControllerMenu.BUTTON_NEXT)).bounds(leftPos + 156, topPos + 4, 14, 11).build());
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (var s : menu.slots) GuiDraw.slot(g, leftPos + s.x - 1, topPos + s.y - 1);
	}

	private String status() {
		if (menu.isFormed() && menu.getPages() == 0) return "Formed - put Pattern Banks inside";
		if (menu.isFormed()) return menu.getPatternCount() + " patterns, " + menu.getPages() + " banks, " + menu.getCraftsPerCycle() + "/cycle";
		return switch (menu.getProblem()) {
			case MatrixControllerBlockEntity.PROBLEM_SIZE -> "Not formed: needs a hollow box 3x3x3 to 7x7x7";
			case MatrixControllerBlockEntity.PROBLEM_WALL -> "Not formed: a wall block isn't a Matrix part";
			case MatrixControllerBlockEntity.PROBLEM_INSIDE -> "Not formed: inside only Banks, Accelerators, air";
			case MatrixControllerBlockEntity.PROBLEM_CONTROLLERS -> "Not formed: exactly one Controller";
			default -> "Checking structure...";
		};
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, 8, 6, 0x404040, false);
		if (menu.getPages() > 0) {
			String p = (menu.getPage() + 1) + "/" + menu.getPages();
			g.drawCenteredString(font, p, 141, 6, 0xFFFFFF);
		}
		String s = status();
		if (font.width(s) > 160) s = font.plainSubstrByWidth(s, 156) + "...";
		g.drawString(font, s, 8, 76, menu.isFormed() && menu.getPages() > 0 ? 0x206020 : 0xAA0000, false);
		g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
	}

	/** Where the Connected machines panel is (also kept clear by JEI). */
	public net.minecraft.client.renderer.Rect2i machinesPanel() {
		int lines = Math.max(1, Math.min(menu.getMachines().size(), 14));
		return new net.minecraft.client.renderer.Rect2i(leftPos + imageWidth + 4, topPos, 150, 18 + lines * 10);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		if (menu.isFormed() && mouseX >= leftPos + 8 && mouseX < leftPos + 168 && mouseY >= topPos + 75 && mouseY < topPos + 85)
			g.renderTooltip(font, java.util.List.of(
					net.minecraft.network.chat.Component.literal(menu.getAccelerators() + " Crafting Accelerator" + (menu.getAccelerators() == 1 ? "" : "s")),
					net.minecraft.network.chat.Component.literal(menu.getCraftsPerCycle() + " crafts per cycle (max " + com.robvanblerk.tieredpower.storage.CraftingTier.MATRIX_MAX_CRAFTS + ")")
							.withStyle(net.minecraft.ChatFormatting.GRAY),
					net.minecraft.network.chat.Component.literal(menu.getOverclock() > 0 ? "Overclock: machines run as if they had " + menu.getOverclock() + " more Speed upgrades" : "No Overclock Accelerators")
							.withStyle(menu.getOverclock() > 0 ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.DARK_GRAY),
					net.minecraft.network.chat.Component.literal("Crafting Upgrades raise each Accelerator: +2, +4, +8, +16")
							.withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), java.util.Optional.empty(), mouseX, mouseY);
		// Machines the Matrix can use, in a panel to the right.
		var list = menu.getMachines();
		var area = machinesPanel();
		int px = area.getX(), py = area.getY(), pw = area.getWidth(), ph = area.getHeight();
		GuiDraw.panel(g, px, py, pw, ph);
		g.drawString(font, "Connected machines", px + 6, py + 6, 0x404040, false);
		for (int i = 0; i < list.size() && i < 14; i++) {
			String line = list.get(i);
			if (font.width(line) > pw - 12) line = font.plainSubstrByWidth(line, pw - 18) + "...";
			g.drawString(font, line, px + 6, py + 18 + i * 10, 0x2050B0, false);
		}
		if (list.isEmpty()) g.drawString(font, "...", px + 6, py + 18, 0x707070, false);
		renderTooltip(g, mouseX, mouseY);
		// Processing patterns without a machine: say how to set one.
		if (hoveredSlot != null && hoveredSlot.index < MatrixControllerMenu.SLOTS && menu.getCarried().isEmpty()) {
			var p = com.robvanblerk.tieredpower.storage.CraftingPattern.fromStack(hoveredSlot.getItem());
			if (p != null && p.processing() && !p.hasMachine())
				g.renderTooltip(font, Component.literal("No machine set - click this pattern while holding the machine's item").withStyle(net.minecraft.ChatFormatting.RED), mouseX, mouseY + 14);
		}
	}
}
