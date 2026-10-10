package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.menu.CraftingTerminalMenu;
import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.StorageActionPacket;

/** The Storage Terminal's grid (4 rows) plus a 3x3 crafting grid underneath. */
public class CraftingTerminalScreen extends StorageTerminalScreen<CraftingTerminalMenu> {
	public CraftingTerminalScreen(CraftingTerminalMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 4, 260);
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(Button.builder(Component.literal("x"),
				b -> ModNetwork.CHANNEL.sendToServer(new StorageActionPacket(menu.containerId, StorageTerminalMenu.ACTION_CLEAR_GRID, ItemStack.EMPTY)))
				.bounds(leftPos + 8, topPos + CraftingTerminalMenu.CRAFT_Y, 12, 12)
				.tooltip(Tooltip.create(Component.literal("Put the crafting grid back into storage")))
				.build());
		craftMissing = addRenderableWidget(Button.builder(Component.literal("+"),
				b -> ModNetwork.CHANNEL.sendToServer(new StorageActionPacket(menu.containerId, StorageTerminalMenu.ACTION_CRAFT_MISSING, ItemStack.EMPTY)))
				.bounds(leftPos + 8, topPos + CraftingTerminalMenu.CRAFT_Y + 40, 12, 14)
				.tooltip(Tooltip.create(Component.literal("Craft All Missing: start crafting jobs for the ingredients the grid is missing (after JEI's +). They're put in the grid as they're made.")))
				.build());
	}

	private Button craftMissing;

	@Override
	protected void containerTick() {
		super.containerTick();
		if (craftMissing != null) craftMissing.visible = menu.getMissingCount() > 0;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		if (menu.getMissingCount() > 0) {
			String t = menu.getMissingCount() + " missing";
			g.pose().pushPose();
			g.pose().translate(0, 0, 300);
			g.pose().scale(0.5f, 0.5f, 1f);
			g.drawString(font, t, (leftPos + 6) * 2, (topPos + CraftingTerminalMenu.CRAFT_Y + 56) * 2, 0xFF6050, true);
			g.pose().popPose();
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		super.renderBg(g, partialTick, mouseX, mouseY);
		GuiDraw.arrow(g, leftPos + 90, topPos + CraftingTerminalMenu.RESULT_Y, 0xFF8B8B8B);
		GuiDraw.bigSlot(g, leftPos + CraftingTerminalMenu.RESULT_X - 5, topPos + CraftingTerminalMenu.RESULT_Y - 5);
	}
}
