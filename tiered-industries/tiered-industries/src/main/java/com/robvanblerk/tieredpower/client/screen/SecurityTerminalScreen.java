package com.robvanblerk.tieredpower.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.menu.SecurityTerminalMenu;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.SecurityActionPacket;
import com.robvanblerk.tieredpower.network.SecurityListPacket;

/** Who may use this storage network: the owner, plus the players added here. */
public class SecurityTerminalScreen extends AbstractContainerScreen<SecurityTerminalMenu> {
	private static final int ROWS = 6, ROW_H = 16;
	private EditBox name;
	private final List<Button> removeButtons = new ArrayList<>();
	private int scroll;

	public SecurityTerminalScreen(SecurityTerminalMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 200;
		imageHeight = 170;
	}

	@Override
	protected void init() {
		super.init();
		name = new EditBox(font, leftPos + 8, topPos + 30, 130, 14, Component.literal("Player name"));
		name.setMaxLength(16);
		name.setHint(Component.literal("Player name"));
		addRenderableWidget(name);
		addRenderableWidget(Button.builder(Component.literal("Add"), b -> add()).bounds(leftPos + 142, topPos + 29, 50, 16).build());
		removeButtons.clear();
		for (int r = 0; r < ROWS; r++) {
			int row = r;
			removeButtons.add(addRenderableWidget(Button.builder(Component.literal("Remove"), b -> {
				var list = menu.getClientMembers();
				if (scroll + row < list.size()) ModNetwork.CHANNEL.sendToServer(new SecurityActionPacket(menu.containerId, 2, "", list.get(scroll + row).id()));
			}).bounds(leftPos + 142, topPos + 58 + r * ROW_H, 50, 14).build()));
		}
		ModNetwork.CHANNEL.sendToServer(new SecurityActionPacket(menu.containerId, 0, "", null));
	}

	private void add() {
		if (name.getValue().isBlank()) return;
		ModNetwork.CHANNEL.sendToServer(new SecurityActionPacket(menu.containerId, 1, name.getValue().trim(), null));
		name.setValue("");
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		name.tick();
		int n = menu.getClientMembers().size();
		for (int r = 0; r < ROWS; r++) removeButtons.get(r).visible = scroll + r < n;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		scroll = (int) Math.max(0, Math.min(Math.max(0, menu.getClientMembers().size() - ROWS), scroll - Math.signum(delta)));
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (name.isFocused()) {
			if (key == 257 || key == 335) { add(); return true; } // enter
			if (key != 256) { name.keyPressed(key, scan, modifiers); return true; }
		}
		return super.keyPressed(key, scan, modifiers);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		GuiDraw.panel(g, leftPos, topPos, imageWidth, imageHeight);
		g.fill(leftPos + 7, topPos + 55, leftPos + 139, topPos + 57 + ROWS * ROW_H, 0xFF8B8B8B);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, title, 8, 6, 0x404040, false);
		g.drawString(font, "Owner: " + menu.getClientOwner(), 8, 18, 0x404040, false);
		g.drawString(font, "Trusted players:", 8, 47, 0x404040, false);
		List<SecurityListPacket.Member> list = menu.getClientMembers();
		if (list.isEmpty()) g.drawString(font, "Only you", 12, 61, 0xFFFFFF, false);
		for (int r = 0; r < ROWS && scroll + r < list.size(); r++) g.drawString(font, list.get(scroll + r).name(), 12, 61 + r * ROW_H, 0xFFFFFF, false);
		g.drawString(font, "Operators can always get in.", 8, imageHeight - 12, 0x707070, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
	}
}
