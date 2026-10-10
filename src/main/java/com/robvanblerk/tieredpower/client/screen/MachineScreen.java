package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import com.robvanblerk.tieredpower.energy.RedstoneMode;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.menu.MachineMenu;

/**
 * Shared GUI drawing. The background panel and player inventory come from textures/gui/machine_base.png;
 * slots, arrows, flames, energy bars and tank frames come from textures/gui/widgets.png.
 * Edit those PNGs to restyle every machine at once.
 */
public abstract class MachineScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {
	public static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "textures/gui/machine_base.png");
	public static final ResourceLocation BASE_TALL = ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "textures/gui/machine_base_tall.png");
	public static final ResourceLocation WIDGETS = ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "textures/gui/widgets.png");

	protected static final int DARK = 0xFF555555;
	protected static final int LIGHT = 0xFFFFFFFF;
	protected static final int TEXT = 0xFF404040;
	protected static final int ENERGY = 0xFFD63A2E;
	protected static final int ENERGY_EMPTY = 0xFF3A1A18;
	protected static final int TANK_EMPTY = 0xFF2A2A2A;

	protected MachineScreen(T menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
		if (sidesOpen && menu.getFacing() != null) {
			drawSidesPanel(g, mouseX, mouseY);
			int hovered = hoveredFace(mouseX, mouseY);
			if (hovered >= 0) g.renderComponentTooltip(font, sideTooltip(hovered), mouseX, mouseY);
		}
		if (menu.hasAutoOutput() && isHovering(SIDES_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Sides: which faces take items in and which let them out (pipes, buses, hoppers, auto-output)"), mouseX, mouseY);
		}
		if (hasUpgradesButton() && isHovering(UPG_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Upgrades: Speed and Energy cards, Muffler, and machine tier"), mouseX, mouseY);
		}
		if (menu.hasAutoOutput() && isHovering(OUT_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Auto-output: " + (menu.isAutoOutput() ? "ON - pushes finished items into neighbouring chests/pipes" : "OFF") + " (click)"), mouseX, mouseY);
		}
		if (menu.hasRedstoneControl() && isHovering(RS_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal("Redstone: " + menu.getRedstoneMode().getDescription() + " (click to change)"), mouseX, mouseY);
		}
		// Hovering the energy bar shows exact numbers
		if (hasEnergyBar() && isHovering(156, 18, 14, 52, mouseX, mouseY)) {
			g.renderTooltip(font, Component.literal(energyText()), mouseX, mouseY);
		}
	}

	/** Machines without an energy bar at the standard spot return false. */
	protected boolean hasEnergyBar() {
		return menu.getCapacity() > 0;
	}

	/** Taller GUIs (e.g. the Quarry's settings panel) return how many extra pixels they need; max 56. */
	protected int extraHeight() {
		return 0;
	}

	@Override
	protected void init() {
		imageHeight = 166 + extraHeight();
		inventoryLabelY = imageHeight - 94;
		super.init();
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		g.blit(extraHeight() > 0 ? BASE_TALL : BASE, x, y, 0, 0, imageWidth, imageHeight);
		if (upgradesOpen) drawUpgradesPanel(g);

		for (int i = 0; i < menu.getMachineSlotCount(); i++) {
			Slot slot = menu.slots.get(i);
			if (!slot.isActive()) continue;
			if (menu.isBigSlot(i)) {
				g.blit(WIDGETS, x + slot.x - 5, y + slot.y - 5, 18, 0, 26, 26);
			} else {
				g.blit(WIDGETS, x + slot.x - 1, y + slot.y - 1, 0, 0, 18, 18);
			}
		}

		// Empty upgrade slots show a faded picture of the upgrade they take.
		for (int i = 0; i < menu.getMachineSlotCount(); i++) {
			Slot slot = menu.slots.get(i);
			ItemStack ghost = menu.getGhostItem(i);
			if (!ghost.isEmpty() && !slot.hasItem() && slot.isActive()) {
				g.renderFakeItem(ghost, x + slot.x, y + slot.y);
				g.pose().pushPose();
				g.pose().translate(0, 0, 300);
				g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xB08B8B8B);
				g.pose().popPose();
			}
		}

		drawContents(g, x, y);

		if (menu.hasRedstoneControl()) drawRedstoneButton(g, x + RS_X, y + RS_Y, isHovering(RS_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY));
		if (menu.hasAutoOutput()) drawOutputButton(g, x + OUT_X, y + RS_Y, isHovering(OUT_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY));
		if (menu.hasAutoOutput()) drawSidesButton(g, x + SIDES_X, y + RS_Y, isHovering(SIDES_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY));
		if (hasUpgradesButton()) drawUpgradesButton(g, x + UPG_X, y + RS_Y, isHovering(UPG_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY));
		if (upgradesOpen) drawUpgradesText(g, mouseX, mouseY);
	}

	// ---- Redstone control button (top-right corner) ----
	protected static final int RS_X = 158, RS_Y = 3, RS_SIZE = 14;

	private static ItemStack redstoneIcon(RedstoneMode mode) {
		return new ItemStack(switch (mode) {
			case ALWAYS -> Items.GUNPOWDER;   // grey dust = redstone ignored
			case HIGH -> Items.REDSTONE;       // dust = needs a signal
			case LOW -> Items.REDSTONE_TORCH;  // torch = inverted
		});
	}

	private void drawRedstoneButton(GuiGraphics g, int bx, int by, boolean hovered) {
		g.fill(bx, by, bx + RS_SIZE, by + RS_SIZE, hovered ? 0xFFFFFFFF : 0xFF373737);
		g.fill(bx + 1, by + 1, bx + RS_SIZE, by + RS_SIZE, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + RS_SIZE - 1, by + RS_SIZE - 1, hovered ? 0xFFA0A0C8 : 0xFF8B8B8B);
		g.pose().pushPose();
		g.pose().translate(bx + 1, by + 1, 0);
		g.pose().scale(0.75f, 0.75f, 1f);
		g.renderFakeItem(redstoneIcon(menu.getRedstoneMode()), 0, 0);
		g.pose().popPose();
	}

	// ---- Sides button and panel ----
	protected static final int SIDES_X = 126;
	/**
	 * The sides window sits to the right of the machine screen. Faces are shown as you see the machine standing in
	 * front of it: Top above, Left / Front / Right / Back across, and the power face (bottom) below.
	 */
	private static final int CELL_W = 32, CELL_H = 18, GAP = 2, PANEL_W = 4 * CELL_W + 3 * GAP + 12, PANEL_H = 3 * CELL_H + 2 * GAP + 48;
	private static final int[] MODE_COLOURS = {0xFF6A6A6A, 0xFF3F6FD8, 0xFFE0822E, 0xFF4FA84F};
	private static final int POWER_COLOUR = 0xFFD63A2E;
	/** Column, row for each machine face (0 front, 1 back, 2 left, 3 right, 4 top, 5 bottom). */
	private static final int[][] FACE_CELLS = {{1, 1}, {3, 1}, {0, 1}, {2, 1}, {1, 0}, {1, 2}};
	private static final String[] FACE_LABELS = {"Front", "Back", "Left", "Right", "Top", "Pwr"};
	private boolean sidesOpen;

	/** Screen area the sides window covers (for JEI, so its item list stays clear of it). */
	public java.util.List<net.minecraft.client.renderer.Rect2i> extraAreas() {
		if (upgradesOpen) return java.util.List.of(new net.minecraft.client.renderer.Rect2i(panelX(), panelY(), UPG_W, UPG_H));
		if (!sidesOpen || menu.getFacing() == null) return java.util.List.of();
		return java.util.List.of(new net.minecraft.client.renderer.Rect2i(panelX(), panelY(), PANEL_W, PANEL_H));
	}

	private int panelX() {
		return leftPos + imageWidth + 4;
	}

	private int panelY() {
		return topPos;
	}

	private int cellScreenX(int face) {
		return panelX() + 6 + FACE_CELLS[face][0] * (CELL_W + GAP);
	}

	private int cellScreenY(int face) {
		return panelY() + 16 + FACE_CELLS[face][1] * (CELL_H + GAP);
	}

	/** The machine face (0-5) under the mouse, or -1. */
	private int hoveredFace(double mx, double my) {
		for (int face = 0; face < 6; face++) {
			int cx = cellScreenX(face), cy = cellScreenY(face);
			if (mx >= cx && mx < cx + CELL_W && my >= cy && my < cy + CELL_H) return face;
		}
		return -1;
	}

	/** The world direction a machine face points (for the tooltip). */
	private Direction worldDirection(int face) {
		for (Direction d : Direction.values()) if (com.robvanblerk.tieredpower.energy.SideConfig.faceIndex(menu.getFacing(), d) == face) return d;
		return Direction.DOWN;
	}

	private java.util.List<Component> sideTooltip(int face) {
		java.util.List<Component> tip = new java.util.ArrayList<>();
		String world = worldDirection(face).getName();
		world = Character.toUpperCase(world.charAt(0)) + world.substring(1);
		if (face == 5) {
			tip.add(Component.literal("Bottom: power").withStyle(net.minecraft.ChatFormatting.RED));
			tip.add(Component.literal("Connect your power cable underneath").withStyle(net.minecraft.ChatFormatting.GRAY));
			return tip;
		}
		int mode = menu.getSideMode(face);
		tip.add(Component.literal(com.robvanblerk.tieredpower.energy.SideConfig.FACES[face] + ": " + com.robvanblerk.tieredpower.energy.SideConfig.MODES[mode]));
		tip.add(Component.literal(switch (mode) {
			case 1 -> "Items can be put in here";
			case 2 -> "Finished items can be taken out here";
			case 3 -> "Items in and finished items out";
			default -> "Nothing goes in or out here";
		}).withStyle(net.minecraft.ChatFormatting.GRAY));
		tip.add(Component.literal("Faces " + world + " in the world").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
		tip.add(Component.literal("Click to change").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
		return tip;
	}

	// ---- Upgrades window ----
	protected static final int UPG_X = 110, UPG_W = 136, UPG_H = 104;
	private boolean upgradesOpen;

	private boolean hasUpgradesButton() {
		return menu.hasUpgrades() || menu.supportsTiers();
	}

	private void setUpgradesOpen(boolean open) {
		upgradesOpen = open;
		menu.upgradesVisible = open;
	}

	private boolean overRemove(double mx, double my) {
		int rx = panelX() + 92, ry = panelY() + 66;
		return mx >= rx && mx < rx + 38 && my >= ry && my < ry + 10;
	}

	private void drawUpgradesButton(GuiGraphics g, int bx, int by, boolean hovered) {
		g.fill(bx, by, bx + RS_SIZE, by + RS_SIZE, hovered ? 0xFFFFFFFF : 0xFF373737);
		g.fill(bx + 1, by + 1, bx + RS_SIZE, by + RS_SIZE, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + RS_SIZE - 1, by + RS_SIZE - 1, upgradesOpen ? 0xFF4F8A4F : (hovered ? 0xFFA0C8A0 : 0xFF8B8B8B));
		// an up arrow
		for (int i = 0; i < 4; i++) g.fill(bx + 6 - i, by + 3 + i, bx + 8 + i, by + 4 + i, 0xFF2A5F2A);
		g.fill(bx + 6, by + 7, bx + 8, by + 11, 0xFF2A5F2A);
	}

	private void drawUpgradesPanel(GuiGraphics g) {
		com.robvanblerk.tieredpower.client.screen.GuiDraw.panel(g, panelX(), panelY(), UPG_W, UPG_H);
	}

	/** What a tier gives, for the upgrades panel. The Chunk Loader shows its area instead. */
	protected String tierEffect(int tier) {
		return com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.TIER_LANES[tier] + " at once";
	}

	private void drawUpgradesText(GuiGraphics g, int mouseX, int mouseY) {
		int x = panelX(), y = panelY();
		g.drawString(font, "Upgrades", x + 6, y + 5, 0x404040, false);
		if (menu.hasUpgrades()) {
			g.drawString(font, "Speed " + menu.getSpeedCount() + "/" + (menu.getSpeedCount() > 8 ? 16 : 8), x + 30, y + 19, 0x404040, false);
			g.drawString(font, String.format("x%.1f faster", menu.getSpeedMultiplier()), x + 30, y + 28, 0x707070, false);
			g.drawString(font, "Energy " + menu.getEfficiencyCount() + "/" + (menu.getEfficiencyCount() > 8 ? 16 : 8), x + 30, y + 41, 0x404040, false);
			g.drawString(font, "-" + Math.round(100 - 100 * com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.efficiencyFactor(menu.getEfficiencyCount())) + "% power", x + 30, y + 50, 0x707070, false);
		}
		g.drawString(font, "Muffler: " + (menu.isMuffled() ? "fitted" : "none"), x + 6, y + 67, 0x404040, false);
		if (menu.isMuffled()) {
			boolean hover = overRemove(mouseX, mouseY);
			g.drawString(font, "[Remove]", x + 92, y + 67, hover ? 0xFFAA0000 : 0xFF7A1F1F, false);
		}
		String tier = !menu.supportsTiers() ? "Tiers: not for this machine"
				: "Tier: " + com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.TIER_NAMES[menu.getTier()]
						+ " (" + tierEffect(menu.getTier()) + ")";
		g.drawString(font, tier, x + 6, y + 81, 0x404040, false);
		if (menu.supportsTiers() && menu.getTier() < com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.TIER_NAMES.length - 1)
			g.drawString(font, "Next: " + com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.TIER_NAMES[menu.getTier() + 1] + " Installer", x + 6, y + 91, 0x707070, false);
	}

	private void drawSidesButton(GuiGraphics g, int bx, int by, boolean hovered) {
		g.fill(bx, by, bx + RS_SIZE, by + RS_SIZE, hovered ? 0xFFFFFFFF : 0xFF373737);
		g.fill(bx + 1, by + 1, bx + RS_SIZE, by + RS_SIZE, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + RS_SIZE - 1, by + RS_SIZE - 1, sidesOpen ? 0xFF6A4F8A : (hovered ? 0xFFA0A0C8 : 0xFF8B8B8B));
		// a little cube: three coloured faces
		g.fill(bx + 4, by + 4, bx + 10, by + 10, 0xFFE0822E);
		g.fill(bx + 3, by + 3, bx + 9, by + 5, 0xFF3F6FD8);
		g.fill(bx + 9, by + 5, bx + 11, by + 11, 0xFF4FA84F);
	}

	private void drawSidesPanel(GuiGraphics g, int mouseX, int mouseY) {
		int x = panelX(), y = panelY();
		com.robvanblerk.tieredpower.client.screen.GuiDraw.panel(g, x, y, PANEL_W, PANEL_H);
		g.drawString(font, "Sides (as you face it)", x + 6, y + 5, 0x404040, false);
		int hovered = hoveredFace(mouseX, mouseY);
		for (int face = 0; face < 6; face++) {
			int cx = cellScreenX(face), cy = cellScreenY(face);
			int colour = face == 5 ? POWER_COLOUR : MODE_COLOURS[menu.getSideMode(face)];
			g.fill(cx, cy, cx + CELL_W, cy + CELL_H, hovered == face ? 0xFFFFFFFF : 0xFF373737);
			g.fill(cx + 1, cy + 1, cx + CELL_W - 1, cy + CELL_H - 1, colour);
			g.drawCenteredString(font, FACE_LABELS[face], cx + CELL_W / 2, cy + 5, 0xFFFFFF);
		}
		// legend
		int ly = y + 16 + 3 * (CELL_H + GAP) + 3;
		String[] names = {"In", "Out", "In + Out", "Off"};
		int[] cols = {MODE_COLOURS[1], MODE_COLOURS[2], MODE_COLOURS[3], MODE_COLOURS[0]};
		for (int i = 0; i < 4; i++) {
			int lx = x + 6 + (i % 2) * 66, lyy = ly + (i / 2) * 11;
			g.fill(lx, lyy + 1, lx + 7, lyy + 8, cols[i]);
			g.drawString(font, names[i], lx + 10, lyy, 0x404040, false);
		}
		g.drawString(font, "Hover a face for its compass direction", x + 6, ly + 24, 0x707070, false);
	}

	// ---- Auto-output button (just left of the redstone button) ----
	protected static final int OUT_X = 142;

	private void drawOutputButton(GuiGraphics g, int bx, int by, boolean hovered) {
		boolean on = menu.isAutoOutput();
		g.fill(bx, by, bx + RS_SIZE, by + RS_SIZE, hovered ? 0xFFFFFFFF : 0xFF373737);
		g.fill(bx + 1, by + 1, bx + RS_SIZE, by + RS_SIZE, 0xFFFFFFFF);
		g.fill(bx + 1, by + 1, bx + RS_SIZE - 1, by + RS_SIZE - 1, on ? 0xFF4FA84F : (hovered ? 0xFFA0A0C8 : 0xFF8B8B8B));
		// arrow pointing out of a box
		int c = on ? 0xFFFFFFFF : 0xFF404040;
		g.fill(bx + 3, by + 6, bx + 9, by + 8, c);
		g.fill(bx + 8, by + 4, bx + 10, by + 10, c);
		g.fill(bx + 10, by + 5, bx + 11, by + 9, c);
	}

	/** Sends a GUI button press to the server (handled by the menu's clickMenuButton). */
	protected void pressButton(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (menu.hasAutoOutput() && isHovering(SIDES_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY)) {
			sidesOpen = !sidesOpen;
			setUpgradesOpen(false);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		if (hasUpgradesButton() && isHovering(UPG_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY)) {
			setUpgradesOpen(!upgradesOpen);
			if (upgradesOpen) sidesOpen = false;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		if (upgradesOpen) {
			if (menu.isMuffled() && overRemove(mouseX, mouseY)) {
				pressButton(com.robvanblerk.tieredpower.menu.MachineMenu.BUTTON_REMOVE_MUFFLER);
				return true;
			}
			boolean onSlot = false;
			for (Slot s : menu.slots) if (s.isActive() && isHovering(s.x, s.y, 16, 16, mouseX, mouseY)) onSlot = true;
			boolean inPanel = mouseX >= panelX() && mouseX < panelX() + UPG_W && mouseY >= panelY() && mouseY < panelY() + UPG_H;
			if (inPanel && !onSlot) return true; // a click on the window itself, not "outside the GUI"
		}
		if (sidesOpen && menu.getFacing() != null) {
			int face = hoveredFace(mouseX, mouseY);
			if (face >= 0) {
				if (face != 5) pressButton(40 + face); // the bottom is always the power face
				return true;
			}
			if (mouseX >= panelX() && mouseX < panelX() + PANEL_W && mouseY >= panelY() && mouseY < panelY() + PANEL_H) return true;
		}
		if (menu.hasAutoOutput() && isHovering(OUT_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY)) {
			pressButton(20);
			return true;
		}
		if (menu.hasRedstoneControl() && isHovering(RS_X, RS_Y, RS_SIZE, RS_SIZE, mouseX, mouseY)
				&& minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/** e.g. "x1.5 speed  30 FE/t" for machines with upgrade slots. */
	protected String statusText() {
		String base = String.format("x%.1f speed  %d FE/t", menu.getSpeedMultiplier(), menu.getEnergyCost());
		int lanes = com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.TIER_LANES[Math.min(com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.TIER_LANES.length - 1, menu.getTier())];
		return menu.supportsTiers() && lanes > 1 ? base + "  " + lanes + " at once" : base;
	}

	protected abstract void drawContents(GuiGraphics g, int x, int y);

	/** Progress arrow (24x17) that fills left to right. */
	protected void drawArrow(GuiGraphics g, int ax, int ay, float fraction) {
		g.blit(WIDGETS, ax, ay, 0, 32, 24, 17);
		int w = (int) (24 * Math.min(1f, Math.max(0f, fraction)));
		if (w > 0) g.blit(WIDGETS, ax, ay, 24, 32, w, 17);
	}

	/** Flame (14x14) that burns down from the top. */
	protected void drawFlame(GuiGraphics g, int fx, int fy, float fraction) {
		g.blit(WIDGETS, fx, fy, 0, 52, 14, 14);
		int h = (int) Math.ceil(14 * Math.min(1f, Math.max(0f, fraction)));
		if (h > 0) g.blit(WIDGETS, fx, fy + 14 - h, 14, 52 + 14 - h, 14, h);
	}

	/** Textured vertical energy bar; (bx, by) is the inside top-left of a 14x52 bar. */
	protected void drawEnergyBar(GuiGraphics g, int bx, int by, int width, int height) {
		g.blit(WIDGETS, bx - 1, by - 1, 0, 70, 16, 54);
		long capacity = menu.getCapacity();
		int filled = capacity <= 0 ? 0 : (int) (52 * Math.min(1.0, (double) menu.getEnergy() / capacity));
		if (filled > 0) g.blit(WIDGETS, bx, by + 52 - filled, 17, 71 + 52 - filled, 14, filled);
	}

	/** Fluid tank: frame, coloured fill from the bottom, then gauge tick marks on top. */
	protected void drawTank(GuiGraphics g, int tx, int ty, float fraction, int colour) {
		g.blit(WIDGETS, tx - 1, ty - 1, 48, 70, 16, 54);
		int filled = (int) (52 * Math.min(1f, Math.max(0f, fraction)));
		if (filled > 0) g.fill(tx, ty + 52 - filled, tx + 14, ty + 52, colour);
		g.blit(WIDGETS, tx, ty, 32, 71, 14, 52);
	}

	protected String energyText() {
		return String.format("%,d / %,d FE", menu.getEnergy(), menu.getCapacity());
	}

	protected void text(GuiGraphics g, String s, int tx, int ty) {
		g.drawString(font, s, tx, ty, TEXT, false);
	}
}
