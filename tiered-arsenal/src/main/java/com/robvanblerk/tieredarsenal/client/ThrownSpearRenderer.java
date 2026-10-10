package com.robvanblerk.tieredarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.robvanblerk.tieredarsenal.entity.ThrownSpear;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** Draws a thrown spear as its item, pointing the way it flies. */
public class ThrownSpearRenderer extends EntityRenderer<ThrownSpear> {
    private final ItemRenderer itemRenderer;

    public ThrownSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownSpear spear, float yaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, spear.yRotO, spear.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, spear.xRotO, spear.getXRot())));
        // The item texture points to the top-right; turn it so the tip leads.
        pose.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        pose.scale(1.4F, 1.4F, 1.4F);
        itemRenderer.renderStatic(spear.getSpear(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, spear.level(), spear.getId());
        pose.popPose();
        super.render(spear, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownSpear spear) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
