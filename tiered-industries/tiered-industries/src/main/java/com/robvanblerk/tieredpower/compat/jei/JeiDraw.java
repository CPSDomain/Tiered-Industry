package com.robvanblerk.tieredpower.compat.jei;

import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import com.robvanblerk.tieredpower.client.screen.MachineScreen;

/** Small drawing helpers shared by the JEI categories (they reuse the machine GUI widgets texture). */
final class JeiDraw {
	static void slot(GuiGraphics g, int itemX, int itemY) {
		g.blit(MachineScreen.WIDGETS, itemX - 1, itemY - 1, 0, 0, 18, 18);
	}

	static void bigSlot(GuiGraphics g, int itemX, int itemY) {
		g.blit(MachineScreen.WIDGETS, itemX - 5, itemY - 5, 18, 0, 26, 26);
	}

	static void emptyArrow(GuiGraphics g, int x, int y) {
		g.blit(MachineScreen.WIDGETS, x, y, 0, 32, 24, 17);
	}

	static IDrawableAnimated animatedArrow(IGuiHelper helper, int ticks) {
		return helper.createAnimatedDrawable(helper.createDrawable(MachineScreen.WIDGETS, 24, 32, 24, 17),
				Math.max(20, ticks), IDrawableAnimated.StartDirection.LEFT, false);
	}

	static void text(GuiGraphics g, String s, int x, int y) {
		g.drawString(Minecraft.getInstance().font, s, x, y, 0xFF606060, false);
	}

	static String fe(long amount) {
		return String.format("%,d FE", amount);
	}

	private JeiDraw() {}
}
