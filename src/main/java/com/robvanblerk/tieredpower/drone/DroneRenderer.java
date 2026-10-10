package com.robvanblerk.tieredpower.drone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Draws a drone from its item models: the body, four spinning rotors, and whatever it's carrying underneath. */
public class DroneRenderer extends EntityRenderer<DroneEntity> {
	public DroneRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.25f;
	}

	@Override
	public void render(DroneEntity drone, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		var items = Minecraft.getInstance().getItemRenderer();
		float time = drone.tickCount + partialTick;
		pose.pushPose();
		pose.translate(0, 0.25 + Mth.sin(time * 0.15f) * 0.03, 0);
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, drone.yRotO, drone.getYRot())));
		// body
		pose.pushPose();
		pose.scale(0.75f, 0.75f, 0.75f);
		items.renderStatic(new ItemStack(ModBlocks.UTILITY_DRONE.get()), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, drone.level(), drone.getId());
		pose.popPose();
		// rotors on the four arms
		ItemStack rotor = new ItemStack(ModBlocks.DRONE_ROTOR.get());
		for (int i = 0; i < 4; i++) {
			pose.pushPose();
			pose.translate((i % 2 == 0 ? -0.24 : 0.24), 0.07, (i < 2 ? -0.24 : 0.24));
			pose.mulPose(Axis.YP.rotationDegrees((time * 70 + i * 45) % 360));
			pose.scale(0.75f, 0.75f, 0.75f);
			items.renderStatic(rotor, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, drone.level(), drone.getId() + i + 1);
			pose.popPose();
		}
		// cargo
		ItemStack carried = drone.carried();
		if (!carried.isEmpty()) {
			pose.pushPose();
			pose.translate(0, -0.22, 0);
			pose.scale(0.4f, 0.4f, 0.4f);
			items.renderStatic(carried, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, drone.level(), drone.getId() + 9);
			pose.popPose();
		}
		pose.popPose();
		super.render(drone, yaw, partialTick, pose, buffers, light);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ResourceLocation getTextureLocation(DroneEntity drone) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
