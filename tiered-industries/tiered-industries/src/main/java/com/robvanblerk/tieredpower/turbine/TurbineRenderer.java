package com.robvanblerk.tieredpower.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Spins a set of blades on every rotor of a formed Industrial Turbine, faster the more steam goes through. */
public class TurbineRenderer implements BlockEntityRenderer<TurbineControllerBlockEntity> {
	public TurbineRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(TurbineControllerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (!be.isFormed() || be.getLevel() == null || be.rotors().isEmpty()) return;
		long now = System.nanoTime();
		float dt = be.clientLastFrame == 0 ? 0 : Math.min(0.1f, (now - be.clientLastFrame) / 1.0e9f);
		be.clientLastFrame = now;
		be.clientAngle = (be.clientAngle + dt * (20 + 700 * be.getSpeed())) % 360f;
		ItemStack blades = new ItemStack(ModBlocks.TURBINE_BLADES.get());
		BlockPos origin = be.getBlockPos();
		var items = Minecraft.getInstance().getItemRenderer();
		for (BlockPos r : be.rotors()) {
			pose.pushPose();
			pose.translate(r.getX() - origin.getX() + 0.5, r.getY() - origin.getY() + 0.5, r.getZ() - origin.getZ() + 0.5);
			pose.mulPose(Axis.YP.rotationDegrees(be.clientAngle + (r.getY() % 2 == 0 ? 0 : 30)));
			int l = LevelRenderer.getLightColor(be.getLevel(), r);
			items.renderStatic(blades, ItemDisplayContext.NONE, l, OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), (int) r.asLong());
			pose.popPose();
		}
	}

	@Override
	public boolean shouldRenderOffScreen(TurbineControllerBlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
