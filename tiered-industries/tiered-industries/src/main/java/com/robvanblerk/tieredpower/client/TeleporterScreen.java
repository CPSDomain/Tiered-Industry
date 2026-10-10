package com.robvanblerk.tieredpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import com.robvanblerk.tieredpower.network.ModNetwork;

/** Name this pad and pick which pad it sends you to. Crouch on the pad to travel. */
public class TeleporterScreen extends Screen {
	private static final int W = 240, H = 200, ROW = 16;
	private final ModNetwork.OpenTeleporterPacket data;
	private EditBox name;
	private int scroll;

	public TeleporterScreen(ModNetwork.OpenTeleporterPacket data) {
		super(Component.literal("Teleporter"));
		this.data = data;
	}

	public static void open(ModNetwork.OpenTeleporterPacket data) {
		Minecraft.getInstance().setScreen(new TeleporterScreen(data));
	}

	private int left() { return (width - W) / 2; }
	private int top() { return (height - H) / 2; }

	private void send(int action, String text, ModNetwork.PadInfo pad) {
		ModNetwork.CHANNEL.sendToServer(new ModNetwork.TeleporterActionPacket(data.pos(), action, text,
				pad == null ? "" : pad.dim(), pad == null ? BlockPos.ZERO : pad.pos()));
	}

	@Override
	protected void init() {
		int x = left(), y = top();
		name = new EditBox(font, x + 10, y + 26, 150, 16, Component.literal("Name"));
		name.setMaxLength(32);
		name.setValue(data.name());
		addRenderableWidget(name);
		addRenderableWidget(Button.builder(Component.literal("Rename"), b -> send(0, name.getValue(), null)).bounds(x + 166, y + 25, 64, 18).build());
		addRenderableWidget(Button.builder(Component.literal("Unlink"), b -> send(2, "", null)).bounds(x + 166, y + H - 26, 64, 18).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int x = left(), y = top();
		g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF000000);
		g.fill(x, y, x + W, y + H, 0xFF2A2140);
		g.drawString(font, "Teleporter Pad", x + 10, y + 8, 0xFFD8B8FF, false);
		g.drawString(font, String.format("%,d FE stored", data.energy()), x + W - 10 - font.width(String.format("%,d FE stored", data.energy())), y + 8, 0xFFB0B0C0, false);
		g.drawString(font, "Travel to:", x + 10, y + 50, 0xFFFFFFFF, false);

		int listY = y + 62, listH = H - 96, rows = listH / ROW;
		if (data.pads().isEmpty()) {
			g.drawString(font, "No other pads yet - place another one.", x + 10, listY + 4, 0xFFB0B0C0, false);
		}
		for (int i = 0; i < rows && i + scroll < data.pads().size(); i++) {
			var pad = data.pads().get(i + scroll);
			int ry = listY + i * ROW;
			boolean selected = i + scroll == data.selected();
			boolean hover = mouseX >= x + 8 && mouseX < x + W - 8 && mouseY >= ry && mouseY < ry + ROW - 1;
			g.fill(x + 8, ry, x + W - 8, ry + ROW - 1, selected ? 0xFF6A4F8A : hover ? 0xFF453868 : 0xFF342A52);
			g.drawString(font, font.plainSubstrByWidth(pad.name(), 130), x + 12, ry + 4, 0xFFFFFFFF, false);
			String where = dimName(pad.dim()) + " " + pad.pos().toShortString();
			g.drawString(font, font.plainSubstrByWidth(where, 90), x + W - 12 - Math.min(90, font.width(where)), ry + 4, 0xFFB0B0C0, false);
		}
		g.drawString(font, data.selected() >= 0 ? "Crouch on the pad to travel." : "Pick a destination.", x + 10, y + H - 21, 0xFFD8B8FF, false);
		super.render(g, mouseX, mouseY, partialTick);
	}

	private static String dimName(String dim) {
		return switch (dim) {
			case "minecraft:overworld" -> "Overworld";
			case "minecraft:the_nether" -> "Nether";
			case "minecraft:the_end" -> "End";
			default -> dim.substring(dim.indexOf(':') + 1);
		};
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		int x = left(), listY = top() + 62, rows = (H - 96) / ROW;
		for (int i = 0; i < rows && i + scroll < data.pads().size(); i++) {
			int ry = listY + i * ROW;
			if (mouseX >= x + 8 && mouseX < x + W - 8 && mouseY >= ry && mouseY < ry + ROW - 1) {
				send(1, "", data.pads().get(i + scroll));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int rows = (H - 96) / ROW;
		scroll = Math.max(0, Math.min(Math.max(0, data.pads().size() - rows), scroll - (int) Math.signum(delta)));
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
