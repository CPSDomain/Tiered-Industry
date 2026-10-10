package com.robvanblerk.tieredarsenal.registry;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.entity.AetheriumGuardian;
import com.robvanblerk.tieredarsenal.entity.ExplosiveArrow;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.robvanblerk.tieredarsenal.entity.ThrownSpear;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TieredArsenal.MOD_ID);

    public static final RegistryObject<EntityType<ThrownSpear>> THROWN_SPEAR = ENTITIES.register("thrown_spear",
            () -> EntityType.Builder.<ThrownSpear>of(ThrownSpear::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(TieredArsenal.MOD_ID + ":thrown_spear"));

    public static final RegistryObject<EntityType<ExplosiveArrow>> EXPLOSIVE_ARROW = ENTITIES.register("explosive_arrow",
            () -> EntityType.Builder.<ExplosiveArrow>of(ExplosiveArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(TieredArsenal.MOD_ID + ":explosive_arrow"));

    public static final RegistryObject<EntityType<AetheriumGuardian>> AETHERIUM_GUARDIAN = ENTITIES.register("aetherium_guardian",
            () -> EntityType.Builder.<AetheriumGuardian>of(AetheriumGuardian::new, MobCategory.MONSTER)
                    .sized(1.0F, 3.2F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(TieredArsenal.MOD_ID + ":aetherium_guardian"));

    /** Mob attributes for the boss. */
    @Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Attributes {
        private Attributes() {}

        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(AETHERIUM_GUARDIAN.get(), AetheriumGuardian.createAttributes().build());
        }
    }
}
