package com.robvanblerk.tieredarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.robvanblerk.tieredarsenal.item.TieredShieldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the tiered shields with the vanilla shield model and our own textures. */
public class TieredShieldRenderer extends BlockEntityWithoutLevelRenderer {
    private static TieredShieldRenderer instance;
    private ShieldModel model;

    public static TieredShieldRenderer get() {
        if (instance == null) {
            instance = new TieredShieldRenderer();
        }
        return instance;
    }

    private TieredShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof TieredShieldItem shield)) return;
        if (model == null) {
            model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        }
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, model.renderType(shield.getTexture()),
                true, stack.hasFoil());
        model.renderToBuffer(pose, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }
}
