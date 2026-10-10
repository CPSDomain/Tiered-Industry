package com.robvanblerk.tieredpower.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.BiomeColors;

import com.robvanblerk.tieredpower.registry.ModBlocks;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.client.screen.BatteryBoxScreen;
import com.robvanblerk.tieredpower.client.screen.BoilerScreen;
import com.robvanblerk.tieredpower.client.screen.ElectricPumpScreen;
import com.robvanblerk.tieredpower.client.screen.FreezerScreen;
import com.robvanblerk.tieredpower.client.screen.ProcessingMachineScreen;
import com.robvanblerk.tieredpower.client.screen.QuarryScreen;
import com.robvanblerk.tieredpower.client.screen.ItemFilterScreen;
import com.robvanblerk.tieredpower.client.screen.CropFarmerScreen;
import com.robvanblerk.tieredpower.client.screen.AutoCrafterScreen;
import com.robvanblerk.tieredpower.client.screen.ChunkLoaderScreen;
import com.robvanblerk.tieredpower.client.screen.WirelessChargerScreen;
import com.robvanblerk.tieredpower.client.screen.WindTurbineScreen;
import com.robvanblerk.tieredpower.client.screen.FissionReactorScreen;
import com.robvanblerk.tieredpower.client.screen.FissionControllerScreen;
import com.robvanblerk.tieredpower.client.screen.GasElectrolyzerScreen;
import com.robvanblerk.tieredpower.client.screen.GasBurnerGeneratorScreen;
import com.robvanblerk.tieredpower.client.screen.SteamEngineScreen;
import com.robvanblerk.tieredpower.client.screen.SteamHammerScreen;
import com.robvanblerk.tieredpower.client.screen.EnchantingMachineScreen;
import com.robvanblerk.tieredpower.client.screen.MobGrinderScreen;
import com.robvanblerk.tieredpower.client.screen.SpawnerControllerScreen;
import com.robvanblerk.tieredpower.client.screen.FluidMixerScreen;
import com.robvanblerk.tieredpower.client.render.FluidTankRenderer;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import com.robvanblerk.tieredpower.client.screen.BankControllerScreen;
import com.robvanblerk.tieredpower.client.screen.FusionControllerScreen;
import com.robvanblerk.tieredpower.client.screen.AlloySmelterScreen;
import com.robvanblerk.tieredpower.client.screen.ChargerScreen;
import com.robvanblerk.tieredpower.client.screen.PulverizerScreen;
import com.robvanblerk.tieredpower.client.screen.ElectrolyzerScreen;
import com.robvanblerk.tieredpower.client.screen.FusionReactorScreen;
import com.robvanblerk.tieredpower.client.screen.SteamTurbineScreen;
import com.robvanblerk.tieredpower.client.screen.CoalGeneratorScreen;
import com.robvanblerk.tieredpower.client.screen.ElectricFurnaceScreen;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Client-only setup: links each menu to the screen that draws it. Never loaded on dedicated servers. */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			// Crafting CPU / Accelerator items show their tier's light strip (item model overrides on "tieredpower:tier").
			for (var b : java.util.List.of(ModBlocks.CRAFTING_CPU, ModBlocks.CRAFTING_ACCELERATOR, ModBlocks.MACHINE_CONNECTOR))
				net.minecraft.client.renderer.item.ItemProperties.register(b.get().asItem(),
						net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "tier"),
						(stack, level, entity, seed) -> com.robvanblerk.tieredpower.storage.CraftingTier.of(stack));
			MenuScreens.register(ModMenus.COAL_GENERATOR.get(), CoalGeneratorScreen::new);
			MenuScreens.register(ModMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
			MenuScreens.register(ModMenus.BATTERY_BOX.get(), BatteryBoxScreen::new);
			MenuScreens.register(ModMenus.BOILER.get(), BoilerScreen::new);
			MenuScreens.register(ModMenus.STEAM_TURBINE.get(), SteamTurbineScreen::new);
			MenuScreens.register(ModMenus.ELECTROLYZER.get(), ElectrolyzerScreen::new);
			MenuScreens.register(ModMenus.FUSION_REACTOR.get(), FusionReactorScreen::new);
			MenuScreens.register(ModMenus.PULVERIZER.get(), PulverizerScreen::new);
			MenuScreens.register(ModMenus.ALLOY_SMELTER.get(), AlloySmelterScreen::new);
			MenuScreens.register(ModMenus.CHARGER.get(), ChargerScreen::new);
			MenuScreens.register(ModMenus.FUSION_CONTROLLER.get(), FusionControllerScreen::new);
			MenuScreens.register(ModMenus.BANK_CONTROLLER.get(), BankControllerScreen::new);
			MenuScreens.register(ModMenus.ELECTRIC_PUMP.get(), ElectricPumpScreen::new);
			MenuScreens.register(ModMenus.ROCK_CRUSHER.get(), ProcessingMachineScreen::new);
			MenuScreens.register(ModMenus.FREEZER.get(), FreezerScreen::new);
			MenuScreens.register(ModMenus.ORE_PURIFIER.get(), ProcessingMachineScreen::new);
			MenuScreens.register(ModMenus.QUARRY.get(), QuarryScreen::new);
			MenuScreens.register(ModMenus.ITEM_FILTER.get(), ItemFilterScreen::new);
			MenuScreens.register(ModMenus.QUANTUM_CHEST.get(), com.robvanblerk.tieredpower.quantum.QuantumChestScreen::new);
			MenuScreens.register(ModMenus.CROP_FARMER.get(), CropFarmerScreen::new);
			MenuScreens.register(ModMenus.AUTO_CRAFTER.get(), AutoCrafterScreen::new);
			MenuScreens.register(ModMenus.CHUNK_LOADER.get(), ChunkLoaderScreen::new);
			MenuScreens.register(ModMenus.DRONE_STATION.get(), com.robvanblerk.tieredpower.drone.DroneStationScreen::new);
			MenuScreens.register(ModMenus.TURBINE_CONTROLLER.get(), com.robvanblerk.tieredpower.turbine.TurbineControllerScreen::new);
			MenuScreens.register(ModMenus.SPATIAL_PROJECTOR.get(), com.robvanblerk.tieredpower.spatial.SpatialProjectorScreen::new);
			MenuScreens.register(ModMenus.STARGATE_DIALER.get(), com.robvanblerk.tieredpower.stargate.StargateDialerScreen::new);
			MenuScreens.register(ModMenus.WIRELESS_CHARGER.get(), WirelessChargerScreen::new);
			MenuScreens.register(ModMenus.WIND_TURBINE.get(), WindTurbineScreen::new);
			MenuScreens.register(ModMenus.FISSION_REACTOR.get(), FissionReactorScreen::new);
			MenuScreens.register(ModMenus.FISSION_CONTROLLER.get(), FissionControllerScreen::new);
			MenuScreens.register(ModMenus.DRIVE_BAY.get(), com.robvanblerk.tieredpower.client.screen.DriveBayScreen::new);
			MenuScreens.register(ModMenus.STORAGE_TERMINAL.get(), com.robvanblerk.tieredpower.client.screen.StorageTerminalScreen<com.robvanblerk.tieredpower.menu.StorageTerminalMenu>::new);
			MenuScreens.register(ModMenus.CRAFTING_TERMINAL.get(), com.robvanblerk.tieredpower.client.screen.CraftingTerminalScreen::new);
			MenuScreens.register(ModMenus.STORAGE_BUS.get(), com.robvanblerk.tieredpower.client.screen.StorageBusScreen::new);
			MenuScreens.register(ModMenus.MOLECULAR_ASSEMBLER.get(), com.robvanblerk.tieredpower.client.screen.MolecularAssemblerScreen::new);
			MenuScreens.register(ModMenus.PATTERN_ENCODER.get(), com.robvanblerk.tieredpower.client.screen.PatternEncoderScreen::new);
			MenuScreens.register(ModMenus.STOCK_KEEPER.get(), com.robvanblerk.tieredpower.client.screen.StockKeeperScreen::new);
			MenuScreens.register(ModMenus.STORM_CALLER.get(), com.robvanblerk.tieredpower.client.screen.StormCallerScreen::new);
			MenuScreens.register(ModMenus.SMITHING_PRESS.get(), com.robvanblerk.tieredpower.client.screen.SmithingPressScreen::new);
			MenuScreens.register(ModMenus.LOGIC_CONTROLLER.get(), com.robvanblerk.tieredpower.logic.LogicControllerScreen::new);
			MenuScreens.register(ModMenus.MACHINE_STATUS.get(), com.robvanblerk.tieredpower.logic.MachineStatusScreen::new);
			MenuScreens.register(ModMenus.DIGITAL_FACTORY.get(), com.robvanblerk.tieredpower.factory.DigitalFactoryScreen::new);
			MenuScreens.register(ModMenus.BIO_REFINERY.get(), com.robvanblerk.tieredpower.industry.BioRefineryScreen::new);
			MenuScreens.register(ModMenus.COKE_OVEN.get(), com.robvanblerk.tieredpower.industry.HeavyFurnaceScreen::new);
			MenuScreens.register(ModMenus.INDUSTRIAL_BLAST_FURNACE.get(), com.robvanblerk.tieredpower.industry.HeavyFurnaceScreen::new);
			MenuScreens.register(ModMenus.BREWING_MACHINE.get(), com.robvanblerk.tieredpower.client.screen.BrewingMachineScreen::new);
			MenuScreens.register(ModMenus.LASER_DRILL.get(), com.robvanblerk.tieredpower.client.screen.WorkerScreen::new);
			MenuScreens.register(ModMenus.DIGITAL_MINER.get(), com.robvanblerk.tieredpower.client.screen.WorkerScreen::new);
			MenuScreens.register(ModMenus.LAUNCH_CONTROLLER.get(), com.robvanblerk.tieredpower.client.screen.LaunchControllerScreen::new);
			MenuScreens.register(ModMenus.CRYOGENIC_CONDENSER.get(), com.robvanblerk.tieredpower.client.screen.RocketFuelScreen::new);
			MenuScreens.register(ModMenus.FUEL_REFINERY.get(), com.robvanblerk.tieredpower.client.screen.RocketFuelScreen::new);
			MenuScreens.register(ModMenus.PARTICLE_COLLIDER.get(), com.robvanblerk.tieredpower.client.screen.ParticleColliderScreen::new);
			MenuScreens.register(ModMenus.FLUID_FILTER.get(), com.robvanblerk.tieredpower.client.screen.FluidFilterScreen::new);
			MenuScreens.register(ModMenus.MATRIX_CONTROLLER.get(), com.robvanblerk.tieredpower.client.screen.MatrixControllerScreen::new);
			MenuScreens.register(ModMenus.BLOCK_BREAKER.get(), com.robvanblerk.tieredpower.client.screen.WorkerScreen::new);
			MenuScreens.register(ModMenus.BLOCK_PLACER.get(), com.robvanblerk.tieredpower.client.screen.WorkerScreen::new);
			MenuScreens.register(ModMenus.TREE_FARM.get(), com.robvanblerk.tieredpower.client.screen.WorkerScreen::new);
			MenuScreens.register(ModMenus.ANIMAL_RANCH.get(), com.robvanblerk.tieredpower.client.screen.WorkerScreen::new);
			MenuScreens.register(ModMenus.SALT_EVAPORATOR.get(), com.robvanblerk.tieredpower.client.screen.SaltEvaporatorScreen::new);
			MenuScreens.register(ModMenus.BRINE_ELECTROLYZER.get(), com.robvanblerk.tieredpower.client.screen.BrineElectrolyzerScreen::new);
			MenuScreens.register(ModMenus.CHEMICAL_WASHER.get(), com.robvanblerk.tieredpower.client.screen.ChemicalWasherScreen::new);
			MenuScreens.register(ModMenus.DISK_WORKBENCH.get(), com.robvanblerk.tieredpower.client.screen.DiskWorkbenchScreen::new);
			MenuScreens.register(ModMenus.SECURITY_TERMINAL.get(), com.robvanblerk.tieredpower.client.screen.SecurityTerminalScreen::new);
			MenuScreens.register(ModMenus.OXYGEN_FURNACE.get(), com.robvanblerk.tieredpower.client.screen.OxygenFurnaceScreen::new);
			MenuScreens.register(ModMenus.BIO_DIGESTER.get(), com.robvanblerk.tieredpower.client.screen.BioDigesterScreen::new);
			MenuScreens.register(ModMenus.ISOTOPE_SEPARATOR.get(), com.robvanblerk.tieredpower.client.screen.IsotopeSeparatorScreen::new);
			MenuScreens.register(ModMenus.AIR_SEPARATOR.get(), com.robvanblerk.tieredpower.client.screen.AirSeparatorScreen::new);
			MenuScreens.register(ModMenus.TRITIUM_BREEDER.get(), com.robvanblerk.tieredpower.client.screen.TritiumBreederScreen::new);
			MenuScreens.register(ModMenus.GAS_ELECTROLYZER.get(), GasElectrolyzerScreen::new);
			MenuScreens.register(ModMenus.GAS_BURNER_GENERATOR.get(), GasBurnerGeneratorScreen::new);
			MenuScreens.register(ModMenus.STEAM_ENGINE.get(), SteamEngineScreen::new);
			MenuScreens.register(ModMenus.STEAM_HAMMER.get(), SteamHammerScreen::new);
			MenuScreens.register(ModMenus.ENCHANTING_MACHINE.get(), EnchantingMachineScreen::new);
			MenuScreens.register(ModMenus.MOB_GRINDER.get(), MobGrinderScreen::new);
			MenuScreens.register(ModMenus.SPAWNER_CONTROLLER.get(), SpawnerControllerScreen::new);
			MenuScreens.register(ModMenus.FLUID_MIXER.get(), FluidMixerScreen::new);
			MenuScreens.register(ModMenus.COMPRESSOR.get(), ProcessingMachineScreen::new);
			MenuScreens.register(ModMenus.ELECTRIC_SAWMILL.get(), ProcessingMachineScreen::new);
		});
	}

	/** Draws the fluid inside Fluid Tanks. */
	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.WIND_TURBINE.get(), com.robvanblerk.tieredpower.client.render.WindTurbineRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.ENERGY_METER.get(), com.robvanblerk.tieredpower.client.render.EnergyMeterRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.POWER_MONITOR.get(), com.robvanblerk.tieredpower.client.render.PowerMonitorRenderer::new);
		event.registerEntityRenderer(com.robvanblerk.tieredpower.registry.ModEntities.ROCKET.get(), com.robvanblerk.tieredpower.client.render.RocketRenderer::new);
		event.registerEntityRenderer(com.robvanblerk.tieredpower.registry.ModEntities.DRONE.get(), com.robvanblerk.tieredpower.drone.DroneRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.LASER_DRILL.get(), com.robvanblerk.tieredpower.client.render.LaserDrillRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.ENERGY_CORE.get(), com.robvanblerk.tieredpower.core.EnergyCoreRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.TURBINE_CONTROLLER.get(), com.robvanblerk.tieredpower.turbine.TurbineRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.SPATIAL_PROJECTOR.get(), com.robvanblerk.tieredpower.spatial.SpatialProjectorRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.STARGATE_RING.get(), com.robvanblerk.tieredpower.stargate.StargateRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.REACTOR_GAUGE.get(), com.robvanblerk.tieredpower.client.render.ReactorGaugeRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.STORAGE_MONITOR.get(), com.robvanblerk.tieredpower.client.render.StorageMonitorRenderer::new);
	}

	/** Tints the water surface in the Sink to match the biome's water colour. */
	@SubscribeEvent
	public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
		// Elevators take the colour of their dye.
		event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? state.getValue(com.robvanblerk.tieredpower.elevator.ElevatorBlock.COLOR).getTextColor() : 0xFFFFFF,
				ModBlocks.ELEVATOR.get());
		event.register((state, level, pos, tintIndex) -> level != null && pos != null
				? BiomeColors.getAverageWaterColor(level, pos) : 0x3F76E4, ModBlocks.SINK.get());
	}

	@SubscribeEvent
	public static void onItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, layer) -> layer == 0 ? 0xFFF9FFFE : 0xFFFFFFFF, ModBlocks.ELEVATOR.get());
		// Energy Core orb: coloured by the core's tier.
		event.register((stack, layer) -> {
			int t = stack.getTag() == null ? 1 : Math.max(1, Math.min(5, stack.getTag().getInt("tier")));
			return 0xFF000000 | com.robvanblerk.tieredpower.core.EnergyCoreBlockEntity.COLOUR[t - 1];
		}, ModBlocks.ENERGY_CORE_ORB.get());
		// Gas Cylinder: the shoulder (layer 1) takes the colour of the gas inside.
		event.register((stack, layer) -> {
			if (layer != 1) return 0xFFFFFFFF;
			var f = com.robvanblerk.tieredpower.item.GasCylinderItem.contents(stack);
			if (f.isEmpty()) return 0xFF8A8F99;
			int c = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(f.getFluid()).getTintColor(f);
			return 0xFF000000 | (c & 0xFFFFFF);
		}, ModBlocks.GAS_CYLINDER.get());
		event.register((stack, tintIndex) -> 0x3F76E4, ModBlocks.SINK.get());
	}

	private ClientSetup() {}
}
