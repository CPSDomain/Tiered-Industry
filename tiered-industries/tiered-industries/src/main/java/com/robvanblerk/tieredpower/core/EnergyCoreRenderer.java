package com.robvanblerk.tieredpower.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/** The glowing orb inside the Energy Core's cage: grows, brightens and spins faster as the core fills; coloured by tier. */
public class EnergyCoreRenderer implements BlockEntityRenderer<EnergyCoreBlockEntity> {
	public EnergyCoreRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(EnergyCoreBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (be.getLevel() == null) return;
		float fill = be.fill();
		float time = be.getLevel().getGameTime() + partialTick;
		float pulse = 1 + 0.04f * (float) Math.sin(time * 0.15);
		float size = (0.25f + 0.45f * fill) * pulse;
		ItemStack orb = new ItemStack(ModBlocks.ENERGY_CORE_ORB.get());
		orb.getOrCreateTag().putInt("tier", be.getTier());
		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(time * (1 + 4 * fill)));
		pose.mulPose(Axis.XP.rotationDegrees(time * 0.6f * (1 + 4 * fill)));
		pose.scale(size, size, size);
		Minecraft.getInstance().getItemRenderer().renderStatic(orb, ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), 0);
		pose.popPose();
	}

	@Override public int getViewDistance() { return 96; }
}
