package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity;
import com.robvanblerk.tieredpower.menu.FissionControllerMenu;

public class FissionControllerScreen extends MachineScreen<FissionControllerMenu> {
	private static final int HEAT_X = 30, BAR_Y = 18, WATER_X = 136;

	public FissionControllerScreen(FissionControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		float heat = (float) menu.getHeat() / FissionControllerBlockEntity.MAX_HEAT;
		int colour = heat < 0.5f ? 0xFF4FB84F : heat < 0.85f ? 0xFFE8A030 : 0xFFE03A2E;
		drawTank(g, x + HEAT_X, y + BAR_Y, heat, colour);
		g.fill(x + HEAT_X - 2, y + BAR_Y + 26, x + HEAT_X + 16, y + BAR_Y + 27, 0xFFFFFFFF);

		if (!menu.isFormed()) {
			text(g, "Not formed", x + 50, y + 20);
			text(g, "Right-click the", x + 50, y + 32);
			text(g, "controller for info", x + 50, y + 42);
		} else {
			text(g, menu.isScrammed() ? "SCRAM!" : menu.getGenerating() > 0 ? "Running" : "Idle", x + 50, y + 18);
			text(g, String.format("%,d FE/t", menu.getGenerating()), x + 50, y + 29);
			text(g, "Heat " + menu.getHeat() / 10 + "%", x + 50, y + 40);
			text(g, "Eff " + menu.getEfficiency() + "%", x + 50, y + 51);
			text(g, "Rods: " + menu.getRods(), x + 50, y + 62);
		}
		drawTank(g, x + WATER_X, y + BAR_Y, (float) menu.getWater() / FissionControllerBlockEntity.WATER_CAPACITY, 0xFF3F76E4);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (isHovering(HEAT_X, BAR_Y, 14, 52, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Heat: full power from 50%; Coolant Channels + water hold it there. SCRAM at 100%."), mouseX, mouseY);
		} else if (isHovering(WATER_X, BAR_Y, 14, 52, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(String.format("Water: %,d / %,d mB", menu.getWater(), FissionControllerBlockEntity.WATER_CAPACITY)), mouseX, mouseY);
		} else if (menu.isFormed() && isHovering(50, 16, 84, 56, mouseX, mouseY)) {
			int cooling = FissionControllerBlockEntity.BASE_COOLING + FissionControllerBlockEntity.COOLING_PER_CHANNEL * menu.getChannels()
					+ FissionControllerBlockEntity.COOLING_PER_WATER_BLOCK * menu.getWaterBlocks();
			g.renderTooltip(font, Component.literal(menu.getAssemblies() + " fuel, " + menu.getChannels() + " channels, " + menu.getWaterBlocks()
					+ " water blocks - cooling up to " + cooling + " heat/t"), mouseX, mouseY);
		} else if (isHovering(7, 25, 18, 18, mouseX, mouseY) && menu.getCarried().isEmpty() && !menu.slots.get(0).hasItem()) {
			g.renderTooltip(font, Component.literal("Fuel rods in (stored inside, up to 64)"), mouseX, mouseY);
		}
	}
}
