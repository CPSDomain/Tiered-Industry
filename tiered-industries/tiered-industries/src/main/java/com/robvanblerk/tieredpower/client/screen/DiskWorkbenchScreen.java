package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.DiskWorkbenchBlockEntity;
import com.robvanblerk.tieredpower.item.FluidDropItem;
import com.robvanblerk.tieredpower.menu.DiskWorkbenchMenu;

public class DiskWorkbenchScreen extends AbstractContainerScreen<DiskWorkbenchMenu> {
	public DiskWorkbenchScreen(DiskWorkbenchMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 184;
	}

	private void press(int id) {
		if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.literal("-"), b -> press(DiskWorkbenchBlockEntity.BUTTON_PRIORITY_DOWN))
				.bounds(leftPos + 44, topPos + 74, 12, 12).tooltip(Tooltip.create(Component.literal("Lower priority"))).build());
		addRenderableWidget(Button.builder(Component.literal("+"), b -> press(DiskWorkbenchBlockEntity.BUTTON_PRIORITY_UP))
				.bounds(leftPos + 104, topPos + 74, 12, 12).tooltip(Tooltip.create(Component.literal("Higher priority: filled first"))).build());
		addRenderableWidget(Button.builder(Component.literal("Fill"), b -> press(DiskWorkbenchBlockEntity.BUTTON_FILL))
				.bounds(leftPos + 120, topPos + 74, 24, 12).tooltip(Tooltip.create(Component.literal("Partition to exactly what's stored on the disk"))).build());
		addRenderableWidget(Button.builder(Component.literal("Clear"), b -> press(DiskWorkbenchBlockEntity.BUTTON_CLEAR))
				.bounds(leftPos + 146, topPos + 74, 26, 12).tooltip(Tooltip.create(Component.literal("Remove the partition: accept anything"))).build());
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (var s : menu.slots) GuiDraw.slot(g, leftPos + s.x - 1, topPos + s.y - 1);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, 8, 6, 0x404040, false);
		g.drawString(font, "Disk", 12, 54, 0x606060, false);
		int p = menu.getPriority();
		String prio = menu.getDiskKind() == 0 ? "" : "Priority " + (p > 0 ? "+" : "") + p;
		g.drawCenteredString(font, prio, 80, 76, 0xFFFFFF);
		g.drawString(font, playerInventoryTitle, 8, DiskWorkbenchMenu.INV_Y - 11, 0x404040, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		for (int i = DiskWorkbenchMenu.PART_START; i < DiskWorkbenchMenu.PART_END; i++) { // fluids drawn as fluids
			var slot = menu.slots.get(i);
			var fluid = FluidDropItem.fluid(slot.getItem());
			if (fluid.isEmpty()) continue;
			g.pose().pushPose();
			g.pose().translate(0, 0, 250);
			GuiDraw.fluid(g, fluid, leftPos + slot.x, topPos + slot.y);
			g.pose().popPose();
		}
		renderTooltip(g, mouseX, mouseY);
		if (menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem()) {
			if (hoveredSlot.index == DiskWorkbenchMenu.DISK)
				g.renderTooltip(font, Component.literal("A Storage or Fluid Disk"), mouseX, mouseY);
			else if (hoveredSlot.index >= DiskWorkbenchMenu.PART_START && hoveredSlot.index < DiskWorkbenchMenu.PART_END)
				g.renderTooltip(font, Component.literal(menu.getDiskKind() == 0 ? "Put a disk in first"
						: menu.getDiskKind() == 2 ? "Click with a bucket or tank: only that fluid (empty = anything)"
						: "Click with an item: the disk only takes these (empty = anything)"), mouseX, mouseY);
		}
	}
}
