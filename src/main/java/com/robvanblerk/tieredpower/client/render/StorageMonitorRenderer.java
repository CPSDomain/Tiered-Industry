package com.robvanblerk.tieredpower.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.robvanblerk.tieredpower.block.entity.StorageMonitorBlockEntity;
import com.robvanblerk.tieredpower.energy.PowerInfo;

/** The chosen item on the top half of the screen, its count and name underneath. */
public class StorageMonitorRenderer implements BlockEntityRenderer<StorageMonitorBlockEntity> {
	public StorageMonitorRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(StorageMonitorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (!be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return;
		Direction facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
		Font font = Minecraft.getInstance().font;
		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
		pose.translate(0, 0, 0.501);
		if (be.getShown().isEmpty()) {
			text(pose, buffers, font, "Right-click", 0, 0xAAAAAA);
			text(pose, buffers, font, "with an item", 1, 0xAAAAAA);
		} else {
			// item icon, upper half
			pose.pushPose();
			pose.translate(0, 0.14, 0.01);
			pose.scale(0.36f, 0.36f, 0.02f);
			Minecraft.getInstance().getItemRenderer().renderStatic(be.getShown(), ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), 0);
			pose.popPose();
			long n = be.getCount();
			String count = n < 0 ? "offline" : PowerInfo.shortFe(n);
			text(pose, buffers, font, count, 2, n == 0 ? 0xFF5555 : n < 0 ? 0xAAAAAA : 0x55FF55);
			text(pose, buffers, font, be.getShown().getHoverName().getString(), 3, 0xFFFFFF);
		}
		pose.popPose();
	}

	/** One of 4 text rows in the middle 12x12 pixels of the face (rows 2-3 are the lower half). */
	private static void text(PoseStack pose, MultiBufferSource buffers, Font font, String s, int row, int colour) {
		float screen = 12f / 16f, rowH = screen / 4f, scale = rowH / 10f;
		pose.pushPose();
		pose.scale(scale, -scale, scale);
		float w = font.width(s), max = screen / scale;
		if (w > max) pose.scale(max / w, 1f, 1f); // squeeze long names to fit; still centred on -w/2
		font.drawInBatch(s, -w / 2f, -20f + 1f + row * 10f, colour, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
		pose.popPose();
	}

	@Override public int getViewDistance() { return 24; }
}
