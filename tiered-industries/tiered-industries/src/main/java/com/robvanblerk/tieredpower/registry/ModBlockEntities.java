package com.robvanblerk.tieredpower.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.entity.BatteryBoxBlockEntity;
import com.robvanblerk.tieredpower.block.entity.BoilerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.SinkBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FluidPipeBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ElectricPumpBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FluidTankBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FreezerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.RockCrusherBlockEntity;
import com.robvanblerk.tieredpower.block.entity.QuarryBlockEntity;
import com.robvanblerk.tieredpower.block.entity.CropFarmerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.AutoCrafterBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ChunkLoaderBlockEntity;
import com.robvanblerk.tieredpower.block.entity.WirelessChargerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.SolarPanelBlockEntity;
import com.robvanblerk.tieredpower.block.entity.WindTurbineBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FissionReactorBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FissionPortBlockEntity;
import com.robvanblerk.tieredpower.block.entity.EnchantingMachineBlockEntity;
import com.robvanblerk.tieredpower.block.entity.MobGrinderBlockEntity;
import com.robvanblerk.tieredpower.block.entity.SpawnerControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FluidMixerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.TeleporterBlockEntity;
import com.robvanblerk.tieredpower.block.entity.CondenserBlockEntity;
import com.robvanblerk.tieredpower.block.entity.GasElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.GasBurnerGeneratorBlockEntity;
import com.robvanblerk.tieredpower.block.entity.SteamEngineBlockEntity;
import com.robvanblerk.tieredpower.block.entity.SteamHammerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.OrePurifierBlockEntity;
import com.robvanblerk.tieredpower.block.entity.CompressorBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ElectricSawmillBlockEntity;
import com.robvanblerk.tieredpower.block.entity.BankControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.BankPortBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FusionControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ReactorPortBlockEntity;
import com.robvanblerk.tieredpower.block.entity.AlloySmelterBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ChargerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.PulverizerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FusionReactorBlockEntity;
import com.robvanblerk.tieredpower.block.entity.SteamTurbineBlockEntity;
import com.robvanblerk.tieredpower.block.entity.CableBlockEntity;
import com.robvanblerk.tieredpower.block.entity.CoalGeneratorBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ElectricFurnaceBlockEntity;

/** Block entities are the "brains" of our blocks: they hold energy/items and tick every game tick. */
public final class ModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
			DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TieredPower.MOD_ID);

	public static final RegistryObject<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR =
			BLOCK_ENTITIES.register("coal_generator", () -> BlockEntityType.Builder
					.of(CoalGeneratorBlockEntity::new, ModBlocks.COAL_GENERATOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<BatteryBoxBlockEntity>> BATTERY_BOX =
			BLOCK_ENTITIES.register("battery_box", () -> BlockEntityType.Builder
					.of(BatteryBoxBlockEntity::new, ModBlocks.BATTERY_BOX.get(), ModBlocks.ADVANCED_BATTERY_BOX.get(),
							ModBlocks.ELITE_BATTERY_BOX.get(), ModBlocks.ULTIMATE_BATTERY_BOX.get(), ModBlocks.QUANTUM_BATTERY_BOX.get()).build(null));

	public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
			BLOCK_ENTITIES.register("electric_furnace", () -> BlockEntityType.Builder
					.of(ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRIC_FURNACE.get()).build(null));

	public static final RegistryObject<BlockEntityType<BoilerBlockEntity>> BOILER =
			BLOCK_ENTITIES.register("boiler", () -> BlockEntityType.Builder
					.of(BoilerBlockEntity::new, ModBlocks.BOILER.get()).build(null));

	public static final RegistryObject<BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE =
			BLOCK_ENTITIES.register("steam_turbine", () -> BlockEntityType.Builder
					.of(SteamTurbineBlockEntity::new, ModBlocks.STEAM_TURBINE.get()).build(null));

	public static final RegistryObject<BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER =
			BLOCK_ENTITIES.register("electrolyzer", () -> BlockEntityType.Builder
					.of(ElectrolyzerBlockEntity::new, ModBlocks.ELECTROLYZER.get()).build(null));

	public static final RegistryObject<BlockEntityType<FusionReactorBlockEntity>> FUSION_REACTOR =
			BLOCK_ENTITIES.register("fusion_reactor", () -> BlockEntityType.Builder
					.of(FusionReactorBlockEntity::new, ModBlocks.FUSION_REACTOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<PulverizerBlockEntity>> PULVERIZER =
			BLOCK_ENTITIES.register("pulverizer", () -> BlockEntityType.Builder
					.of(PulverizerBlockEntity::new, ModBlocks.PULVERIZER.get()).build(null));

	public static final RegistryObject<BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER =
			BLOCK_ENTITIES.register("alloy_smelter", () -> BlockEntityType.Builder
					.of(AlloySmelterBlockEntity::new, ModBlocks.ALLOY_SMELTER.get()).build(null));

	public static final RegistryObject<BlockEntityType<ChargerBlockEntity>> CHARGER =
			BLOCK_ENTITIES.register("charger", () -> BlockEntityType.Builder
					.of(ChargerBlockEntity::new, ModBlocks.CHARGER.get()).build(null));

	public static final RegistryObject<BlockEntityType<FusionControllerBlockEntity>> FUSION_CONTROLLER =
			BLOCK_ENTITIES.register("fusion_controller", () -> BlockEntityType.Builder
					.of(FusionControllerBlockEntity::new, ModBlocks.FUSION_CONTROLLER.get()).build(null));

	public static final RegistryObject<BlockEntityType<ReactorPortBlockEntity>> REACTOR_PORT =
			BLOCK_ENTITIES.register("reactor_port", () -> BlockEntityType.Builder
					.of(ReactorPortBlockEntity::new, ModBlocks.REACTOR_FUEL_PORT.get(), ModBlocks.REACTOR_POWER_PORT.get()).build(null));

	public static final RegistryObject<BlockEntityType<BankControllerBlockEntity>> BANK_CONTROLLER =
			BLOCK_ENTITIES.register("bank_controller", () -> BlockEntityType.Builder
					.of(BankControllerBlockEntity::new, ModBlocks.BANK_CONTROLLER.get()).build(null));

	public static final RegistryObject<BlockEntityType<BankPortBlockEntity>> BANK_PORT =
			BLOCK_ENTITIES.register("bank_port", () -> BlockEntityType.Builder
					.of(BankPortBlockEntity::new, ModBlocks.BANK_PORT.get()).build(null));

	public static final RegistryObject<BlockEntityType<SinkBlockEntity>> SINK =
			BLOCK_ENTITIES.register("sink", () -> BlockEntityType.Builder
					.of(SinkBlockEntity::new, ModBlocks.SINK.get()).build(null));

	public static final RegistryObject<BlockEntityType<ElectricPumpBlockEntity>> ELECTRIC_PUMP =
			BLOCK_ENTITIES.register("electric_pump", () -> BlockEntityType.Builder
					.of(ElectricPumpBlockEntity::new, ModBlocks.ELECTRIC_PUMP.get()).build(null));

	public static final RegistryObject<BlockEntityType<RockCrusherBlockEntity>> ROCK_CRUSHER =
			BLOCK_ENTITIES.register("rock_crusher", () -> BlockEntityType.Builder
					.of(RockCrusherBlockEntity::new, ModBlocks.ROCK_CRUSHER.get()).build(null));

	public static final RegistryObject<BlockEntityType<FreezerBlockEntity>> FREEZER =
			BLOCK_ENTITIES.register("freezer", () -> BlockEntityType.Builder
					.of(FreezerBlockEntity::new, ModBlocks.FREEZER.get()).build(null));

	public static final RegistryObject<BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
			BLOCK_ENTITIES.register("fluid_tank", () -> BlockEntityType.Builder
					.of(FluidTankBlockEntity::new, ModBlocks.BASIC_FLUID_TANK.get(), ModBlocks.ADVANCED_FLUID_TANK.get(),
							ModBlocks.ELITE_FLUID_TANK.get(), ModBlocks.ULTIMATE_FLUID_TANK.get(), ModBlocks.QUANTUM_FLUID_TANK.get()).build(null));

	public static final RegistryObject<BlockEntityType<OrePurifierBlockEntity>> ORE_PURIFIER =
			BLOCK_ENTITIES.register("ore_purifier", () -> BlockEntityType.Builder
					.of(OrePurifierBlockEntity::new, ModBlocks.ORE_PURIFIER.get()).build(null));

	public static final RegistryObject<BlockEntityType<CompressorBlockEntity>> COMPRESSOR =
			BLOCK_ENTITIES.register("compressor", () -> BlockEntityType.Builder
					.of(CompressorBlockEntity::new, ModBlocks.COMPRESSOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<ElectricSawmillBlockEntity>> ELECTRIC_SAWMILL =
			BLOCK_ENTITIES.register("electric_sawmill", () -> BlockEntityType.Builder
					.of(ElectricSawmillBlockEntity::new, ModBlocks.ELECTRIC_SAWMILL.get()).build(null));

	public static final RegistryObject<BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
			BLOCK_ENTITIES.register("fluid_pipe", () -> BlockEntityType.Builder
					.of(FluidPipeBlockEntity::new, ModBlocks.pipeBlocks()).build(null));

	public static final RegistryObject<BlockEntityType<QuarryBlockEntity>> QUARRY =
			BLOCK_ENTITIES.register("quarry", () -> BlockEntityType.Builder
					.of(QuarryBlockEntity::new, ModBlocks.QUARRY.get()).build(null));

	public static final RegistryObject<BlockEntityType<CropFarmerBlockEntity>> CROP_FARMER =
			BLOCK_ENTITIES.register("crop_farmer", () -> BlockEntityType.Builder
					.of(CropFarmerBlockEntity::new, ModBlocks.CROP_FARMER.get()).build(null));

	public static final RegistryObject<BlockEntityType<AutoCrafterBlockEntity>> AUTO_CRAFTER =
			BLOCK_ENTITIES.register("auto_crafter", () -> BlockEntityType.Builder
					.of(AutoCrafterBlockEntity::new, ModBlocks.AUTO_CRAFTER.get()).build(null));

	public static final RegistryObject<BlockEntityType<ChunkLoaderBlockEntity>> CHUNK_LOADER =
			BLOCK_ENTITIES.register("chunk_loader", () -> BlockEntityType.Builder
					.of(ChunkLoaderBlockEntity::new, ModBlocks.CHUNK_LOADER.get()).build(null));

	public static final RegistryObject<BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE =
			BLOCK_ENTITIES.register("item_pipe", () -> BlockEntityType.Builder
					.of(ItemPipeBlockEntity::new, ModBlocks.itemPipeBlocks()).build(null));

	public static final RegistryObject<BlockEntityType<WirelessChargerBlockEntity>> WIRELESS_CHARGER =
			BLOCK_ENTITIES.register("wireless_charger", () -> BlockEntityType.Builder
					.of(WirelessChargerBlockEntity::new, ModBlocks.WIRELESS_CHARGER.get()).build(null));

	public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
			BLOCK_ENTITIES.register("solar_panel", () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new,
					ModBlocks.SOLAR_PANEL.get(), ModBlocks.ADVANCED_SOLAR_PANEL.get(), ModBlocks.ELITE_SOLAR_PANEL.get(), ModBlocks.ULTIMATE_SOLAR_PANEL.get(), ModBlocks.QUANTUM_SOLAR_PANEL.get(), ModBlocks.STELLAR_SOLAR_PANEL.get()).build(null));

	public static final RegistryObject<BlockEntityType<WindTurbineBlockEntity>> WIND_TURBINE =
			BLOCK_ENTITIES.register("wind_turbine", () -> BlockEntityType.Builder.of(WindTurbineBlockEntity::new, ModBlocks.WIND_TURBINE.get()).build(null));

	public static final RegistryObject<BlockEntityType<FissionReactorBlockEntity>> FISSION_REACTOR =
			BLOCK_ENTITIES.register("fission_reactor", () -> BlockEntityType.Builder.of(FissionReactorBlockEntity::new, ModBlocks.FISSION_REACTOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<FissionControllerBlockEntity>> FISSION_CONTROLLER =
			BLOCK_ENTITIES.register("fission_controller", () -> BlockEntityType.Builder.of(FissionControllerBlockEntity::new, ModBlocks.FISSION_CONTROLLER.get()).build(null));

	public static final RegistryObject<BlockEntityType<FissionPortBlockEntity>> FISSION_PORT =
			BLOCK_ENTITIES.register("fission_port", () -> BlockEntityType.Builder.of(FissionPortBlockEntity::new,
					ModBlocks.FISSION_FUEL_PORT.get(), ModBlocks.FISSION_WASTE_PORT.get(), ModBlocks.FISSION_COOLANT_PORT.get(), ModBlocks.FISSION_POWER_PORT.get()).build(null));

	public static final RegistryObject<BlockEntityType<EnchantingMachineBlockEntity>> ENCHANTING_MACHINE =
			BLOCK_ENTITIES.register("enchanting_machine", () -> BlockEntityType.Builder.of(EnchantingMachineBlockEntity::new, ModBlocks.ENCHANTING_MACHINE.get()).build(null));

	public static final RegistryObject<BlockEntityType<MobGrinderBlockEntity>> MOB_GRINDER =
			BLOCK_ENTITIES.register("mob_grinder", () -> BlockEntityType.Builder.of(MobGrinderBlockEntity::new, ModBlocks.MOB_GRINDER.get()).build(null));

	public static final RegistryObject<BlockEntityType<SpawnerControllerBlockEntity>> SPAWNER_CONTROLLER =
			BLOCK_ENTITIES.register("spawner_controller", () -> BlockEntityType.Builder.of(SpawnerControllerBlockEntity::new, ModBlocks.SPAWNER_CONTROLLER.get()).build(null));

	public static final RegistryObject<BlockEntityType<FluidMixerBlockEntity>> FLUID_MIXER =
			BLOCK_ENTITIES.register("fluid_mixer", () -> BlockEntityType.Builder.of(FluidMixerBlockEntity::new, ModBlocks.FLUID_MIXER.get()).build(null));

	public static final RegistryObject<BlockEntityType<TeleporterBlockEntity>> TELEPORTER =
			BLOCK_ENTITIES.register("teleporter", () -> BlockEntityType.Builder.of(TeleporterBlockEntity::new, ModBlocks.TELEPORTER.get()).build(null));

	public static final RegistryObject<BlockEntityType<CondenserBlockEntity>> CONDENSER =
			BLOCK_ENTITIES.register("condenser", () -> BlockEntityType.Builder.of(CondenserBlockEntity::new, ModBlocks.CONDENSER.get()).build(null));

	public static final RegistryObject<BlockEntityType<GasElectrolyzerBlockEntity>> GAS_ELECTROLYZER =
			BLOCK_ENTITIES.register("gas_electrolyzer", () -> BlockEntityType.Builder.of(GasElectrolyzerBlockEntity::new, ModBlocks.GAS_ELECTROLYZER.get()).build(null));

	public static final RegistryObject<BlockEntityType<GasBurnerGeneratorBlockEntity>> GAS_BURNER_GENERATOR =
			BLOCK_ENTITIES.register("gas_burner_generator", () -> BlockEntityType.Builder.of(GasBurnerGeneratorBlockEntity::new, ModBlocks.GAS_BURNER_GENERATOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE =
			BLOCK_ENTITIES.register("steam_engine", () -> BlockEntityType.Builder.of(SteamEngineBlockEntity::new, ModBlocks.STEAM_ENGINE.get()).build(null));

	public static final RegistryObject<BlockEntityType<SteamHammerBlockEntity>> STEAM_HAMMER =
			BLOCK_ENTITIES.register("steam_hammer", () -> BlockEntityType.Builder.of(SteamHammerBlockEntity::new, ModBlocks.STEAM_HAMMER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.EnergyMeterBlockEntity>> ENERGY_METER =
			BLOCK_ENTITIES.register("energy_meter", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.EnergyMeterBlockEntity::new, ModBlocks.ENERGY_METER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.PowerMonitorBlockEntity>> POWER_MONITOR =
			BLOCK_ENTITIES.register("power_monitor", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.PowerMonitorBlockEntity::new, ModBlocks.POWER_MONITOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity>> STORAGE_CONTROLLER =
			BLOCK_ENTITIES.register("storage_controller", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity::new, ModBlocks.STORAGE_CONTROLLER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.DriveBayBlockEntity>> DRIVE_BAY =
			BLOCK_ENTITIES.register("drive_bay", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.DriveBayBlockEntity::new, ModBlocks.DRIVE_BAY.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.StorageBusBlockEntity>> STORAGE_BUS =
			BLOCK_ENTITIES.register("storage_bus", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.StorageBusBlockEntity::new,
					ModBlocks.IMPORT_BUS.get(), ModBlocks.EXPORT_BUS.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.StorageInterfaceBlockEntity>> STORAGE_INTERFACE =
			BLOCK_ENTITIES.register("storage_interface", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.StorageInterfaceBlockEntity::new, ModBlocks.STORAGE_INTERFACE.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.CreativeEnergyCellBlockEntity>> CREATIVE_ENERGY_CELL =
			BLOCK_ENTITIES.register("creative_energy_cell", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.CreativeEnergyCellBlockEntity::new, ModBlocks.CREATIVE_ENERGY_CELL.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity>> MOLECULAR_ASSEMBLER =
			BLOCK_ENTITIES.register("molecular_assembler", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity::new, ModBlocks.MOLECULAR_ASSEMBLER.get(), ModBlocks.ASSEMBLER_PANEL.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.PatternEncoderBlockEntity>> PATTERN_ENCODER =
			BLOCK_ENTITIES.register("pattern_encoder", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.PatternEncoderBlockEntity::new, ModBlocks.PATTERN_ENCODER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.StockKeeperBlockEntity>> STOCK_KEEPER =
			BLOCK_ENTITIES.register("stock_keeper", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.StockKeeperBlockEntity::new, ModBlocks.STOCK_KEEPER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.LightningCollectorBlockEntity>> LIGHTNING_COLLECTOR =
			BLOCK_ENTITIES.register("lightning_collector", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.LightningCollectorBlockEntity::new, ModBlocks.LIGHTNING_COLLECTOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.GeothermalGeneratorBlockEntity>> GEOTHERMAL_GENERATOR =
			BLOCK_ENTITIES.register("geothermal_generator", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.GeothermalGeneratorBlockEntity::new, ModBlocks.GEOTHERMAL_GENERATOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.WaterWheelBlockEntity>> WATER_WHEEL =
			BLOCK_ENTITIES.register("water_wheel", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.WaterWheelBlockEntity::new, ModBlocks.WATER_WHEEL.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.StormCallerBlockEntity>> STORM_CALLER =
			BLOCK_ENTITIES.register("storm_caller", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.StormCallerBlockEntity::new, ModBlocks.STORM_CALLER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.IsotopeSeparatorBlockEntity>> ISOTOPE_SEPARATOR =
			BLOCK_ENTITIES.register("isotope_separator", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.IsotopeSeparatorBlockEntity::new, ModBlocks.ISOTOPE_SEPARATOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.AirSeparatorBlockEntity>> AIR_SEPARATOR =
			BLOCK_ENTITIES.register("air_separator", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.AirSeparatorBlockEntity::new, ModBlocks.AIR_SEPARATOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.TritiumBreederBlockEntity>> TRITIUM_BREEDER =
			BLOCK_ENTITIES.register("tritium_breeder", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.TritiumBreederBlockEntity::new, ModBlocks.TRITIUM_BREEDER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.CryoInjectorBlockEntity>> CRYO_INJECTOR =
			BLOCK_ENTITIES.register("cryo_injector", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.CryoInjectorBlockEntity::new, ModBlocks.CRYO_INJECTOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.OxygenFurnaceBlockEntity>> OXYGEN_FURNACE =
			BLOCK_ENTITIES.register("oxygen_furnace", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.OxygenFurnaceBlockEntity::new, ModBlocks.OXYGEN_FURNACE.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.BioDigesterBlockEntity>> BIO_DIGESTER =
			BLOCK_ENTITIES.register("bio_digester", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.BioDigesterBlockEntity::new, ModBlocks.BIO_DIGESTER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.DiskWorkbenchBlockEntity>> DISK_WORKBENCH =
			BLOCK_ENTITIES.register("disk_workbench", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.DiskWorkbenchBlockEntity::new, ModBlocks.DISK_WORKBENCH.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.SecurityTerminalBlockEntity>> SECURITY_TERMINAL =
			BLOCK_ENTITIES.register("security_terminal", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.SecurityTerminalBlockEntity::new, ModBlocks.SECURITY_TERMINAL.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.ReactorGaugeBlockEntity>> REACTOR_GAUGE =
			BLOCK_ENTITIES.register("reactor_gauge", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.ReactorGaugeBlockEntity::new, ModBlocks.REACTOR_GAUGE.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.SaltEvaporatorBlockEntity>> SALT_EVAPORATOR =
			BLOCK_ENTITIES.register("salt_evaporator", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.SaltEvaporatorBlockEntity::new, ModBlocks.SALT_EVAPORATOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.BrineElectrolyzerBlockEntity>> BRINE_ELECTROLYZER =
			BLOCK_ENTITIES.register("brine_electrolyzer", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.BrineElectrolyzerBlockEntity::new, ModBlocks.BRINE_ELECTROLYZER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.ChemicalWasherBlockEntity>> CHEMICAL_WASHER =
			BLOCK_ENTITIES.register("chemical_washer", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.ChemicalWasherBlockEntity::new, ModBlocks.CHEMICAL_WASHER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.SmithingPressBlockEntity>> SMITHING_PRESS =
			BLOCK_ENTITIES.register("smithing_press", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.SmithingPressBlockEntity::new, ModBlocks.SMITHING_PRESS.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.PowerTransmitterBlockEntity>> POWER_TRANSMITTER =
			BLOCK_ENTITIES.register("power_transmitter", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.PowerTransmitterBlockEntity::new, ModBlocks.POWER_TRANSMITTER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.PowerReceiverBlockEntity>> POWER_RECEIVER =
			BLOCK_ENTITIES.register("power_receiver", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.PowerReceiverBlockEntity::new, ModBlocks.POWER_RECEIVER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.BlockBreakerBlockEntity>> BLOCK_BREAKER =
			BLOCK_ENTITIES.register("block_breaker", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.BlockBreakerBlockEntity::new, ModBlocks.BLOCK_BREAKER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.BlockPlacerBlockEntity>> BLOCK_PLACER =
			BLOCK_ENTITIES.register("block_placer", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.BlockPlacerBlockEntity::new, ModBlocks.BLOCK_PLACER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.TreeFarmBlockEntity>> TREE_FARM =
			BLOCK_ENTITIES.register("tree_farm", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.TreeFarmBlockEntity::new, ModBlocks.TREE_FARM.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.AnimalRanchBlockEntity>> ANIMAL_RANCH =
			BLOCK_ENTITIES.register("animal_ranch", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.AnimalRanchBlockEntity::new, ModBlocks.ANIMAL_RANCH.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.StorageMonitorBlockEntity>> STORAGE_MONITOR =
			BLOCK_ENTITIES.register("storage_monitor", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.StorageMonitorBlockEntity::new, ModBlocks.STORAGE_MONITOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.MatrixControllerBlockEntity>> MATRIX_CONTROLLER =
			BLOCK_ENTITIES.register("matrix_controller", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.MatrixControllerBlockEntity::new, ModBlocks.MATRIX_CONTROLLER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.PatternBankBlockEntity>> PATTERN_BANK =
			BLOCK_ENTITIES.register("pattern_bank", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.PatternBankBlockEntity::new, ModBlocks.PATTERN_BANK.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.ParticleColliderBlockEntity>> PARTICLE_COLLIDER =
			BLOCK_ENTITIES.register("particle_collider", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.ParticleColliderBlockEntity::new, ModBlocks.PARTICLE_COLLIDER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.AntimatterReactorBlockEntity>> ANTIMATTER_REACTOR =
			BLOCK_ENTITIES.register("antimatter_reactor", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.AntimatterReactorBlockEntity::new, ModBlocks.ANTIMATTER_REACTOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.WirelessSenderBlockEntity>> WIRELESS_SENDER =
			BLOCK_ENTITIES.register("wireless_sender", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.WirelessSenderBlockEntity::new, ModBlocks.WIRELESS_SENDER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.WirelessReceiverBlockEntity>> WIRELESS_RECEIVER =
			BLOCK_ENTITIES.register("wireless_receiver", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.WirelessReceiverBlockEntity::new, ModBlocks.WIRELESS_RECEIVER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.BufferBlockEntity>> BUFFER =
			BLOCK_ENTITIES.register("buffer", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.BufferBlockEntity::new, ModBlocks.ITEM_BUFFER.get(), ModBlocks.FLUID_BUFFER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.CryogenicCondenserBlockEntity>> CRYOGENIC_CONDENSER =
			BLOCK_ENTITIES.register("cryogenic_condenser", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.CryogenicCondenserBlockEntity::new, ModBlocks.CRYOGENIC_CONDENSER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.FuelRefineryBlockEntity>> FUEL_REFINERY =
			BLOCK_ENTITIES.register("fuel_refinery", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.FuelRefineryBlockEntity::new, ModBlocks.FUEL_REFINERY.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.LaunchControllerBlockEntity>> LAUNCH_CONTROLLER =
			BLOCK_ENTITIES.register("launch_controller", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.LaunchControllerBlockEntity::new, ModBlocks.LAUNCH_CONTROLLER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.ReceiverDishBlockEntity>> RECEIVER_DISH =
			BLOCK_ENTITIES.register("receiver_dish", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.ReceiverDishBlockEntity::new, ModBlocks.RECEIVER_DISH.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.LaserDrillBlockEntity>> LASER_DRILL =
			BLOCK_ENTITIES.register("laser_drill", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.LaserDrillBlockEntity::new, ModBlocks.LASER_DRILL.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.BrewingMachineBlockEntity>> BREWING_MACHINE =
			BLOCK_ENTITIES.register("brewing_machine", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.BrewingMachineBlockEntity::new, ModBlocks.BREWING_MACHINE.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.FluidicPlenisherBlockEntity>> FLUIDIC_PLENISHER =
			BLOCK_ENTITIES.register("fluidic_plenisher", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.FluidicPlenisherBlockEntity::new, ModBlocks.FLUIDIC_PLENISHER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.greenhouse.GreenhouseBlockEntity>> GREENHOUSE =
			BLOCK_ENTITIES.register("greenhouse", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.greenhouse.GreenhouseBlockEntity::new, ModBlocks.SPRINKLER.get(), ModBlocks.GROW_LAMP.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.DigitalMinerBlockEntity>> DIGITAL_MINER =
			BLOCK_ENTITIES.register("digital_miner", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.DigitalMinerBlockEntity::new, ModBlocks.DIGITAL_MINER.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.industry.HeavyFurnaceBlockEntity>> HEAVY_FURNACE =
			BLOCK_ENTITIES.register("heavy_furnace", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.industry.HeavyFurnaceBlockEntity::new, ModBlocks.COKE_OVEN.get(), ModBlocks.INDUSTRIAL_BLAST_FURNACE.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.block.entity.GasTankBlockEntity>> GAS_TANK =
			BLOCK_ENTITIES.register("gas_tank", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.block.entity.GasTankBlockEntity::new, ModBlocks.GAS_TANK.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.industry.BioRefineryBlockEntity>> BIO_REFINERY =
			BLOCK_ENTITIES.register("bio_refinery", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.industry.BioRefineryBlockEntity::new, ModBlocks.BIO_REFINERY.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.industry.DieselGeneratorBlockEntity>> DIESEL_GENERATOR =
			BLOCK_ENTITIES.register("diesel_generator", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.industry.DieselGeneratorBlockEntity::new, ModBlocks.DIESEL_GENERATOR.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.core.EnergyCoreBlockEntity>> ENERGY_CORE =
			BLOCK_ENTITIES.register("energy_core", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.core.EnergyCoreBlockEntity::new, ModBlocks.ENERGY_CORE.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.core.EnergyPylonBlockEntity>> ENERGY_PYLON =
			BLOCK_ENTITIES.register("energy_pylon", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.core.EnergyPylonBlockEntity::new, ModBlocks.INPUT_PYLON.get(), ModBlocks.OUTPUT_PYLON.get()).build(null));

	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.stargate.StargateDialerBlockEntity>> STARGATE_DIALER =
			BLOCK_ENTITIES.register("stargate_dialer", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.stargate.StargateDialerBlockEntity::new, ModBlocks.STARGATE_DIALER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.quantum.QuantumChestBlockEntity>> QUANTUM_CHEST =
			BLOCK_ENTITIES.register("quantum_chest", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.quantum.QuantumChestBlockEntity::new, ModBlocks.QUANTUM_CHEST.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.stargate.StargateRingBlockEntity>> STARGATE_RING =
			BLOCK_ENTITIES.register("stargate_ring", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.stargate.StargateRingBlockEntity::new, ModBlocks.STARGATE_FRAME.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.stargate.EventHorizonBlockEntity>> EVENT_HORIZON =
			BLOCK_ENTITIES.register("event_horizon", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.stargate.EventHorizonBlockEntity::new, ModBlocks.EVENT_HORIZON.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.spatial.SpatialProjectorBlockEntity>> SPATIAL_PROJECTOR =
			BLOCK_ENTITIES.register("spatial_projector", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.spatial.SpatialProjectorBlockEntity::new, ModBlocks.SPATIAL_PROJECTOR.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.turbine.TurbineControllerBlockEntity>> TURBINE_CONTROLLER =
			BLOCK_ENTITIES.register("turbine_controller", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.turbine.TurbineControllerBlockEntity::new, ModBlocks.TURBINE_CONTROLLER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.turbine.TurbineValveBlockEntity>> TURBINE_VALVE =
			BLOCK_ENTITIES.register("turbine_valve", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.turbine.TurbineValveBlockEntity::new, ModBlocks.TURBINE_VALVE.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.drone.DroneStationBlockEntity>> DRONE_STATION =
			BLOCK_ENTITIES.register("drone_station", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.drone.DroneStationBlockEntity::new, ModBlocks.DRONE_STATION.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.conveyor.ConveyorBlockEntity>> CONVEYOR =
			BLOCK_ENTITIES.register("conveyor", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.conveyor.ConveyorBlockEntity::new, ModBlocks.conveyorBlocks()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.factory.DigitalFactoryBlockEntity>> DIGITAL_FACTORY =
			BLOCK_ENTITIES.register("digital_factory", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.factory.DigitalFactoryBlockEntity::new, ModBlocks.DIGITAL_FACTORY.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.factory.FactoryPortBlockEntity>> FACTORY_PORT =
			BLOCK_ENTITIES.register("factory_port", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.factory.FactoryPortBlockEntity::new, ModBlocks.FACTORY_PORT.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.redstone.WirelessRedstoneBlockEntity>> WIRELESS_REDSTONE =
			BLOCK_ENTITIES.register("wireless_redstone", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.redstone.WirelessRedstoneBlockEntity::new, ModBlocks.REDSTONE_TRANSMITTER.get(), ModBlocks.REDSTONE_RECEIVER.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.logic.MachineStatusDisplayBlockEntity>> MACHINE_STATUS_DISPLAY =
			BLOCK_ENTITIES.register("machine_status_display", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.logic.MachineStatusDisplayBlockEntity::new, ModBlocks.MACHINE_STATUS_DISPLAY.get()).build(null));
	public static final RegistryObject<BlockEntityType<com.robvanblerk.tieredpower.logic.LogicControllerBlockEntity>> LOGIC_CONTROLLER =
			BLOCK_ENTITIES.register("logic_controller", () -> BlockEntityType.Builder.of(com.robvanblerk.tieredpower.logic.LogicControllerBlockEntity::new, ModBlocks.LOGIC_CONTROLLER.get()).build(null));

	/** One block entity type shared by every cable tier; each cable reads its tier from its block. */
	public static final RegistryObject<BlockEntityType<CableBlockEntity>> CABLE =
			BLOCK_ENTITIES.register("cable", () -> BlockEntityType.Builder
					.of(CableBlockEntity::new, ModBlocks.cableBlocks()).build(null));

	private ModBlockEntities() {}
}
