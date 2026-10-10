package com.robvanblerk.tieredpower.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.space.RocketEntity;

public final class ModEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TieredPower.MOD_ID);

	public static final RegistryObject<EntityType<RocketEntity>> ROCKET = ENTITIES.register("rocket",
			() -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC).sized(1.2f, 4.0f).clientTrackingRange(16).updateInterval(1).build("rocket"));

	public static final RegistryObject<EntityType<com.robvanblerk.tieredpower.drone.DroneEntity>> DRONE = ENTITIES.register("drone",
			() -> EntityType.Builder.<com.robvanblerk.tieredpower.drone.DroneEntity>of(com.robvanblerk.tieredpower.drone.DroneEntity::new, MobCategory.MISC)
					.sized(0.6f, 0.3f).clientTrackingRange(8).updateInterval(2).setShouldReceiveVelocityUpdates(true).build("drone"));

	private ModEntities() {}
}
