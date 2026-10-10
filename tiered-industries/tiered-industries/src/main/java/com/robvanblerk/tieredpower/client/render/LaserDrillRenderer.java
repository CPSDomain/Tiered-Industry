package com.robvanblerk.tieredpower.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.LaserDrillBlockEntity;

/** The orbital laser: a hot pink-red beam from the sky down onto the drill while it's firing. */
public class LaserDrillRenderer implements BlockEntityRenderer<LaserDrillBlockEntity> {
	private static final float[] COLOUR = {1.0f, 0.25f, 0.45f};

	public LaserDrillRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(LaserDrillBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		var state = be.getBlockState();
		if (be.getLevel() == null || !state.hasProperty(MachineBlock.LIT) || !state.getValue(MachineBlock.LIT)) return;
		BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0f, be.getLevel().getGameTime(),
				1, 1024, COLOUR, 0.12f, 0.3f);
	}

	@Override public boolean shouldRenderOffScreen(LaserDrillBlockEntity be) { return true; }
	@Override public int getViewDistance() { return 256; }
	@Override public boolean shouldRender(LaserDrillBlockEntity be, Vec3 camera) { return true; }
}
