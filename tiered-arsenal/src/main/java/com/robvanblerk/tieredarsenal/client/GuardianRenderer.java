package com.robvanblerk.tieredarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.entity.AetheriumGuardian;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** A large purple skeleton with glowing eyes, using the vanilla skeleton model. */
public class GuardianRenderer extends HumanoidMobRenderer<AetheriumGuardian, SkeletonModel<AetheriumGuardian>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TieredArsenal.MOD_ID, "textures/entity/aetherium_guardian.png");
    private static final RenderType EYES =
            RenderType.eyes(new ResourceLocation(TieredArsenal.MOD_ID, "textures/entity/aetherium_guardian_eyes.png"));
    public static final float SCALE = 1.6F;

    public GuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.9F);
        addLayer(new Eyes(this));
    }

    @Override
    protected void scale(AetheriumGuardian guardian, PoseStack pose, float partialTicks) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(AetheriumGuardian guardian) {
        return TEXTURE;
    }

    private static class Eyes extends EyesLayer<AetheriumGuardian, SkeletonModel<AetheriumGuardian>> {
        Eyes(RenderLayerParent<AetheriumGuardian, SkeletonModel<AetheriumGuardian>> parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
