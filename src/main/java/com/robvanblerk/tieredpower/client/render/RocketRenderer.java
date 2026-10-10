package com.robvanblerk.tieredpower.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.space.RocketEntity;

/** Draws the rocket with its 3D item model, about four blocks tall. */
public class RocketRenderer extends EntityRenderer<RocketEntity> {
	private ItemStack stack;

	public RocketRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.6f;
	}

	@Override
	public void render(RocketEntity rocket, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		if (stack == null) stack = new ItemStack(ModBlocks.ROCKET.get());
		pose.pushPose();
		pose.scale(2.0f, 2.0f, 2.0f);
		pose.translate(0, 0.5, 0); // the item renderer centres models on the origin; stand it on the pad instead
		Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, rocket.level(), rocket.getId());
		pose.popPose();
		super.render(rocket, yaw, partialTick, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(RocketEntity rocket) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
