package com.robvanblerk.tieredpower;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraft.core.BlockPos;
import com.robvanblerk.tieredpower.block.entity.ChunkLoaderBlockEntity;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;
import com.robvanblerk.tieredpower.registry.ModRecipes;
import com.robvanblerk.tieredpower.registry.ModFluids;

/** Main mod class. Forge creates this once at startup; we hook our registries onto the mod event bus. */
@Mod(TieredPower.MOD_ID)
public class TieredPower {
	public static final String MOD_ID = "tieredpower";
	public static final Logger LOGGER = LogUtils.getLogger();

	public TieredPower(FMLJavaModLoadingContext context) {
		IEventBus modBus = context.getModEventBus();
		context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
		ModFluids.TYPES.register(modBus);
		ModFluids.FLUIDS.register(modBus);
		ModBlocks.BLOCKS.register(modBus);
		ModBlocks.ITEMS.register(modBus);
		ModBlocks.TABS.register(modBus);
		ModBlockEntities.BLOCK_ENTITIES.register(modBus);
		ModMenus.MENUS.register(modBus);
		com.robvanblerk.tieredpower.registry.ModEntities.ENTITIES.register(modBus);
		ModRecipes.TYPES.register(modBus);
		ModRecipes.SERIALIZERS.register(modBus);
		com.robvanblerk.tieredpower.stargate.PlanetOnlyFilter.TYPES.register(modBus);
		com.robvanblerk.tieredpower.stargate.AddressTabletLoot.SERIALIZERS.register(modBus);
		com.robvanblerk.tieredpower.planet.PlanetRuinFeature.FEATURES.register(modBus);
		com.robvanblerk.tieredpower.network.ModNetwork.register();

		// When a world loads, drop chunk-loading tickets whose Chunk Loader no longer exists.
		ForgeChunkManager.setForcedChunkLoadingCallback(MOD_ID, (level, tickets) -> {
			for (BlockPos owner : new java.util.ArrayList<>(tickets.getBlockTickets().keySet())) {
				if (!(level.getBlockEntity(owner) instanceof ChunkLoaderBlockEntity)) tickets.removeAllTickets(owner);
			}
		});
	}
}
