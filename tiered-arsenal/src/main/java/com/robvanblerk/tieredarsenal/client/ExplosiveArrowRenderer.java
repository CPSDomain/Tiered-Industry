package com.robvanblerk.tieredarsenal.client;

import com.robvanblerk.tieredarsenal.entity.ExplosiveArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ExplosiveArrowRenderer extends ArrowRenderer<ExplosiveArrow> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/projectiles/tipped_arrow.png");

    public ExplosiveArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ExplosiveArrow arrow) {
        return TEXTURE;
    }
}
