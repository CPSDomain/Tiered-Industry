package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.ProcessingMachineBlockEntity;
import com.robvanblerk.tieredpower.menu.ProcessingMachineMenu;

/** Screen shared by the Ore Purifier, Compressor and Electric Sawmill. */
public class ProcessingMachineScreen extends MachineScreen<ProcessingMachineMenu> {
	public static final int ARROW_X = 52, ARROW_Y = 35;
	private static final int TANK_X = 136, TANK_Y = 18;

	public ProcessingMachineScreen(ProcessingMachineMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		drawArrow(g, x + ARROW_X, y + ARROW_Y, menu.getProgressFraction());
		if (menu.usesWater()) {
			drawTank(g, x + TANK_X, y + TANK_Y, (float) menu.getWater() / ProcessingMachineBlockEntity.TANK_CAPACITY, 0xFF3F76E4);
		}
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
		text(g, statusText(), x + 8, y + 62);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (menu.usesWater() && isHovering(TANK_X, TANK_Y, 14, 52, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB", menu.getWater(), ProcessingMachineBlockEntity.TANK_CAPACITY)), mouseX, mouseY);
		}
	}
}
