package com.robvanblerk.tieredpower.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.robvanblerk.tieredpower.network.JobsListPacket;
import com.robvanblerk.tieredpower.network.JobsRequestPacket;
import com.robvanblerk.tieredpower.network.ModNetwork;

/** Every crafting job on the network, with progress and a Cancel button each. Refreshes every second. Esc goes back. */
public class JobsScreen extends Screen {
	private static final int W = 240, H = 196, ROWS = 7, ROW_H = 20;
	private final Screen parent;
	private final int containerId;
	private List<JobsListPacket.Job> jobs = List.of();
	private int cpus, scroll, ticks;
	private final List<Button> cancelButtons = new ArrayList<>();
	/** The job whose steps are shown in the side panel (-1 = none). */
	private int selected = -1;

	public JobsScreen(Screen parent, int containerId) {
		super(Component.literal("Crafting jobs"));
		this.parent = parent;
		this.containerId = containerId;
	}

	private int left() { return (width - W) / 2; }
	private int top() { return (height - H) / 2; }

	@Override
	protected void init() {
		cancelButtons.clear();
		for (int r = 0; r < ROWS; r++) {
			int row = r;
			Button b = Button.builder(Component.literal("Cancel"), btn -> ModNetwork.CHANNEL.sendToServer(new JobsRequestPacket(containerId, scroll + row)))
					.bounds(left() + W - 52, top() + 24 + r * ROW_H, 44, 16).build();
			cancelButtons.add(addRenderableWidget(b));
		}
		addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(left() + 8, top() + H - 22, 60, 16).build());
		ModNetwork.CHANNEL.sendToServer(new JobsRequestPacket(containerId, -1));
	}

	public void receive(JobsListPacket packet) {
		jobs = packet.jobs();
		cpus = packet.cpus();
		scroll = Math.max(0, Math.min(scroll, jobs.size() - ROWS));
	}

	@Override
	public void tick() {
		if (++ticks % 20 == 0) ModNetwork.CHANNEL.sendToServer(new JobsRequestPacket(containerId, -1));
		for (int r = 0; r < ROWS; r++) cancelButtons.get(r).visible = scroll + r < jobs.size();
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		int x = left(), y = top();
		for (int r = 0; r < ROWS && scroll + r < jobs.size(); r++) {
			int ry = y + 24 + r * ROW_H;
			if (mx >= x + 8 && mx < x + W - 56 && my >= ry && my < ry + 18) {
				selected = selected == scroll + r ? -1 : scroll + r; // click again to close
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		scroll = (int) Math.max(0, Math.min(Math.max(0, jobs.size() - ROWS), scroll - Math.signum(delta)));
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int x = left(), y = top();
		GuiDraw.panel(g, x, y, W, H);
		g.drawString(font, "Crafting jobs  (" + cpus + " at once)", x + 8, y + 8, 0x404040, false);
		if (jobs.isEmpty()) g.drawString(font, "Nothing is being crafted", x + 8, y + 30, 0x707070, false);
		for (int r = 0; r < ROWS && scroll + r < jobs.size(); r++) {
			JobsListPacket.Job j = jobs.get(scroll + r);
			int ry = y + 24 + r * ROW_H;
			g.renderItem(j.item(), x + 8, ry);
			String name = j.amount() + " x " + j.item().getHoverName().getString();
			if (font.width(name) > 150) name = font.plainSubstrByWidth(name, 144) + "...";
			g.drawString(font, name, x + 28, ry, 0x404040, false);
			String status = j.status();
			if (font.width(status) > 150) status = font.plainSubstrByWidth(status, 144) + "...";
			g.drawString(font, status, x + 28, ry + 9, status.startsWith("Waiting") ? 0xAA0000 : status.startsWith("Queued") ? 0x806000 : 0x2050B0, false);
		}
		if (jobs.size() > ROWS) g.drawString(font, "scroll for more", x + W / 2 - 30, y + H - 18, 0x808080, false);
		super.render(g, mouseX, mouseY, partialTick);
		// Steps of the selected job, in a panel to the right.
		if (selected >= 0 && selected < jobs.size()) {
			var job = jobs.get(selected);
			int px = x + W + 4, pw = 230;
			int lines = Math.max(1, Math.min(job.steps().size(), 16));
			GuiDraw.panel(g, px, y, pw, 22 + lines * 10);
			String head = "Steps: " + job.amount() + " x " + job.item().getHoverName().getString();
			if (font.width(head) > pw - 12) head = font.plainSubstrByWidth(head, pw - 18) + "...";
			g.drawString(font, head, px + 6, y + 6, 0x404040, false);
			if (job.steps().isEmpty()) g.drawString(font, "Finishing up", px + 6, y + 18, 0x707070, false);
			for (int i2 = 0; i2 < job.steps().size() && i2 < 16; i2++) {
				String line = job.steps().get(i2);
				if (font.width(line) > pw - 12) line = font.plainSubstrByWidth(line, pw - 18) + "...";
				g.drawString(font, line, px + 6, y + 18 + i2 * 10, line.contains("running") ? 0x2050B0 : 0x404040, false);
			}
		}
		for (int r = 0; r < ROWS && scroll + r < jobs.size(); r++) { // full status on hover
			int ry = y + 24 + r * ROW_H;
			if (mouseX >= x + 8 && mouseX < x + W - 56 && mouseY >= ry && mouseY < ry + 18) {
				JobsListPacket.Job j = jobs.get(scroll + r);
				g.renderComponentTooltip(font, List.of(Component.literal(j.amount() + " x " + j.item().getHoverName().getString()),
						Component.literal(j.status()), Component.literal(j.left() + " crafts/operations left"),
						Component.literal("Click to show its steps").withStyle(net.minecraft.ChatFormatting.GRAY)), mouseX, mouseY);
			}
		}
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
