package com.robvanblerk.tieredpower.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Vanilla-style panels and slots drawn with plain rectangles (no texture needed). */
public final class GuiDraw {
	public static void panel(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, 0xFF000000);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
		g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
		g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
		g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
		g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
	}

	public static void slot(GuiGraphics g, int x, int y) {
		g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
		g.fill(x, y, x + 17, y + 1, 0xFF373737);
		g.fill(x, y, x + 1, y + 17, 0xFF373737);
		g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
		g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
	}

	/** A 26x26 output slot. */
	public static void bigSlot(GuiGraphics g, int x, int y) {
		g.fill(x, y, x + 26, y + 26, 0xFF8B8B8B);
		g.fill(x, y, x + 25, y + 1, 0xFF373737);
		g.fill(x, y, x + 1, y + 25, 0xFF373737);
		g.fill(x + 1, y + 25, x + 26, y + 26, 0xFFFFFFFF);
		g.fill(x + 25, y + 1, x + 26, y + 26, 0xFFFFFFFF);
	}

	/** A simple right-pointing arrow. */
	public static void arrow(GuiGraphics g, int x, int y, int colour) {
		g.fill(x, y + 6, x + 16, y + 10, colour);
		for (int i = 0; i < 7; i++) g.fill(x + 14 + i, y + 1 + i, x + 15 + i, y + 15 - i, colour);
	}

	/** A fluid drawn as a 16x16 square of its (tinted) texture. */
	public static void fluid(GuiGraphics g, net.minecraftforge.fluids.FluidStack fluid, int x, int y) {
		if (fluid.isEmpty()) return;
		var ext = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
		var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
		int tint = ext.getTintColor(fluid);
		float a = ((tint >> 24) & 0xFF) / 255f;
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f, a <= 0 ? 1f : a);
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		g.blit(x, y, 0, 16, 16, sprite);
		com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	/** "500m", "4B", "1.2kB" - a fluid amount short enough for a slot corner. */
	public static String fluidAmount(long mb) {
		if (mb < 1000) return mb + "m";
		if (mb % 1000 == 0 && mb < 10_000_000) return (mb / 1000) + "B";
		return com.robvanblerk.tieredpower.energy.PowerInfo.shortFe(mb / 1000).replace(",", "") + "B";
	}

	private GuiDraw() {}
}
