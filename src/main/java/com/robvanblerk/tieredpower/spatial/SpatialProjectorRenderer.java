package com.robvanblerk.tieredpower.spatial;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** Outlines the cube a Spatial Projector will capture or fill. */
public class SpatialProjectorRenderer implements BlockEntityRenderer<SpatialProjectorBlockEntity> {
	public SpatialProjectorRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(SpatialProjectorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		int idx = be.getBlockState().getValue(SpatialProjectorBlock.SIZE);
		if (idx == 0) return;
		int n = SpatialProjectorBlockEntity.SIZES[idx], h = (n - 1) / 2;
		AABB box = new AABB(-h, 1, -h, h + 1, 1 + n, h + 1).inflate(0.002);
		LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, 0.45f, 0.85f, 1.0f, 0.9f);
	}

	@Override
	public boolean shouldRenderOffScreen(SpatialProjectorBlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 64;
	}
}
