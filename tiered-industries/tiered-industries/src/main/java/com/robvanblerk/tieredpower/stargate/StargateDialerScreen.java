package com.robvanblerk.tieredpower.stargate;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import com.robvanblerk.tieredpower.TieredPower;

/**
 * The Dialler's screen is a DHD, like on TV: two rings of 19 glyph keys around a big red dome. Press six glyphs and
 * the point of origin (the pyramid), then the dome. The quick-dial list on the right types an address in for you.
 */
public class StargateDialerScreen extends AbstractContainerScreen<StargateDialerMenu> {
	private static final ResourceLocation GLYPHS = ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "textures/gui/dhd_glyphs.png");
	private static final int KEYS = 19;
	private static final float R_BASE = 102, R_RIM = 98, R_OUT_OUT = 95, R_OUT_IN = 64, R_IN_OUT = 60, R_IN_IN = 33, R_DOME = 28;
	private static final int CX = 104, CY = 106, PANEL = 214;
	private static final int ROW_Y = 78, ROW_H = 13, CLEAR_Y = 196;
	/** The bottom strip: redstone dial selector (left) and close button (right). */
	private static final int STRIP_Y = 216, STRIP_H = 14, RS_X = 4, RS_W = 196, CLOSE_X = 214, CLOSE_W = 96;

	private final List<Integer> entered = new ArrayList<>();
	/** Glyphs still to be "typed" by a quick-dial, one every few ticks. */
	private final List<Integer> queue = new ArrayList<>();
	private int queueTimer;
	private String status = "";
	private int statusColour = 0xFFFFFF, statusTicks;
	private boolean domePressed;

	public StargateDialerScreen(StargateDialerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 316;
		imageHeight = 234;
	}

	// ---- layout ----

	/** Keys 0-18 are the outer ring (glyphs 1-19); keys 19-37 the inner ring: the point of origin at the top, then glyphs 20-37. */
	private static int outerGlyph(int k) { return k + 1; }              // glyphs 1-19
	private static int innerGlyph(int k) { return k == 0 ? 0 : 19 + k; } // point of origin, then glyphs 20-37

	private static int keyGlyph(int key) { return key < KEYS ? outerGlyph(key) : innerGlyph(key - KEYS); }

	/** Angle (degrees, screen space, 0 = right, clockwise) of the centre of key k in a ring - key 0 at the top. */
	private static double keyAngle(int k) { return -90 + k * 360.0 / KEYS; }

	/** Which key is under the mouse (0-37), -2 for the dome, -1 for none. */
	private int keyAt(double mx, double my) {
		double dx = mx - (leftPos + CX), dy = my - (topPos + CY);
		double r = Math.sqrt(dx * dx + dy * dy);
		if (r < R_DOME) return -2;
		double a = Math.toDegrees(Math.atan2(dy, dx)) + 90 + 180.0 / KEYS;
		a = ((a % 360) + 360) % 360;
		int k = (int) (a / (360.0 / KEYS)) % KEYS;
		if (r >= R_OUT_IN && r <= R_OUT_OUT) return k;
		if (r >= R_IN_IN && r <= R_IN_OUT) return KEYS + k;
		return -1;
	}

	// ---- drawing ----

	private static void sector(BufferBuilder b, Matrix4f m, float cx, float cy, float r0, float r1, double a0, double a1, int argb) {
		int steps = Math.max(1, (int) Math.ceil((a1 - a0) / 6));
		int al = (argb >>> 24) & 0xFF, rd = (argb >> 16) & 0xFF, gr = (argb >> 8) & 0xFF, bl = argb & 0xFF;
		for (int i = 0; i < steps; i++) {
			double s0 = Math.toRadians(a0 + (a1 - a0) * i / steps), s1 = Math.toRadians(a0 + (a1 - a0) * (i + 1) / steps);
			b.vertex(m, cx + r0 * (float) Math.cos(s0), cy + r0 * (float) Math.sin(s0), 0).color(rd, gr, bl, al).endVertex();
			b.vertex(m, cx + r0 * (float) Math.cos(s1), cy + r0 * (float) Math.sin(s1), 0).color(rd, gr, bl, al).endVertex();
			b.vertex(m, cx + r1 * (float) Math.cos(s1), cy + r1 * (float) Math.sin(s1), 0).color(rd, gr, bl, al).endVertex();
			b.vertex(m, cx + r1 * (float) Math.cos(s0), cy + r1 * (float) Math.sin(s0), 0).color(rd, gr, bl, al).endVertex();
		}
	}

	private static int shade(int c, float f) {
		int r = Math.min(255, (int) (((c >> 16) & 0xFF) * f)), g = Math.min(255, (int) (((c >> 8) & 0xFF) * f)), b = Math.min(255, (int) ((c & 0xFF) * f));
		return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		// right-hand panel
		g.fill(x + PANEL - 4, y, x + imageWidth, y + imageHeight, 0xFF22252B);
		g.fill(x + PANEL - 3, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF30343B);

		int hover = keyAt(mouseX, mouseY);
		boolean open = menu.isOpen() || menu.isIncoming();
		// bottom strip
		g.fill(x, y + STRIP_Y - 4, x + imageWidth, y + imageHeight, 0xFF22252B);
		g.fill(x + 1, y + STRIP_Y - 3, x + imageWidth - 1, y + imageHeight - 1, 0xFF30343B);
		boolean hr = isHovering(RS_X, STRIP_Y, RS_W, STRIP_H, mouseX, mouseY);
		g.fill(x + RS_X, y + STRIP_Y, x + RS_X + RS_W, y + STRIP_Y + STRIP_H, hr ? 0xFF5A3434 : 0xFF472A2A);
		boolean hx = open && isHovering(CLOSE_X, STRIP_Y, CLOSE_W, STRIP_H, mouseX, mouseY);
		g.fill(x + CLOSE_X, y + STRIP_Y, x + CLOSE_X + CLOSE_W, y + STRIP_Y + STRIP_H, !open ? 0xFF2A2D33 : hx ? 0xFF6A4040 : 0xFF553333);
		float cx = x + CX, cy = y + CY;
		g.flush();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		Matrix4f m = g.pose().last().pose();
		BufferBuilder b = Tesselator.getInstance().getBuilder();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		// the console: a dark metal disc with a lighter rim
		sector(b, m, cx, cy, 0, R_BASE, 0, 360, 0xFF2E3138);
		sector(b, m, cx, cy, R_RIM, R_BASE, 0, 360, 0xFF5E646D);
		sector(b, m, cx, cy, R_IN_OUT, R_OUT_IN, 0, 360, 0xFF24262B);
		// the keys
		double half = 180.0 / KEYS, gap = 1.6;
		for (int key = 0; key < 2 * KEYS; key++) {
			boolean outer = key < KEYS;
			int k = outer ? key : key - KEYS;
			double a = keyAngle(k);
			boolean lit = entered.contains(keyGlyph(key)) || open;
			int base = lit ? 0xFFFFA638 : (outer ? 0xFFBDAE8E : 0xFFAE9F80);
			if (key == hover && !lit) base = shade(base, 1.15f);
			float r0 = outer ? R_OUT_IN : R_IN_IN, r1 = outer ? R_OUT_OUT : R_IN_OUT;
			sector(b, m, cx, cy, r0, r1, a - half + gap / 2, a + half - gap / 2, shade(base, 0.72f));
			sector(b, m, cx, cy, r0 + 1.5f, r1 - 1.5f, a - half + gap / 2 + 0.8, a + half - gap / 2 - 0.8, base);
		}
		// the dome, shaded to look round; it glows when the gate is open (or the button is down)
		boolean glow = open || domePressed;
		int dome = glow ? 0xFFFF5530 : (hover == -2 ? 0xFFC22A20 : 0xFFA8221A);
		sector(b, m, cx, cy, R_DOME, R_DOME + 4, 0, 360, 0xFF4A4E56);
		for (int i = 0; i < 7; i++) {
			float r1 = R_DOME * (1 - i / 7f), r0 = R_DOME * (1 - (i + 1) / 7f);
			sector(b, m, cx, cy, r0, r1, 0, 360, shade(dome, 0.7f + i * 0.09f));
		}
		sector(b, m, cx - 7, cy - 8, 0, 5, 0, 360, glow ? 0xC0FFE0C0 : 0x60FFC8C0); // highlight
		BufferUploader.drawWithShader(b.end());
		RenderSystem.enableCull();

		// glyphs on the keys
		for (int key = 0; key < 2 * KEYS; key++) {
			boolean outer = key < KEYS;
			int k = outer ? key : key - KEYS, glyph = keyGlyph(key);
			double a = Math.toRadians(keyAngle(k));
			float rm = outer ? (R_OUT_IN + R_OUT_OUT) / 2 : (R_IN_IN + R_IN_OUT) / 2;
			int size = outer ? 14 : 12;
			int gx = Math.round(cx + rm * (float) Math.cos(a)) - size / 2, gy = Math.round(cy + rm * (float) Math.sin(a)) - size / 2;
			boolean lit = entered.contains(glyph) || open;
			if (lit) g.setColor(0.45f, 0.15f, 0.0f, 1f);
			else g.setColor(0.25f, 0.2f, 0.14f, 1f);
			drawGlyph(g, glyph, gx, gy, size);
		}
		g.setColor(1f, 1f, 1f, 1f);

		// address being entered: seven chevron boxes
		for (int i = 0; i <= GateAddress.LENGTH; i++) {
			int bx = x + PANEL + i * 13, by = y + 18;
			boolean on = i < entered.size();
			g.fill(bx, by, bx + 12, by + 12, on ? 0xFF7A4A10 : 0xFF1B1D22);
			g.fill(bx + 1, by + 1, bx + 11, by + 11, on ? 0xFFFFA638 : 0xFF2A2D33);
			if (on) {
				g.setColor(0.35f, 0.12f, 0f, 1f);
				drawGlyph(g, entered.get(i), bx + 1, by + 1, 10);
				g.setColor(1f, 1f, 1f, 1f);
			}
		}

		// quick dial
		List<Integer> rows = rows();
		for (int i = 0; i < rows.size(); i++) {
			int choice = rows.get(i);
			int ry = y + ROW_Y + i * ROW_H;
			boolean hov = mouseX >= x + PANEL && mouseX < x + imageWidth - 6 && mouseY >= ry && mouseY < ry + ROW_H - 1;
			g.fill(x + PANEL, ry, x + imageWidth - 6, ry + ROW_H - 1, hov ? 0xFF474C55 : 0xFF3A3E46);
			int colour = choice < 0 ? 0xFF4A8A4A : (0xFF000000 | Planet.values()[choice].colour);
			g.fill(x + PANEL + 2, ry + 2, x + PANEL + 10, ry + ROW_H - 3, colour);
		}
		// clear button
		boolean hc = isHovering(imageWidth - 46, CLEAR_Y, 40, 12, mouseX, mouseY);
		g.fill(x + imageWidth - 46, y + CLEAR_Y, x + imageWidth - 6, y + CLEAR_Y + 12, hc ? 0xFF6A4040 : 0xFF553333);
	}

	private void drawGlyph(GuiGraphics g, int glyph, int gx, int gy, int size) {
		RenderSystem.enableBlend();
		g.blit(GLYPHS, gx, gy, size, size, (glyph % 8) * 16, (glyph / 8) * 16, 16, 16, 128, 80);
	}

	private static final int VISIBLE_ROWS = 9;
	private int scroll;

	/** The address book: home (on a planet only), then every world whose address you know. */
	private List<Integer> allRows() {
		List<Integer> out = new ArrayList<>();
		if (menu.onPlanet()) out.add(-1);
		for (Planet p : Planet.values()) if (menu.knows(p)) out.add(p.ordinal());
		return out;
	}

	/** The rows currently scrolled into view. */
	private List<Integer> rows() {
		List<Integer> all = allRows();
		scroll = Math.max(0, Math.min(scroll, all.size() - VISIBLE_ROWS));
		return all.subList(scroll, Math.min(all.size(), scroll + VISIBLE_ROWS));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (mouseX >= leftPos + PANEL && allRows().size() > VISIBLE_ROWS) {
			scroll -= (int) Math.signum(delta);
			rows();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(font, "Dial Home Device", PANEL, 6, 0xFFD8B070, false);
		if (statusTicks > 0 || !status.isEmpty()) g.drawString(font, status, PANEL, 34, statusColour, false);
		else if (!menu.hasRing()) g.drawString(font, "No Stargate nearby", PANEL, 34, 0xFF6060, false);
		else if (menu.isIncoming()) g.drawString(font, "Incoming - gate open", PANEL, 34, 0x7FD0FF, false);
		else if (menu.getOpenTicks() < 0) g.drawString(font, "Gate open (redstone)", PANEL, 34, 0x7FD0FF, false);
		else if (menu.getOpenTicks() > 0) g.drawString(font, "Gate open: " + menu.getOpenTicks() / 20 + " s", PANEL, 34, 0x7FD0FF, false);
		else g.drawString(font, entered.isEmpty() ? "Press 6 glyphs + origin" : "Chevron " + entered.size() + " encoded", PANEL, 34, 0xC0C0C0, false);
		if (menu.onPlanet()) g.drawString(font, "Free from here", PANEL, 48, 0x9FD89F, false);
		else {
			g.drawString(font, "50 billion FE", PANEL, 46, 0xC0C0C0, false);
			g.fill(PANEL, 56, imageWidth - 6, 61, 0xFF1B1D22);
			g.fill(PANEL + 1, 57, PANEL + 1 + (int) ((imageWidth - 8 - PANEL) * menu.getEnergyFraction()), 60, 0xFFD63A2E);
		}
		int total = allRows().size();
		g.drawString(font, "Addresses", PANEL, 66, 0xFFD8B070, false);
		if (total > VISIBLE_ROWS) {
			String more = (scroll > 0 ? "\u25B2" : " ") + (scroll + VISIBLE_ROWS < total ? "\u25BC" : " ");
			g.drawString(font, more, imageWidth - 18, 66, 0xFFD8B070, false);
		}
		List<Integer> rows = rows();
		for (int i = 0; i < rows.size(); i++) {
			int choice = rows.get(i);
			String name = choice < 0 ? "Home" : Planet.values()[choice].title;
			g.drawString(font, name, PANEL + 13, ROW_Y + i * ROW_H + 2, 0xFFFFFF, false);
		}
		g.drawCenteredString(font, "Clear", imageWidth - 26, CLEAR_Y + 2, 0xFFFFFF);
		int rt = menu.getRedstoneTarget();
		String rs = rt == StargateDialerBlockEntity.REDSTONE_OFF ? "Off" : rt < 0 ? "Home" : Planet.values()[Math.min(rt, Planet.values().length - 1)].title;
		g.drawString(font, "Redstone signal dials: " + rs, RS_X + 4, STRIP_Y + 3, 0xFFFFFF, false);
		g.drawCenteredString(font, "Close gate", CLOSE_X + CLOSE_W / 2, STRIP_Y + 3, menu.isOpen() || menu.isIncoming() ? 0xFFFFFF : 0x707070);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		int x = leftPos, y = topPos;
		List<Integer> rows = rows();
		for (int i = 0; i < rows.size(); i++) {
			int ry = y + ROW_Y + i * ROW_H;
			if (mouseX >= x + PANEL && mouseX < x + imageWidth - 6 && mouseY >= ry && mouseY < ry + ROW_H - 1) {
				int choice = rows.get(i);
				List<Component> tip = new ArrayList<>();
				if (choice < 0) {
					tip.add(Component.literal("Home"));
					tip.add(Component.literal("Back to where you came through from").withStyle(ChatFormatting.GRAY));
				} else {
					Planet p = Planet.values()[choice];
					tip.add(Component.literal(p.title));
					tip.add(Component.literal(p.description).withStyle(ChatFormatting.GRAY));
				}
				if (choice >= 0) {
					StringBuilder sb = new StringBuilder("Glyphs: ");
					for (int gl : GateAddress.of(Planet.values()[choice])) sb.append(gl).append(' ');
					tip.add(Component.literal(sb.toString().trim()).withStyle(ChatFormatting.GOLD));
				}
				tip.add(Component.literal("Click to enter its address").withStyle(ChatFormatting.DARK_AQUA));
				g.renderComponentTooltip(font, tip, mouseX, mouseY);
			}
		}
		if (keyAt(mouseX, mouseY) == -2)
			g.renderTooltip(font, Component.literal(menu.isOpen() || menu.isIncoming() ? "Close the gate" : "Dial"), mouseX, mouseY);
		if (isHovering(RS_X, STRIP_Y, RS_W, STRIP_H, mouseX, mouseY))
			g.renderComponentTooltip(font, List.of(Component.literal("Click to change"),
					Component.literal("A redstone signal into the DHD dials this address and holds").withStyle(ChatFormatting.GRAY),
					Component.literal("the gate open until the signal goes off.").withStyle(ChatFormatting.GRAY),
					Component.literal("From home: 50 billion FE to dial, 1 million FE/t while open.").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
	}

	// ---- input ----

	private void click(float pitch) {
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, pitch));
	}

	private void press(int glyph) {
		if (entered.size() > GateAddress.LENGTH || entered.contains(glyph)) return;
		entered.add(glyph);
		status = "";
		click(entered.size() == GateAddress.LENGTH + 1 ? 0.7f : 1.0f + entered.size() * 0.05f);
	}

	private void fail(String why) {
		status = why;
		statusColour = 0xFF6060;
		statusTicks = 60;
		entered.clear();
		queue.clear();
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.6f));
	}

	private void dome() {
		if (menu.isOpen() || menu.isIncoming()) { closeGate(); return; }
		if (!menu.hasRing()) { fail("No Stargate nearby"); return; }
		Integer choice = GateAddress.resolve(entered.stream().mapToInt(Integer::intValue).toArray());
		if (choice == null) { fail("Dial program failed"); return; }
		if (choice < 0 && !menu.onPlanet()) { fail("You're already home"); return; }
		domePressed = true;
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_POWER_SELECT, 0.8f));
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, choice < 0 ? StargateDialerMenu.BUTTON_HOME : StargateDialerMenu.BUTTON_PLANET + choice);
		}
	}

	private void closeGate() {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StargateDialerMenu.BUTTON_CLOSE);
			click(0.6f);
		}
		entered.clear();
		domePressed = false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			if (isHovering(RS_X, STRIP_Y, RS_W, STRIP_H, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StargateDialerMenu.BUTTON_REDSTONE);
				click(1.0f);
				return true;
			}
			if ((menu.isOpen() || menu.isIncoming()) && isHovering(CLOSE_X, STRIP_Y, CLOSE_W, STRIP_H, mouseX, mouseY)) { closeGate(); return true; }
			int key = keyAt(mouseX, mouseY);
			if (key == -2) { dome(); return true; }
			if (key >= 0) { if (queue.isEmpty()) press(keyGlyph(key)); return true; }
			List<Integer> rows = rows();
			for (int i = 0; i < rows.size(); i++) {
				int ry = topPos + ROW_Y + i * ROW_H;
				if (mouseX >= leftPos + PANEL && mouseX < leftPos + imageWidth - 6 && mouseY >= ry && mouseY < ry + ROW_H - 1) {
					entered.clear();
					queue.clear();
					status = "";
					for (int gl : rows.get(i) < 0 ? GateAddress.HOME : GateAddress.of(Planet.values()[rows.get(i)])) queue.add(gl);
					queue.add(GateAddress.POINT_OF_ORIGIN);
					queueTimer = 0;
					return true;
				}
			}
			if (isHovering(imageWidth - 46, CLEAR_Y, 40, 12, mouseX, mouseY)) {
				entered.clear();
				queue.clear();
				status = "";
				click(0.8f);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (statusTicks > 0 && --statusTicks == 0) status = "";
		if (!queue.isEmpty() && ++queueTimer % 4 == 0) press(queue.remove(0));
	}
}
