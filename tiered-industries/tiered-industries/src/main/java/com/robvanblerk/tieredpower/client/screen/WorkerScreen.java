package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.WorkerBlockEntity;
import com.robvanblerk.tieredpower.menu.WorkerMenu;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Shared screen for the automation machines: inputs on the left, results on the right, a status line. */
public class WorkerScreen extends MachineScreen<WorkerMenu> {
	public WorkerScreen(WorkerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	private String job() {
		var t = menu.getType();
		if (t == ModMenus.BLOCK_BREAKER.get()) return "Breaking blocks";
		if (t == ModMenus.BLOCK_PLACER.get()) return "Placing blocks";
		if (t == ModMenus.TREE_FARM.get()) return "Tending trees";
		if (t == ModMenus.LASER_DRILL.get()) return "Mining from orbit";
		if (t == ModMenus.DIGITAL_MINER.get()) return "Mining ores";
		return "Tending animals";
	}

	private String inputHint() {
		var t = menu.getType();
		if (t == ModMenus.BLOCK_BREAKER.get()) return "No inputs needed";
		if (t == ModMenus.BLOCK_PLACER.get()) return "Blocks to place";
		if (t == ModMenus.TREE_FARM.get()) return "Saplings (and bone meal)";
		if (t == ModMenus.LASER_DRILL.get()) return "Laser Lenses (ore x10 as likely) or Item Filters (block or allow ores)";
		if (t == ModMenus.DIGITAL_MINER.get()) return "Item Filters (which ores) and an Enchanted Book of Silk Touch or Fortune";
		return "Feed (wheat, seeds, carrots...) and empty buckets";
	}

	private String specialText() {
		var t = menu.getType();
		if (t == ModMenus.BLOCK_PLACER.get()) return "Needs blocks to place";
		if (t == ModMenus.TREE_FARM.get()) return "Needs saplings";
		if (t == ModMenus.DIGITAL_MINER.get()) return "Finished - no wanted ores left in range";
		if (t == ModMenus.LASER_DRILL.get()) return menu.getSpecial() == com.robvanblerk.tieredpower.block.entity.LaserDrillBlockEntity.SPECIAL_NO_SKY
				? "Needs a clear view of the sky" : "Needs a Mining Satellite in orbit";
		return "Waiting";
	}

	@Override
	protected void drawContents(GuiGraphics g, int x, int y) {
		String s = switch (menu.getStatus()) {
			case WorkerBlockEntity.STATUS_WORKING -> job() + " (" + menu.getActions() + ")";
			case WorkerBlockEntity.STATUS_NO_POWER -> "Needs power";
			case WorkerBlockEntity.STATUS_FULL -> "Output full - empty it";
			case WorkerBlockEntity.STATUS_SPECIAL -> specialText();
			default -> "Nothing to do";
		};
		text(g, s, x + 8, y + 72 - 10);
		drawEnergyBar(g, x + 156, y + 18, 14, 52);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		for (int i = 0; i < WorkerBlockEntity.INPUTS; i++)
			if (menu.getCarried().isEmpty() && !menu.slots.get(i).hasItem()
					&& isHovering(WorkerMenu.INPUT_X - 1, WorkerMenu.TOP_Y - 1 + i * 18, 18, 18, mouseX, mouseY))
				g.renderTooltip(font, Component.literal(inputHint()), mouseX, mouseY);
	}
}
