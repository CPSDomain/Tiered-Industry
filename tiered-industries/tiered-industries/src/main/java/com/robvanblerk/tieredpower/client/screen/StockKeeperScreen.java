package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.block.entity.StockKeeperBlockEntity;
import com.robvanblerk.tieredpower.energy.PowerInfo;
import com.robvanblerk.tieredpower.menu.StockKeeperMenu;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.StockKeeperPacket;

/**
 * Pick up to 9 items (click a slot with the item) and how many of each to keep. Click a slot to select it, type the
 * amount and press Set. Each slot shows its state underneath.
 */
public class StockKeeperScreen extends AbstractContainerScreen<StockKeeperMenu> {
	private static final int[] STATUS_COLOURS = {0xFF8B8B8B, 0xFF4FA84F, 0xFF3F6FD8, 0xFFD63A2E, 0xFF6A6A6A};
	private static final String[] STATUS_NAMES = {"Not set", "In stock", "Crafting", "Can't craft (missing ingredients or no pattern)", "No powered network"};
	private int selected;
	private EditBox amount;

	public StockKeeperScreen(StockKeeperMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 176;
		imageHeight = 166;
	}

	@Override
	protected void init() {
		super.init();
		amount = new EditBox(font, leftPos + 72, topPos + 50, 52, 12, Component.literal("Amount"));
		amount.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
		amount.setMaxLength(7);
		addRenderableWidget(amount);
		addRenderableWidget(Button.builder(Component.literal("Set"), b -> apply()).bounds(leftPos + 128, topPos + 49, 26, 14).build());
		addRenderableWidget(Button.builder(Component.literal("x"), b -> ModNetwork.CHANNEL.sendToServer(new StockKeeperPacket(menu.containerId, selected, -1)))
				.bounds(leftPos + 156, topPos + 49, 14, 14)
				.tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Stop keeping the selected item"))).build());
		amount.setValue(String.valueOf(menu.getAmount(selected)));
	}

	private void apply() {
		int n;
		try {
			n = amount.getValue().isEmpty() ? 0 : Integer.parseInt(amount.getValue());
		} catch (NumberFormatException e) {
			n = StockKeeperBlockEntity.MAX_AMOUNT;
		}
		ModNetwork.CHANNEL.sendToServer(new StockKeeperPacket(menu.containerId, selected, n));
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		for (int i = 0; i < 9; i++) {
			int sx = leftPos + 8 + i * 18, sy = topPos + StockKeeperMenu.SLOT_Y;
			if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16 && menu.getCarried().isEmpty() && !menu.slots.get(i).getItem().isEmpty()) {
				selected = i;
				amount.setValue(String.valueOf(menu.getAmount(i)));
				return true; // select, don't clear
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (amount.isFocused()) {
			if (key == 257 || key == 335) apply(); // enter
			else if (key != 256) {
				amount.keyPressed(key, scan, modifiers);
				return true;
			}
		}
		return super.keyPressed(key, scan, modifiers);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		for (var s : menu.slots) GuiDraw.slot(g, leftPos + s.x - 1, topPos + s.y - 1);
		for (int i = 0; i < 9; i++) {
			int sx = leftPos + 8 + i * 18, sy = topPos + StockKeeperMenu.SLOT_Y + 18;
			int st = menu.slots.get(i).getItem().isEmpty() ? 0 : menu.getStatus(i);
			g.fill(sx, sy, sx + 16, sy + 3, STATUS_COLOURS[Math.min(st, STATUS_COLOURS.length - 1)]);
			if (i == selected) {
				int x = sx - 1, y = topPos + StockKeeperMenu.SLOT_Y - 1;
				g.fill(x, y - 1, x + 18, y, 0xFFFFD83A);
				g.fill(x, y + 18, x + 18, y + 19, 0xFFFFD83A);
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, 8, 6, 0x404040, false);
		g.drawString(font, "Keep in stock:", 8, 52, 0x404040, false);
		g.drawString(font, playerInventoryTitle, 8, 73, 0x404040, false);
		for (int i = 0; i < 9; i++) {
			if (menu.slots.get(i).getItem().isEmpty()) continue;
			String n = PowerInfo.shortFe(menu.getAmount(i)).replace(",", "");
			g.pose().pushPose();
			g.pose().translate(0, 0, 300);
			g.pose().scale(0.5f, 0.5f, 1f);
			g.drawString(font, n, (8 + i * 18 + 17) * 2 - font.width(n), (StockKeeperMenu.SLOT_Y + 12) * 2, 0xFFFFFF, true);
			g.pose().popPose();
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		for (int i = 0; i < 9; i++) {
			int sx = leftPos + 8 + i * 18, sy = topPos + StockKeeperMenu.SLOT_Y;
			if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 21) {
				var item = menu.slots.get(i).getItem();
				if (item.isEmpty()) {
					g.renderTooltip(font, Component.literal(menu.getCarried().isEmpty() ? "Click with an item to keep it in stock" : "Click to keep this item in stock"), mouseX, mouseY);
				} else if (menu.getCarried().isEmpty()) {
					g.renderComponentTooltip(font, java.util.List.of(item.getHoverName(),
							Component.literal("Keep " + String.format("%,d", menu.getAmount(i)) + ": " + STATUS_NAMES[Math.min(menu.getStatus(i), STATUS_NAMES.length - 1)]),
							Component.literal("Click to select it, then set the amount below (x removes it)").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
				}
				return;
			}
		}
		renderTooltip(g, mouseX, mouseY);
	}
}
