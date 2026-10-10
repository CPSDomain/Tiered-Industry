package com.robvanblerk.tieredpower.registry;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.menu.BatteryBoxMenu;
import com.robvanblerk.tieredpower.menu.BoilerMenu;
import com.robvanblerk.tieredpower.menu.ElectricPumpMenu;
import com.robvanblerk.tieredpower.menu.FreezerMenu;
import com.robvanblerk.tieredpower.menu.ProcessingMachineMenu;
import com.robvanblerk.tieredpower.menu.QuarryMenu;
import com.robvanblerk.tieredpower.menu.ItemFilterMenu;
import com.robvanblerk.tieredpower.menu.CropFarmerMenu;
import com.robvanblerk.tieredpower.menu.AutoCrafterMenu;
import com.robvanblerk.tieredpower.menu.ChunkLoaderMenu;
import com.robvanblerk.tieredpower.menu.WirelessChargerMenu;
import com.robvanblerk.tieredpower.menu.WindTurbineMenu;
import com.robvanblerk.tieredpower.menu.FissionReactorMenu;
import com.robvanblerk.tieredpower.menu.FissionControllerMenu;
import com.robvanblerk.tieredpower.menu.GasElectrolyzerMenu;
import com.robvanblerk.tieredpower.menu.GasBurnerGeneratorMenu;
import com.robvanblerk.tieredpower.menu.SteamEngineMenu;
import com.robvanblerk.tieredpower.menu.SteamHammerMenu;
import com.robvanblerk.tieredpower.menu.EnchantingMachineMenu;
import com.robvanblerk.tieredpower.menu.MobGrinderMenu;
import com.robvanblerk.tieredpower.menu.SpawnerControllerMenu;
import com.robvanblerk.tieredpower.menu.FluidMixerMenu;
import com.robvanblerk.tieredpower.menu.BankControllerMenu;
import com.robvanblerk.tieredpower.menu.FusionControllerMenu;
import com.robvanblerk.tieredpower.menu.AlloySmelterMenu;
import com.robvanblerk.tieredpower.menu.ChargerMenu;
import com.robvanblerk.tieredpower.menu.PulverizerMenu;
import com.robvanblerk.tieredpower.menu.ElectrolyzerMenu;
import com.robvanblerk.tieredpower.menu.FusionReactorMenu;
import com.robvanblerk.tieredpower.menu.SteamTurbineMenu;
import com.robvanblerk.tieredpower.menu.CoalGeneratorMenu;
import com.robvanblerk.tieredpower.menu.ElectricFurnaceMenu;

/** Menus are the server side of a GUI: which slots exist and what numbers get synced to the player. */
public final class ModMenus {
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TieredPower.MOD_ID);

	public static final RegistryObject<MenuType<CoalGeneratorMenu>> COAL_GENERATOR =
			MENUS.register("coal_generator", () -> new MenuType<CoalGeneratorMenu>(CoalGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<BatteryBoxMenu>> BATTERY_BOX =
			MENUS.register("battery_box", () -> new MenuType<BatteryBoxMenu>(BatteryBoxMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE =
			MENUS.register("electric_furnace", () -> new MenuType<ElectricFurnaceMenu>(ElectricFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<BoilerMenu>> BOILER =
			MENUS.register("boiler", () -> new MenuType<BoilerMenu>(BoilerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<SteamTurbineMenu>> STEAM_TURBINE =
			MENUS.register("steam_turbine", () -> new MenuType<SteamTurbineMenu>(SteamTurbineMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<ElectrolyzerMenu>> ELECTROLYZER =
			MENUS.register("electrolyzer", () -> new MenuType<ElectrolyzerMenu>(ElectrolyzerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<FusionReactorMenu>> FUSION_REACTOR =
			MENUS.register("fusion_reactor", () -> new MenuType<FusionReactorMenu>(FusionReactorMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<PulverizerMenu>> PULVERIZER =
			MENUS.register("pulverizer", () -> new MenuType<PulverizerMenu>(PulverizerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<AlloySmelterMenu>> ALLOY_SMELTER =
			MENUS.register("alloy_smelter", () -> new MenuType<AlloySmelterMenu>(AlloySmelterMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<ChargerMenu>> CHARGER =
			MENUS.register("charger", () -> new MenuType<ChargerMenu>(ChargerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<FusionControllerMenu>> FUSION_CONTROLLER =
			MENUS.register("fusion_controller", () -> new MenuType<FusionControllerMenu>(FusionControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<BankControllerMenu>> BANK_CONTROLLER =
			MENUS.register("bank_controller", () -> new MenuType<BankControllerMenu>(BankControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<ElectricPumpMenu>> ELECTRIC_PUMP =
			MENUS.register("electric_pump", () -> new MenuType<ElectricPumpMenu>(ElectricPumpMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<ProcessingMachineMenu>> ROCK_CRUSHER =
			MENUS.register("rock_crusher", () -> new MenuType<ProcessingMachineMenu>(
					(id, inv) -> new ProcessingMachineMenu(ModMenus.ROCK_CRUSHER.get(), id, inv, false), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<FreezerMenu>> FREEZER =
			MENUS.register("freezer", () -> new MenuType<FreezerMenu>(FreezerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<ProcessingMachineMenu>> ORE_PURIFIER =
			MENUS.register("ore_purifier", () -> new MenuType<ProcessingMachineMenu>(
					(id, inv) -> new ProcessingMachineMenu(ModMenus.ORE_PURIFIER.get(), id, inv, true), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<ProcessingMachineMenu>> COMPRESSOR =
			MENUS.register("compressor", () -> new MenuType<ProcessingMachineMenu>(
					(id, inv) -> new ProcessingMachineMenu(ModMenus.COMPRESSOR.get(), id, inv, false), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<ProcessingMachineMenu>> ELECTRIC_SAWMILL =
			MENUS.register("electric_sawmill", () -> new MenuType<ProcessingMachineMenu>(
					(id, inv) -> new ProcessingMachineMenu(ModMenus.ELECTRIC_SAWMILL.get(), id, inv, false), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<QuarryMenu>> QUARRY =
			MENUS.register("quarry", () -> new MenuType<QuarryMenu>(QuarryMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<CropFarmerMenu>> CROP_FARMER =
			MENUS.register("crop_farmer", () -> new MenuType<CropFarmerMenu>(CropFarmerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<AutoCrafterMenu>> AUTO_CRAFTER =
			MENUS.register("auto_crafter", () -> new MenuType<AutoCrafterMenu>(AutoCrafterMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.stargate.StargateDialerMenu>> STARGATE_DIALER =
			MENUS.register("stargate_dialer", () -> new MenuType<com.robvanblerk.tieredpower.stargate.StargateDialerMenu>(com.robvanblerk.tieredpower.stargate.StargateDialerMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.spatial.SpatialProjectorMenu>> SPATIAL_PROJECTOR =
			MENUS.register("spatial_projector", () -> new MenuType<com.robvanblerk.tieredpower.spatial.SpatialProjectorMenu>(com.robvanblerk.tieredpower.spatial.SpatialProjectorMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.turbine.TurbineControllerMenu>> TURBINE_CONTROLLER =
			MENUS.register("turbine_controller", () -> new MenuType<com.robvanblerk.tieredpower.turbine.TurbineControllerMenu>(com.robvanblerk.tieredpower.turbine.TurbineControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.drone.DroneStationMenu>> DRONE_STATION =
			MENUS.register("drone_station", () -> new MenuType<com.robvanblerk.tieredpower.drone.DroneStationMenu>(com.robvanblerk.tieredpower.drone.DroneStationMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<ChunkLoaderMenu>> CHUNK_LOADER =
			MENUS.register("chunk_loader", () -> new MenuType<ChunkLoaderMenu>(ChunkLoaderMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.quantum.QuantumChestMenu>> QUANTUM_CHEST =
			MENUS.register("quantum_chest", () -> new MenuType<com.robvanblerk.tieredpower.quantum.QuantumChestMenu>(com.robvanblerk.tieredpower.quantum.QuantumChestMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<ItemFilterMenu>> ITEM_FILTER =
			MENUS.register("item_filter", () -> new MenuType<ItemFilterMenu>(ItemFilterMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<WirelessChargerMenu>> WIRELESS_CHARGER =
			MENUS.register("wireless_charger", () -> new MenuType<WirelessChargerMenu>(WirelessChargerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<WindTurbineMenu>> WIND_TURBINE =
			MENUS.register("wind_turbine", () -> new MenuType<WindTurbineMenu>(WindTurbineMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<FissionReactorMenu>> FISSION_REACTOR =
			MENUS.register("fission_reactor", () -> new MenuType<FissionReactorMenu>(FissionReactorMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<FissionControllerMenu>> FISSION_CONTROLLER =
			MENUS.register("fission_controller", () -> new MenuType<FissionControllerMenu>(FissionControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<EnchantingMachineMenu>> ENCHANTING_MACHINE =
			MENUS.register("enchanting_machine", () -> new MenuType<EnchantingMachineMenu>(EnchantingMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<MobGrinderMenu>> MOB_GRINDER =
			MENUS.register("mob_grinder", () -> new MenuType<MobGrinderMenu>(MobGrinderMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<SpawnerControllerMenu>> SPAWNER_CONTROLLER =
			MENUS.register("spawner_controller", () -> new MenuType<SpawnerControllerMenu>(SpawnerControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<FluidMixerMenu>> FLUID_MIXER =
			MENUS.register("fluid_mixer", () -> new MenuType<FluidMixerMenu>(FluidMixerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<GasElectrolyzerMenu>> GAS_ELECTROLYZER =
			MENUS.register("gas_electrolyzer", () -> new MenuType<GasElectrolyzerMenu>(GasElectrolyzerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<GasBurnerGeneratorMenu>> GAS_BURNER_GENERATOR =
			MENUS.register("gas_burner_generator", () -> new MenuType<GasBurnerGeneratorMenu>(GasBurnerGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<SteamEngineMenu>> STEAM_ENGINE =
			MENUS.register("steam_engine", () -> new MenuType<SteamEngineMenu>(SteamEngineMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<SteamHammerMenu>> STEAM_HAMMER =
			MENUS.register("steam_hammer", () -> new MenuType<SteamHammerMenu>(SteamHammerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.DriveBayMenu>> DRIVE_BAY =
			MENUS.register("drive_bay", () -> new MenuType<com.robvanblerk.tieredpower.menu.DriveBayMenu>(com.robvanblerk.tieredpower.menu.DriveBayMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.StorageTerminalMenu>> STORAGE_TERMINAL =
			MENUS.register("storage_terminal", () -> new MenuType<com.robvanblerk.tieredpower.menu.StorageTerminalMenu>(com.robvanblerk.tieredpower.menu.StorageTerminalMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.CraftingTerminalMenu>> CRAFTING_TERMINAL =
			MENUS.register("crafting_terminal", () -> new MenuType<com.robvanblerk.tieredpower.menu.CraftingTerminalMenu>(com.robvanblerk.tieredpower.menu.CraftingTerminalMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.StorageBusMenu>> STORAGE_BUS =
			MENUS.register("storage_bus", () -> new MenuType<com.robvanblerk.tieredpower.menu.StorageBusMenu>(com.robvanblerk.tieredpower.menu.StorageBusMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.MolecularAssemblerMenu>> MOLECULAR_ASSEMBLER =
			MENUS.register("molecular_assembler", () -> new MenuType<com.robvanblerk.tieredpower.menu.MolecularAssemblerMenu>(com.robvanblerk.tieredpower.menu.MolecularAssemblerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.PatternEncoderMenu>> PATTERN_ENCODER =
			MENUS.register("pattern_encoder", () -> new MenuType<com.robvanblerk.tieredpower.menu.PatternEncoderMenu>(com.robvanblerk.tieredpower.menu.PatternEncoderMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.StockKeeperMenu>> STOCK_KEEPER =
			MENUS.register("stock_keeper", () -> new MenuType<com.robvanblerk.tieredpower.menu.StockKeeperMenu>(com.robvanblerk.tieredpower.menu.StockKeeperMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.StormCallerMenu>> STORM_CALLER =
			MENUS.register("storm_caller", () -> new MenuType<com.robvanblerk.tieredpower.menu.StormCallerMenu>(com.robvanblerk.tieredpower.menu.StormCallerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.IsotopeSeparatorMenu>> ISOTOPE_SEPARATOR =
			MENUS.register("isotope_separator", () -> new MenuType<com.robvanblerk.tieredpower.menu.IsotopeSeparatorMenu>(com.robvanblerk.tieredpower.menu.IsotopeSeparatorMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.AirSeparatorMenu>> AIR_SEPARATOR =
			MENUS.register("air_separator", () -> new MenuType<com.robvanblerk.tieredpower.menu.AirSeparatorMenu>(com.robvanblerk.tieredpower.menu.AirSeparatorMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.TritiumBreederMenu>> TRITIUM_BREEDER =
			MENUS.register("tritium_breeder", () -> new MenuType<com.robvanblerk.tieredpower.menu.TritiumBreederMenu>(com.robvanblerk.tieredpower.menu.TritiumBreederMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.OxygenFurnaceMenu>> OXYGEN_FURNACE =
			MENUS.register("oxygen_furnace", () -> new MenuType<com.robvanblerk.tieredpower.menu.OxygenFurnaceMenu>(com.robvanblerk.tieredpower.menu.OxygenFurnaceMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.BioDigesterMenu>> BIO_DIGESTER =
			MENUS.register("bio_digester", () -> new MenuType<com.robvanblerk.tieredpower.menu.BioDigesterMenu>(com.robvanblerk.tieredpower.menu.BioDigesterMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.DiskWorkbenchMenu>> DISK_WORKBENCH =
			MENUS.register("disk_workbench", () -> new MenuType<com.robvanblerk.tieredpower.menu.DiskWorkbenchMenu>(com.robvanblerk.tieredpower.menu.DiskWorkbenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.SecurityTerminalMenu>> SECURITY_TERMINAL =
			MENUS.register("security_terminal", () -> new MenuType<com.robvanblerk.tieredpower.menu.SecurityTerminalMenu>(com.robvanblerk.tieredpower.menu.SecurityTerminalMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.SaltEvaporatorMenu>> SALT_EVAPORATOR =
			MENUS.register("salt_evaporator", () -> new MenuType<com.robvanblerk.tieredpower.menu.SaltEvaporatorMenu>(com.robvanblerk.tieredpower.menu.SaltEvaporatorMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.BrineElectrolyzerMenu>> BRINE_ELECTROLYZER =
			MENUS.register("brine_electrolyzer", () -> new MenuType<com.robvanblerk.tieredpower.menu.BrineElectrolyzerMenu>(com.robvanblerk.tieredpower.menu.BrineElectrolyzerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.ChemicalWasherMenu>> CHEMICAL_WASHER =
			MENUS.register("chemical_washer", () -> new MenuType<com.robvanblerk.tieredpower.menu.ChemicalWasherMenu>(com.robvanblerk.tieredpower.menu.ChemicalWasherMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.SmithingPressMenu>> SMITHING_PRESS =
			MENUS.register("smithing_press", () -> new MenuType<com.robvanblerk.tieredpower.menu.SmithingPressMenu>(com.robvanblerk.tieredpower.menu.SmithingPressMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>> BLOCK_BREAKER =
			MENUS.register("block_breaker", () -> new MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>((id, inv) -> new com.robvanblerk.tieredpower.menu.WorkerMenu(ModMenus.BLOCK_BREAKER.get(), id, inv), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>> BLOCK_PLACER =
			MENUS.register("block_placer", () -> new MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>((id, inv) -> new com.robvanblerk.tieredpower.menu.WorkerMenu(ModMenus.BLOCK_PLACER.get(), id, inv), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>> TREE_FARM =
			MENUS.register("tree_farm", () -> new MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>((id, inv) -> new com.robvanblerk.tieredpower.menu.WorkerMenu(ModMenus.TREE_FARM.get(), id, inv), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>> ANIMAL_RANCH =
			MENUS.register("animal_ranch", () -> new MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>((id, inv) -> new com.robvanblerk.tieredpower.menu.WorkerMenu(ModMenus.ANIMAL_RANCH.get(), id, inv), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.MatrixControllerMenu>> MATRIX_CONTROLLER =
			MENUS.register("matrix_controller", () -> new MenuType<com.robvanblerk.tieredpower.menu.MatrixControllerMenu>(com.robvanblerk.tieredpower.menu.MatrixControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.ParticleColliderMenu>> PARTICLE_COLLIDER =
			MENUS.register("particle_collider", () -> new MenuType<com.robvanblerk.tieredpower.menu.ParticleColliderMenu>(com.robvanblerk.tieredpower.menu.ParticleColliderMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.FluidFilterMenu>> FLUID_FILTER =
			MENUS.register("fluid_filter", () -> new MenuType<com.robvanblerk.tieredpower.menu.FluidFilterMenu>(com.robvanblerk.tieredpower.menu.FluidFilterMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.RocketFuelMenu>> CRYOGENIC_CONDENSER =
			MENUS.register("cryogenic_condenser", () -> new MenuType<com.robvanblerk.tieredpower.menu.RocketFuelMenu>((id, inv) -> new com.robvanblerk.tieredpower.menu.RocketFuelMenu(ModMenus.CRYOGENIC_CONDENSER.get(), id, inv, false), FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.RocketFuelMenu>> FUEL_REFINERY =
			MENUS.register("fuel_refinery", () -> new MenuType<com.robvanblerk.tieredpower.menu.RocketFuelMenu>((id, inv) -> new com.robvanblerk.tieredpower.menu.RocketFuelMenu(ModMenus.FUEL_REFINERY.get(), id, inv, true), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.LaunchControllerMenu>> LAUNCH_CONTROLLER =
			MENUS.register("launch_controller", () -> new MenuType<com.robvanblerk.tieredpower.menu.LaunchControllerMenu>(com.robvanblerk.tieredpower.menu.LaunchControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>> LASER_DRILL =
			MENUS.register("laser_drill", () -> new MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>((id, inv) -> new com.robvanblerk.tieredpower.menu.WorkerMenu(ModMenus.LASER_DRILL.get(), id, inv), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.BrewingMachineMenu>> BREWING_MACHINE =
			MENUS.register("brewing_machine", () -> new MenuType<com.robvanblerk.tieredpower.menu.BrewingMachineMenu>(com.robvanblerk.tieredpower.menu.BrewingMachineMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>> DIGITAL_MINER =
			MENUS.register("digital_miner", () -> new MenuType<com.robvanblerk.tieredpower.menu.WorkerMenu>((id, inv) -> new com.robvanblerk.tieredpower.menu.WorkerMenu(ModMenus.DIGITAL_MINER.get(), id, inv), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.industry.HeavyFurnaceMenu>> COKE_OVEN =
			MENUS.register("coke_oven", () -> new MenuType<com.robvanblerk.tieredpower.industry.HeavyFurnaceMenu>((id, inv) -> new com.robvanblerk.tieredpower.industry.HeavyFurnaceMenu(ModMenus.COKE_OVEN.get(), id, inv, true), FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.industry.HeavyFurnaceMenu>> INDUSTRIAL_BLAST_FURNACE =
			MENUS.register("industrial_blast_furnace", () -> new MenuType<com.robvanblerk.tieredpower.industry.HeavyFurnaceMenu>((id, inv) -> new com.robvanblerk.tieredpower.industry.HeavyFurnaceMenu(ModMenus.INDUSTRIAL_BLAST_FURNACE.get(), id, inv, false), FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.industry.BioRefineryMenu>> BIO_REFINERY =
			MENUS.register("bio_refinery", () -> new MenuType<com.robvanblerk.tieredpower.industry.BioRefineryMenu>(com.robvanblerk.tieredpower.industry.BioRefineryMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.factory.DigitalFactoryMenu>> DIGITAL_FACTORY =
			MENUS.register("digital_factory", () -> new MenuType<com.robvanblerk.tieredpower.factory.DigitalFactoryMenu>(com.robvanblerk.tieredpower.factory.DigitalFactoryMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.logic.MachineStatusMenu>> MACHINE_STATUS =
			MENUS.register("machine_status", () -> new MenuType<com.robvanblerk.tieredpower.logic.MachineStatusMenu>(com.robvanblerk.tieredpower.logic.MachineStatusMenu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final RegistryObject<MenuType<com.robvanblerk.tieredpower.logic.LogicControllerMenu>> LOGIC_CONTROLLER =
			MENUS.register("logic_controller", () -> new MenuType<com.robvanblerk.tieredpower.logic.LogicControllerMenu>(com.robvanblerk.tieredpower.logic.LogicControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));

	private ModMenus() {}
}
