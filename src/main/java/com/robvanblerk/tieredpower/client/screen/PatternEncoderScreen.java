package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.PatternEncoderBlockEntity;
import com.robvanblerk.tieredpower.menu.PatternEncoderMenu;

public class PatternEncoderScreen extends AbstractContainerScreen<PatternEncoderMenu> {
	private Button modeButton, subsButton, chargeButton;

	public PatternEncoderScreen(PatternEncoderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 184;
	}

	private void press(int id) {
		if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	private boolean processing() {
		return menu.getMode() == PatternEncoderBlockEntity.MODE_PROCESSING;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.literal("Encode"), b -> press(PatternEncoderMenu.BUTTON_ENCODE))
				.bounds(leftPos + 134, topPos + 35, 38, 14)
				.tooltip(Tooltip.create(Component.literal("Turn a Blank Pattern into a pattern for this recipe"))).build());
		addRenderableWidget(Button.builder(Component.literal("x"), b -> press(PatternEncoderMenu.BUTTON_CLEAR))
				.bounds(leftPos + 8, topPos + 17, 14, 14).tooltip(Tooltip.create(Component.literal("Clear"))).build());
		modeButton = addRenderableWidget(Button.builder(Component.literal("C"), b -> press(PatternEncoderMenu.BUTTON_MODE))
				.bounds(leftPos + 8, topPos + 35, 14, 14).build());
		subsButton = addRenderableWidget(Button.builder(Component.literal("S"), b -> press(PatternEncoderMenu.BUTTON_SUBSTITUTES))
				.bounds(leftPos + 8, topPos + 53, 14, 14).build());
		chargeButton = addRenderableWidget(Button.builder(Component.literal("\u26A1"), b -> press(PatternEncoderMenu.BUTTON_CHARGE))
				.bounds(leftPos + 8, topPos + 53, 14, 14)
				.tooltip(Tooltip.create(Component.literal("Charge: fill the energy items in the outputs to full. With the same item as the input, that's a charging pattern - give it to an assembler next to a Charger."))).build());
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		boolean p = processing();
		modeButton.setMessage(Component.literal(p ? "P" : "C"));
		modeButton.setTooltip(Tooltip.create(Component.literal(p
				? "Processing mode: a machine recipe (inputs and outputs with amounts). Click to switch to Crafting."
				: "Crafting mode: a crafting-table recipe. Click to switch to Processing (machine recipes).")));
		subsButton.visible = !p;
		chargeButton.visible = p; // processing mode only
		subsButton.setMessage(Component.literal(menu.substitutesOn() ? "S" : "-"));
		subsButton.setTooltip(Tooltip.create(Component.literal(menu.substitutesOn()
				? "Substitutes ON: any item the recipe accepts can be used (any planks...)" : "Substitutes OFF: only the exact items shown")));
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (var s : menu.slots) {
			if (!s.isActive()) continue;
			if (s.index == PatternEncoderMenu.PREVIEW) GuiDraw.bigSlot(g, leftPos + s.x - 5, topPos + s.y - 5);
			else GuiDraw.slot(g, leftPos + s.x - 1, topPos + s.y - 1);
		}
		GuiDraw.arrow(g, leftPos + 84, topPos + 35, 0xFF8B8B8B);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, processing() ? "Processing Pattern" : "Crafting Pattern", 8, 6, 0x404040, false);
		g.drawString(font, "Blank", 142, 6, 0x606060, false);
		g.drawString(font, playerInventoryTitle, 8, PatternEncoderMenu.INV_Y - 11, 0x404040, false);
		if (processing()) g.drawString(font, "Machine", 26, 64, 0x404040, false);
		if (processing()) g.drawString(font, "Fluids", 82, 79, 0x606060, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		if (processing()) { // draw fluids over their slots, with amounts
			for (int i = PatternEncoderMenu.FLUID_IN_START; i < PatternEncoderMenu.PLAYER_START; i++) {
				var slot = menu.slots.get(i);
				var fluid = com.robvanblerk.tieredpower.item.FluidDropItem.fluid(slot.getItem());
				if (fluid.isEmpty()) continue;
				int sx = leftPos + slot.x, sy = topPos + slot.y;
				g.pose().pushPose();
				g.pose().translate(0, 0, 250);
				GuiDraw.fluid(g, fluid, sx, sy);
				String n = GuiDraw.fluidAmount(fluid.getAmount());
				g.pose().scale(0.5f, 0.5f, 1f);
				g.drawString(font, n, (sx + 17) * 2 - font.width(n), (sy + 12) * 2, 0xAADDFF, true);
				g.pose().popPose();
			}
		}
		renderTooltip(g, mouseX, mouseY);
		if (processing() && menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == PatternEncoderMenu.MACHINE) {
			g.renderTooltip(font, font.split(Component.literal("Machine: click this slot with the machine this recipe runs in (e.g. an Electric Furnace); empty hand clears it. Needed for patterns kept in an Assembly Matrix. JEI's + fills it in for this mod's machines."), 200), mouseX, mouseY);
			return;
		}
		if (processing() && menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem()
				&& hoveredSlot.index >= PatternEncoderMenu.FLUID_IN_START && hoveredSlot.index < PatternEncoderMenu.MACHINE) {
			g.renderTooltip(font, Component.literal((hoveredSlot.index < PatternEncoderMenu.FLUID_OUT_START ? "Fluid input" : "Fluid output")
					+ ": click with a bucket or tank of it (right-click adds more), or use JEI's +"), mouseX, mouseY);
			return;
		}
		if (menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem()
				&& hoveredSlot.index >= PatternEncoderMenu.GRID_START && hoveredSlot.index < PatternEncoderMenu.FLUID_IN_START) {
			String tip = processing()
					? "Left-click with a stack to set that amount, right-click to add one (nothing is used up), or use JEI's +"
					: "Click with an item to add it (nothing is used up), or use JEI's +";
			g.renderTooltip(font, Component.literal(tip), mouseX, mouseY);
		}
	}
}
