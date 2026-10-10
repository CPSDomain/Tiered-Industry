package com.robvanblerk.tieredarsenal.client;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.registry.ModItems;
import com.robvanblerk.tieredarsenal.registry.ModEntities;
import com.robvanblerk.tieredarsenal.registry.ModMenus;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

/** Client-only setup: makes the tiered bows show their draw animation like the vanilla bow. */
@Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (RegistryObject<Item> bow : ModItems.BOWS.values()) {
                registerBowProperties(bow.get());
            }
            for (RegistryObject<Item> shield : ModItems.SHIELDS.values()) {
                ItemProperties.register(shield.get(), new ResourceLocation("blocking"), (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
            for (RegistryObject<Item> crossbow : ModItems.CROSSBOWS.values()) {
                registerCrossbowProperties(crossbow.get());
            }
            MenuScreens.register(ModMenus.ARMOURY_WORKBENCH.get(), ArmouryScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_SPEAR.get(), ThrownSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.EXPLOSIVE_ARROW.get(), ExplosiveArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.AETHERIUM_GUARDIAN.get(), GuardianRenderer::new);
    }

    /** Same item properties the vanilla crossbow uses, so the draw and loaded models switch. */
    private static void registerCrossbowProperties(Item crossbow) {
        ItemProperties.register(crossbow, new ResourceLocation("pull"), (stack, level, entity, seed) -> {
            if (entity == null || CrossbowItem.isCharged(stack)) return 0.0F;
            return entity.getUseItem() == stack
                    ? (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / CrossbowItem.getChargeDuration(stack)
                    : 0.0F;
        });
        ItemProperties.register(crossbow, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack)
                        ? 1.0F : 0.0F);
        ItemProperties.register(crossbow, new ResourceLocation("charged"), (stack, level, entity, seed) ->
                CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemProperties.register(crossbow, new ResourceLocation("firework"), (stack, level, entity, seed) ->
                CrossbowItem.isCharged(stack) && CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET)
                        ? 1.0F : 0.0F);
    }

    private static void registerBowProperties(Item bow) {
        ItemProperties.register(bow, new ResourceLocation("pull"), (stack, level, entity, seed) -> {
            if (entity == null || entity.getUseItem() != stack) return 0.0F;
            return (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F;
        });
        ItemProperties.register(bow, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }
}
