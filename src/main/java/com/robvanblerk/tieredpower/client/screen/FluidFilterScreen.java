package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.item.FluidDropItem;
import com.robvanblerk.tieredpower.menu.FluidFilterMenu;

public class FluidFilterScreen extends AbstractContainerScreen<FluidFilterMenu> {
	private Button toggle;

	public FluidFilterScreen(FluidFilterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		toggle = addRenderableWidget(Button.builder(Component.literal("Allow"), b -> {
			if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FluidFilterMenu.BUTTON_TOGGLE);
		}).bounds(leftPos + 120, topPos + 34, 48, 16).build());
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		toggle.setMessage(Component.literal(menu.isWhitelist() ? "Allow" : "Block"));
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (var s : menu.slots) GuiDraw.slot(g, leftPos + s.x - 1, topPos + s.y - 1);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		for (int i = 0; i < 9; i++) {
			var slot = menu.slots.get(i);
			var fluid = FluidDropItem.fluid(slot.getItem());
			if (fluid.isEmpty()) continue;
			g.pose().pushPose();
			g.pose().translate(0, 0, 250);
			GuiDraw.fluid(g, fluid, leftPos + slot.x, topPos + slot.y);
			g.pose().popPose();
		}
		renderTooltip(g, mouseX, mouseY);
		if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.index < 9 && !hoveredSlot.hasItem())
			g.renderTooltip(font, Component.literal("Click with a bucket or tank of a fluid or gas"), mouseX, mouseY);
	}
}
