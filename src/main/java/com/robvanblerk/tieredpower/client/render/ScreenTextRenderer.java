package com.robvanblerk.tieredpower.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Draws a few glowing lines of text on a block's front face (its FACING side), like a small screen. */
public abstract class ScreenTextRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
	public record Line(String text, int colour) {}

	/** Block pixels are 1/16; the screen area is the middle 12x12 pixels. */
	private static final float SCREEN = 12f / 16f;
	private static final int ROWS = 4;

	protected abstract List<Line> lines(T be);

	@Override
	public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (!be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return;
		Direction facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
		Font font = Minecraft.getInstance().font;
		List<Line> lines = lines(be);

		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
		pose.translate(0, 0, 0.5005);
		float rowHeight = SCREEN / ROWS;
		float scale = rowHeight / 10f;
		pose.scale(scale, -scale, scale);
		float top = -(ROWS * 10f) / 2f + 1f;
		float maxWidth = SCREEN / scale;
		for (int i = 0; i < lines.size() && i < ROWS; i++) {
			Line l = lines.get(i);
			float w = font.width(l.text());
			pose.pushPose();
			if (w > maxWidth) { // squeeze long lines to fit
				float squeeze = maxWidth / w;
				pose.scale(squeeze, 1f, 1f);
				w = maxWidth / squeeze;
			}
			font.drawInBatch(l.text(), -w / 2f, top + i * 10f, l.colour(), false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0,
					LightTexture.FULL_BRIGHT);
			pose.popPose();
		}
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(T be) {
		return false;
	}

	@Override
	public int getViewDistance() {
		return 24;
	}

	protected static boolean near(BlockEntity be) {
		var player = Minecraft.getInstance().player;
		return player != null && player.position().distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) < 24 * 24;
	}
}
