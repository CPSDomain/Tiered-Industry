package com.robvanblerk.tieredpower.registry;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.BatteryBoxBlock;
import com.robvanblerk.tieredpower.block.BoilerBlock;
import com.robvanblerk.tieredpower.block.BankControllerBlock;
import com.robvanblerk.tieredpower.block.BankPortBlock;
import com.robvanblerk.tieredpower.block.EnergyCellBlock;
import com.robvanblerk.tieredpower.block.FusionControllerBlock;
import com.robvanblerk.tieredpower.block.ReactorPortBlock;
import net.minecraft.world.level.block.GlassBlock;
import com.robvanblerk.tieredpower.block.AlloySmelterBlock;
import com.robvanblerk.tieredpower.block.ChargerBlock;
import com.robvanblerk.tieredpower.block.PulverizerBlock;
import com.robvanblerk.tieredpower.block.ElectrolyzerBlock;
import com.robvanblerk.tieredpower.block.FusionReactorBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.item.Rarity;
import com.robvanblerk.tieredpower.block.SteamTurbineBlock;
import com.robvanblerk.tieredpower.block.CableBlock;
import com.robvanblerk.tieredpower.block.CoalGeneratorBlock;
import com.robvanblerk.tieredpower.block.ElectricFurnaceBlock;
import com.robvanblerk.tieredpower.energy.BatteryTier;
import com.robvanblerk.tieredpower.energy.CableTier;
import com.robvanblerk.tieredpower.item.UpgradeItem;
import com.robvanblerk.tieredpower.item.WrenchItem;
import com.robvanblerk.tieredpower.block.SinkBlock;
import com.robvanblerk.tieredpower.block.FluidPipeBlock;
import com.robvanblerk.tieredpower.block.ItemPipeBlock;
import com.robvanblerk.tieredpower.energy.ItemPipeTier;
import com.robvanblerk.tieredpower.item.ItemFilterItem;
import com.robvanblerk.tieredpower.energy.PipeTier;
import com.robvanblerk.tieredpower.block.ElectricPumpBlock;
import com.robvanblerk.tieredpower.block.FluidTankBlock;
import com.robvanblerk.tieredpower.block.FreezerBlock;
import com.robvanblerk.tieredpower.block.RockCrusherBlock;
import com.robvanblerk.tieredpower.block.QuarryBlock;
import com.robvanblerk.tieredpower.block.CropFarmerBlock;
import com.robvanblerk.tieredpower.block.AutoCrafterBlock;
import com.robvanblerk.tieredpower.block.ChunkLoaderBlock;
import com.robvanblerk.tieredpower.block.OrePurifierBlock;
import com.robvanblerk.tieredpower.block.CompressorBlock;
import com.robvanblerk.tieredpower.block.ElectricSawmillBlock;
import com.robvanblerk.tieredpower.item.FuelItem;
import com.robvanblerk.tieredpower.item.powered.ElectricChainsawItem;
import com.robvanblerk.tieredpower.item.powered.ElectricDrillItem;
import com.robvanblerk.tieredpower.item.powered.JetpackItem;
import com.robvanblerk.tieredpower.item.powered.JetpackMaterial;
import com.robvanblerk.tieredpower.item.powered.PortableBatteryItem;
import com.robvanblerk.tieredpower.item.powered.PoweredTier;
import com.robvanblerk.tieredpower.block.WirelessChargerBlock;
import com.robvanblerk.tieredpower.block.SolarPanelBlock;
import com.robvanblerk.tieredpower.block.WindTurbineBlock;
import com.robvanblerk.tieredpower.block.FissionReactorBlock;
import com.robvanblerk.tieredpower.block.FissionControllerBlock;
import com.robvanblerk.tieredpower.block.FissionPortBlock;
import com.robvanblerk.tieredpower.block.FuelAssemblyBlock;
import com.robvanblerk.tieredpower.block.EnchantingMachineBlock;
import com.robvanblerk.tieredpower.block.MobGrinderBlock;
import com.robvanblerk.tieredpower.block.SpawnerControllerBlock;
import com.robvanblerk.tieredpower.block.FluidMixerBlock;
import com.robvanblerk.tieredpower.block.TeleporterBlock;
import com.robvanblerk.tieredpower.block.CondenserBlock;
import com.robvanblerk.tieredpower.block.GasElectrolyzerBlock;
import com.robvanblerk.tieredpower.block.GasBurnerGeneratorBlock;
import com.robvanblerk.tieredpower.block.SteamEngineBlock;
import com.robvanblerk.tieredpower.block.SteamHammerBlock;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;

/** Registers every block, its matching item, and the creative tab. */
public final class ModBlocks {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TieredPower.MOD_ID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TieredPower.MOD_ID);
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TieredPower.MOD_ID);

	private static BlockBehaviour.Properties machine() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f)
				.requiresCorrectToolForDrops().sound(SoundType.METAL)
				.isValidSpawn((s, l, p, e) -> false); // no mobs on machines or multiblock walls
	}

	public static final RegistryObject<Block> COAL_GENERATOR = register("coal_generator", () -> new CoalGeneratorBlock(machine()));
	public static final RegistryObject<Block> BATTERY_BOX = register("battery_box", () -> new BatteryBoxBlock(BatteryTier.BASIC, machine()));
	public static final RegistryObject<Block> ADVANCED_BATTERY_BOX = register("advanced_battery_box", () -> new BatteryBoxBlock(BatteryTier.ADVANCED, machine()));
	public static final RegistryObject<Block> ELITE_BATTERY_BOX = register("elite_battery_box", () -> new BatteryBoxBlock(BatteryTier.ELITE, machine()));
	public static final RegistryObject<Block> ULTIMATE_BATTERY_BOX = register("ultimate_battery_box", () -> new BatteryBoxBlock(BatteryTier.ULTIMATE, machine()));
	public static final RegistryObject<Block> QUANTUM_BATTERY_BOX = register("quantum_battery_box", () -> new BatteryBoxBlock(BatteryTier.QUANTUM, machine()));
	public static final RegistryObject<Block> ELECTRIC_FURNACE = register("electric_furnace", () -> new ElectricFurnaceBlock(machine()));

	// ---- Stage 2: steam ----
	public static final RegistryObject<Block> BOILER = register("boiler", () -> new BoilerBlock(machine()));
	public static final RegistryObject<Block> STEAM_TURBINE = register("steam_turbine", () -> new SteamTurbineBlock(machine()));
	public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));

	// ---- Stage 3: fusion ----
	public static final RegistryObject<Block> LITHIUM_ORE = register("lithium_ore", () -> new DropExperienceBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f).requiresCorrectToolForDrops(), UniformInt.of(1, 3)));
	public static final RegistryObject<Block> DEEPSLATE_LITHIUM_ORE = register("deepslate_lithium_ore", () -> new DropExperienceBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops()
					.sound(SoundType.DEEPSLATE), UniformInt.of(1, 3)));
	public static final RegistryObject<Item> RAW_LITHIUM = ITEMS.register("raw_lithium", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> LITHIUM_INGOT = ITEMS.register("lithium_ingot", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> EMPTY_FUEL_CELL = ITEMS.register("empty_fuel_cell", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> DEUTERIUM_CELL = ITEMS.register("deuterium_cell", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> TRITIUM_CELL = ITEMS.register("tritium_cell", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Block> ELECTROLYZER = register("electrolyzer", () -> new ElectrolyzerBlock(machine()));
	public static final RegistryObject<Block> FUSION_REACTOR = register("fusion_reactor", () -> new FusionReactorBlock(
			machine().strength(10.0f, 1200.0f).lightLevel(state -> state.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 15 : 0)));

	// ---- Machines that use power ----
	public static final RegistryObject<Block> PULVERIZER = register("pulverizer", () -> new PulverizerBlock(machine()));
	public static final RegistryObject<Block> ALLOY_SMELTER = register("alloy_smelter", () -> new AlloySmelterBlock(machine()));
	public static final RegistryObject<Block> CHARGER = register("charger", () -> new ChargerBlock(machine()));
	public static final RegistryObject<Item> IRON_DUST = ITEMS.register("iron_dust", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> GOLD_DUST = ITEMS.register("gold_dust", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COPPER_DUST = ITEMS.register("copper_dust", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> LITHIUM_DUST = ITEMS.register("lithium_dust", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SUPERCONDUCTOR_INGOT = ITEMS.register("superconductor_ingot", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
	/** Tier 5 material: made in the Alloy Smelter from netherite and superconductor. */
	public static final RegistryObject<Item> QUANTUM_ALLOY_INGOT = ITEMS.register("quantum_alloy_ingot", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));

	// ---- Upgrades ----
	public static final RegistryObject<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade",
			() -> new UpgradeItem(UpgradeItem.Kind.SPEED, new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade",
			() -> new UpgradeItem(UpgradeItem.Kind.EFFICIENCY, new Item.Properties().stacksTo(16)));

	// ---- Multiblock Fusion Reactor ----
	private static BlockBehaviour.Properties reactor() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(8.0f, 1200.0f)
				.requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)
				.isValidSpawn((s, l, p, e) -> false);
	}

	public static final RegistryObject<Block> REACTOR_CASING = register("reactor_casing", () -> new Block(reactor()));
	public static final RegistryObject<Block> REACTOR_GLASS = register("reactor_glass", () -> new GlassBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(5.0f, 1200.0f).sound(SoundType.GLASS)
					.noOcclusion().isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
					.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
	public static final RegistryObject<Block> MAGNET_COIL = register("magnet_coil", () -> new com.robvanblerk.tieredpower.block.MagnetCoilBlock(
			reactor().lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.block.MagnetCoilBlock.ACTIVE) ? 13 : 3)));
	public static final RegistryObject<Block> REACTOR_FUEL_PORT = register("reactor_fuel_port",
			() -> new ReactorPortBlock(ReactorPortBlock.Kind.FUEL, reactor()));
	public static final RegistryObject<Block> REACTOR_POWER_PORT = register("reactor_power_port",
			() -> new ReactorPortBlock(ReactorPortBlock.Kind.POWER, reactor()));
	public static final RegistryObject<Block> FUSION_CONTROLLER = register("fusion_controller", () -> new FusionControllerBlock(
			reactor().lightLevel(state -> state.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 12 : 0)));

	// ---- Energy Bank multiblock ----
	public static final RegistryObject<Block> BANK_CASING = register("bank_casing", () -> new Block(reactor()));
	public static final RegistryObject<Block> BANK_GLASS = register("bank_glass", () -> new GlassBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(5.0f, 1200.0f).sound(SoundType.GLASS)
					.noOcclusion().isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
					.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
	public static final RegistryObject<Block> BANK_PORT = register("bank_port", () -> new BankPortBlock(reactor()));
	public static final RegistryObject<Block> BANK_CONTROLLER = register("bank_controller", () -> new BankControllerBlock(
			reactor().lightLevel(state -> state.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 10 : 0)));
	public static final RegistryObject<Block> ENERGY_CELL = register("energy_cell",
			() -> new EnergyCellBlock(32_000_000L, reactor().lightLevel(s -> 4)));
	public static final RegistryObject<Block> ADVANCED_ENERGY_CELL = register("advanced_energy_cell",
			() -> new EnergyCellBlock(256_000_000L, reactor().lightLevel(s -> 6)));
	public static final RegistryObject<Block> ULTIMATE_ENERGY_CELL = register("ultimate_energy_cell",
			() -> new EnergyCellBlock(2_048_000_000L, reactor().lightLevel(s -> 8)));
	public static final RegistryObject<Block> QUANTUM_ENERGY_CELL = register("quantum_energy_cell",
			() -> new EnergyCellBlock(16_384_000_000L, reactor().lightLevel(s -> 10)));

	// ---- Tools and utilities ----
	public static final RegistryObject<Item> GUIDE_BOOK = ITEMS.register("guide_book", () -> new com.robvanblerk.tieredpower.item.GuideBookItem(new Item.Properties()));
	public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Block> SINK = register("sink", () -> new SinkBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion().forceSolidOn()));

	// ---- Phase 7: fluids and resources ----
	public static final RegistryObject<Block> ELECTRIC_PUMP = register("electric_pump", () -> new ElectricPumpBlock(machine()));
	public static final RegistryObject<Block> ROCK_CRUSHER = register("rock_crusher", () -> new RockCrusherBlock(machine()));
	public static final RegistryObject<Block> FREEZER = register("freezer", () -> new FreezerBlock(machine()));

	private static BlockBehaviour.Properties tank() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.GLASS).noOcclusion()
				.isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
				.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
	}

	public static final RegistryObject<Block> BASIC_FLUID_TANK = register("basic_fluid_tank", () -> new FluidTankBlock(16_000, tank()));
	public static final RegistryObject<Block> ADVANCED_FLUID_TANK = register("advanced_fluid_tank", () -> new FluidTankBlock(64_000, tank()));
	public static final RegistryObject<Block> ELITE_FLUID_TANK = register("elite_fluid_tank", () -> new FluidTankBlock(256_000, tank()));
	public static final RegistryObject<Block> ULTIMATE_FLUID_TANK = register("ultimate_fluid_tank", () -> new FluidTankBlock(1_024_000, tank()));
	public static final RegistryObject<Block> QUANTUM_FLUID_TANK = register("quantum_fluid_tank", () -> new FluidTankBlock(4_096_000, tank()));

	// ---- Phase 8: processing ----
	public static final RegistryObject<Block> ORE_PURIFIER = register("ore_purifier", () -> new OrePurifierBlock(machine()));
	public static final RegistryObject<Block> COMPRESSOR = register("compressor", () -> new CompressorBlock(machine()));
	public static final RegistryObject<Block> ELECTRIC_SAWMILL = register("electric_sawmill", () -> new ElectricSawmillBlock(machine()));
	public static final RegistryObject<Item> IRON_PLATE = ITEMS.register("iron_plate", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> GOLD_PLATE = ITEMS.register("gold_plate", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COPPER_PLATE = ITEMS.register("copper_plate", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> STEEL_PLATE = ITEMS.register("steel_plate", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SAWDUST = ITEMS.register("sawdust", () -> new FuelItem(100, new Item.Properties()));

	/** Fluid Pipes and Gas Pipes: basic_fluid_pipe ... ultimate_gas_pipe. */
	public static final Map<PipeTier.Kind, Map<PipeTier, RegistryObject<Block>>> PIPES = new EnumMap<>(PipeTier.Kind.class);

	static {
		for (PipeTier.Kind kind : PipeTier.Kind.values()) {
			Map<PipeTier, RegistryObject<Block>> tiers = new EnumMap<>(PipeTier.class);
			for (PipeTier tier : PipeTier.values()) {
				tiers.put(tier, register(tier.getName() + "_" + kind.suffix, () -> new FluidPipeBlock(tier, kind,
						BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8f).sound(SoundType.METAL).noOcclusion().forceSolidOn())));
			}
			PIPES.put(kind, tiers);
		}
	}

	/** Item Pipes: basic_item_pipe ... ultimate_item_pipe. */
	public static final Map<ItemPipeTier, RegistryObject<Block>> ITEM_PIPES = new EnumMap<>(ItemPipeTier.class);

	static {
		for (ItemPipeTier tier : ItemPipeTier.values()) {
			ITEM_PIPES.put(tier, register(tier.getName() + "_item_pipe", () -> new ItemPipeBlock(tier,
					BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8f).sound(SoundType.METAL).noOcclusion().forceSolidOn())));
		}
	}

	public static final RegistryObject<Item> ITEM_FILTER = ITEMS.register("item_filter", () -> new ItemFilterItem(new Item.Properties().stacksTo(16)));

	public static Block[] itemPipeBlocks() {
		return ITEM_PIPES.values().stream().map(RegistryObject::get).toArray(Block[]::new);
	}

	public static Block[] pipeBlocks() {
		return PIPES.values().stream().flatMap(m -> m.values().stream()).map(RegistryObject::get).toArray(Block[]::new);
	}

	// ---- Phase 9: automation ----
	public static final RegistryObject<Block> QUARRY = register("quarry", () -> new QuarryBlock(machine()));
	public static final RegistryObject<Block> CROP_FARMER = register("crop_farmer", () -> new CropFarmerBlock(machine()));
	public static final RegistryObject<Block> AUTO_CRAFTER = register("auto_crafter", () -> new AutoCrafterBlock(machine()));
	public static final RegistryObject<Block> CHUNK_LOADER = register("chunk_loader", () -> new ChunkLoaderBlock(machine()));

	// ---- Phase 11: powered items ----
	public static final TagKey<Block> DRILL_MINEABLE = TagKey.create(Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tieredpower", "mineable/drill"));
	public static final TagKey<Block> CHAINSAW_MINEABLE = TagKey.create(Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tieredpower", "mineable/chainsaw"));

	public static final RegistryObject<Item> ELECTRIC_DRILL = ITEMS.register("electric_drill",
			() -> new ElectricDrillItem(PoweredTier.ELECTRIC, DRILL_MINEABLE, 100_000, 100, false, new Item.Properties()));
	public static final RegistryObject<Item> ADVANCED_DRILL = ITEMS.register("advanced_drill",
			() -> new ElectricDrillItem(PoweredTier.ADVANCED, DRILL_MINEABLE, 1_000_000, 200, true, new Item.Properties().fireResistant()));
	public static final RegistryObject<Item> ELECTRIC_CHAINSAW = ITEMS.register("electric_chainsaw",
			() -> new ElectricChainsawItem(PoweredTier.ELECTRIC, CHAINSAW_MINEABLE, 200_000, 100, new Item.Properties()));
	public static final RegistryObject<Item> JETPACK = ITEMS.register("jetpack",
			() -> new JetpackItem(JetpackMaterial.BASIC, 400_000, 40, 0.1, 0.45, new Item.Properties()));
	public static final RegistryObject<Item> ADVANCED_JETPACK = ITEMS.register("advanced_jetpack",
			() -> new JetpackItem(JetpackMaterial.ADVANCED, 4_000_000, 80, 0.16, 0.8, new Item.Properties().fireResistant()));
	public static final RegistryObject<Item> PORTABLE_BATTERY = ITEMS.register("portable_battery",
			() -> new PortableBatteryItem(2_000_000, 2_000, new Item.Properties()));
	public static final RegistryObject<Item> ADVANCED_PORTABLE_BATTERY = ITEMS.register("advanced_portable_battery",
			() -> new PortableBatteryItem(32_000_000, 16_000, new Item.Properties().fireResistant()));
	public static final RegistryObject<Block> WIRELESS_CHARGER = register("wireless_charger", () -> new WirelessChargerBlock(machine()));

	// ---- Phase 12: solar, wind, fission ----
	private static BlockBehaviour.Properties panel() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0f).sound(SoundType.METAL).noOcclusion()
				.isValidSpawn((s, l, p, e) -> false);
	}

	public static final RegistryObject<Block> SOLAR_PANEL = register("solar_panel", () -> new SolarPanelBlock(20, panel()));
	public static final RegistryObject<Block> ADVANCED_SOLAR_PANEL = register("advanced_solar_panel", () -> new SolarPanelBlock(100, panel()));
	public static final RegistryObject<Block> ELITE_SOLAR_PANEL = register("elite_solar_panel", () -> new SolarPanelBlock(450, panel()));
	public static final RegistryObject<Block> ULTIMATE_SOLAR_PANEL = register("ultimate_solar_panel", () -> new SolarPanelBlock(2_000, panel()));
	public static final RegistryObject<Block> QUANTUM_SOLAR_PANEL = register("quantum_solar_panel", () -> new SolarPanelBlock(9_000, panel()));
	public static final RegistryObject<Block> WIND_TURBINE = register("wind_turbine", () -> new WindTurbineBlock(machine()));
	public static final RegistryObject<Block> FISSION_REACTOR = register("fission_reactor", () -> new FissionReactorBlock(
			machine().strength(8.0f, 1200.0f).lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 10 : 0)));

	public static final RegistryObject<Block> URANIUM_ORE = register("uranium_ore", () -> new DropExperienceBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f).requiresCorrectToolForDrops().lightLevel(s -> 2), UniformInt.of(1, 3)));
	public static final RegistryObject<Block> DEEPSLATE_URANIUM_ORE = register("deepslate_uranium_ore", () -> new DropExperienceBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5f, 3.0f).requiresCorrectToolForDrops()
					.sound(SoundType.DEEPSLATE).lightLevel(s -> 2), UniformInt.of(1, 3)));
	public static final RegistryObject<Item> RAW_URANIUM = ITEMS.register("raw_uranium", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> URANIUM_INGOT = ITEMS.register("uranium_ingot", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> URANIUM_DUST = ITEMS.register("uranium_dust", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> URANIUM_FUEL_ROD = ITEMS.register("uranium_fuel_rod", () -> new Item(new Item.Properties().stacksTo(16)));
	// Phase 39: nuclear fuel cycle
	public static final RegistryObject<Item> PLUTONIUM_DUST = ITEMS.register("plutonium_dust", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> PLUTONIUM_INGOT = ITEMS.register("plutonium_ingot", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> MOX_FUEL_ROD = ITEMS.register("mox_fuel_rod", () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> DEPLETED_FUEL_ROD = ITEMS.register("depleted_fuel_rod", () -> new Item(new Item.Properties().stacksTo(64)));

	// ---- Phase 15: multiblock fission ----
	public static final RegistryObject<Block> FISSION_CASING = register("fission_casing", () -> new Block(reactor()));
	public static final RegistryObject<Block> FISSION_GLASS = register("fission_glass", () -> new GlassBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(5.0f, 1200.0f).sound(SoundType.GLASS)
					.noOcclusion().isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
					.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
	public static final RegistryObject<Block> FUEL_ASSEMBLY = register("fuel_assembly", () -> new FuelAssemblyBlock(
			reactor().lightLevel(s -> s.getValue(FuelAssemblyBlock.ACTIVE) ? 12 : 2)));
	public static final RegistryObject<Block> COOLANT_CHANNEL = register("coolant_channel", () -> new Block(reactor()));
	public static final RegistryObject<Block> FISSION_FUEL_PORT = register("fission_fuel_port", () -> new FissionPortBlock(FissionPortBlock.Kind.FUEL, reactor()));
	public static final RegistryObject<Block> FISSION_WASTE_PORT = register("fission_waste_port", () -> new FissionPortBlock(FissionPortBlock.Kind.WASTE, reactor()));
	public static final RegistryObject<Block> FISSION_COOLANT_PORT = register("fission_coolant_port", () -> new FissionPortBlock(FissionPortBlock.Kind.COOLANT, reactor()));
	public static final RegistryObject<Block> FISSION_POWER_PORT = register("fission_power_port", () -> new FissionPortBlock(FissionPortBlock.Kind.POWER, reactor()));
	public static final RegistryObject<Block> FISSION_CONTROLLER = register("fission_controller", () -> new FissionControllerBlock(
			reactor().lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 12 : 0)));

	// ---- Phase 16: more machines ----
	public static final RegistryObject<Block> ENCHANTING_MACHINE = register("enchanting_machine", () -> new EnchantingMachineBlock(machine()));
	public static final RegistryObject<Block> MOB_GRINDER = register("mob_grinder", () -> new MobGrinderBlock(machine()));
	public static final RegistryObject<Block> SPAWNER_CONTROLLER = register("spawner_controller", () -> new SpawnerControllerBlock(machine()));
	public static final RegistryObject<Block> FLUID_MIXER = register("fluid_mixer", () -> new FluidMixerBlock(machine()));
	public static final RegistryObject<Block> TELEPORTER = register("teleporter", () -> new TeleporterBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 7)));
	public static final RegistryObject<Block> CONDENSER = register("condenser", () -> new CondenserBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).sound(SoundType.METAL)));
	public static final RegistryObject<Item> TELEPORTER_LINKER = ITEMS.register("teleporter_linker", () -> new com.robvanblerk.tieredpower.item.TeleporterLinkerItem(new Item.Properties()));

	// ---- Phase 63: Logic Controller ----
	public static final RegistryObject<Block> DIGITAL_FACTORY = register("digital_factory", () -> new com.robvanblerk.tieredpower.factory.DigitalFactoryBlock(machine()));
	public static final RegistryObject<Block> FACTORY_CELL = register("factory_cell", () -> new Block(machine().lightLevel(s -> 4)));
	public static final RegistryObject<Block> FACTORY_PORT = register("factory_port", () -> new com.robvanblerk.tieredpower.factory.FactoryPortBlock(machine()));
	public static final RegistryObject<Block> LOGIC_CONTROLLER = register("logic_controller", () -> new com.robvanblerk.tieredpower.logic.LogicControllerBlock(machine()));
	public static final RegistryObject<Block> REDSTONE_TRANSMITTER = register("redstone_transmitter", () -> new com.robvanblerk.tieredpower.redstone.WirelessRedstoneBlock(false, machine()));
	public static final RegistryObject<Block> REDSTONE_RECEIVER = register("redstone_receiver", () -> new com.robvanblerk.tieredpower.redstone.WirelessRedstoneBlock(true, machine()));
	public static final RegistryObject<Block> MACHINE_STATUS_DISPLAY = register("machine_status_display", () -> new com.robvanblerk.tieredpower.logic.MachineStatusDisplayBlock(machine().lightLevel(s -> 6)));

	// ---- Phase 62: Energy Core ----
	public static final RegistryObject<Block> ENERGY_CORE = register("energy_core", () -> new com.robvanblerk.tieredpower.core.EnergyCoreBlock(false, machine().noOcclusion().lightLevel(s -> 10)));
	public static final RegistryObject<Block> INPUT_PYLON = register("input_pylon", () -> new com.robvanblerk.tieredpower.core.EnergyCoreBlock(true, machine().noOcclusion()));
	public static final RegistryObject<Block> OUTPUT_PYLON = register("output_pylon", () -> new com.robvanblerk.tieredpower.core.EnergyCoreBlock(true, machine().noOcclusion()));
	/** Core Upgrades for tiers II-V (index 0 = tier II). */
	public static final java.util.List<RegistryObject<Item>> CORE_UPGRADES = java.util.List.of(
			ITEMS.register("core_upgrade_2", () -> new com.robvanblerk.tieredpower.core.CoreUpgradeItem(2, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))),
			ITEMS.register("core_upgrade_3", () -> new com.robvanblerk.tieredpower.core.CoreUpgradeItem(3, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))),
			ITEMS.register("core_upgrade_4", () -> new com.robvanblerk.tieredpower.core.CoreUpgradeItem(4, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))),
			ITEMS.register("core_upgrade_5", () -> new com.robvanblerk.tieredpower.core.CoreUpgradeItem(5, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))));
	/** Only used to draw the glowing orb inside the core (not in any tab). */
	public static final RegistryObject<Item> ENERGY_CORE_ORB = ITEMS.register("energy_core_orb", () -> new Item(new Item.Properties()));

	// ---- Phase 61: Liquid Experience ----
	public static final RegistryObject<net.minecraft.world.level.block.LiquidBlock> EXPERIENCE_BLOCK = BLOCKS.register("experience",
			() -> new net.minecraft.world.level.block.LiquidBlock(ModFluids.EXPERIENCE, net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WATER).noLootTable().lightLevel(s -> 8)));
	public static final RegistryObject<Item> EXPERIENCE_BUCKET = ITEMS.register("experience_bucket",
			() -> new net.minecraft.world.item.BucketItem(ModFluids.EXPERIENCE, new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));

	// ---- Phase 60: biodiesel ----
	public static final RegistryObject<Block> BIO_REFINERY = register("bio_refinery", () -> new com.robvanblerk.tieredpower.industry.BioRefineryBlock(machine()));
	public static final RegistryObject<Block> DIESEL_GENERATOR = register("diesel_generator", () -> new com.robvanblerk.tieredpower.industry.DieselGeneratorBlock(machine()));
	public static final RegistryObject<net.minecraft.world.level.block.LiquidBlock> BIODIESEL_BLOCK = BLOCKS.register("biodiesel",
			() -> new net.minecraft.world.level.block.LiquidBlock(ModFluids.BIODIESEL, net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WATER).noLootTable()));
	public static final RegistryObject<Item> BIODIESEL_BUCKET = ITEMS.register("biodiesel_bucket",
			() -> new net.minecraft.world.item.BucketItem(ModFluids.BIODIESEL, new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));

	// ---- Phase 59: gas containers ----
	public static final RegistryObject<Item> GAS_CYLINDER = ITEMS.register("gas_cylinder", () -> new com.robvanblerk.tieredpower.item.GasCylinderItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Block> GAS_TANK = register("gas_tank", () -> new com.robvanblerk.tieredpower.block.GasTankBlock(machine().noOcclusion()));

	// ---- Phase 58: Coke Oven and Industrial Blast Furnace ----
	public static final RegistryObject<Block> COKE_OVEN = register("coke_oven", () -> new com.robvanblerk.tieredpower.industry.HeavyFurnaceBlock(machine().lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 12 : 0)));
	public static final RegistryObject<Block> INDUSTRIAL_BLAST_FURNACE = register("industrial_blast_furnace", () -> new com.robvanblerk.tieredpower.industry.HeavyFurnaceBlock(machine().lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 13 : 0)));
	public static final RegistryObject<Item> COAL_COKE = ITEMS.register("coal_coke", () -> new com.robvanblerk.tieredpower.industry.CoalCokeItem(new Item.Properties()));
	public static final RegistryObject<Block> TREATED_PLANKS = register("treated_planks", () -> new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS)));
	public static final RegistryObject<net.minecraft.world.level.block.LiquidBlock> CREOSOTE_BLOCK = BLOCKS.register("creosote",
			() -> new net.minecraft.world.level.block.LiquidBlock(ModFluids.CREOSOTE, net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WATER).noLootTable()));
	public static final RegistryObject<Item> CREOSOTE_BUCKET = ITEMS.register("creosote_bucket",
			() -> new net.minecraft.world.item.BucketItem(ModFluids.CREOSOTE, new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));

	// ---- Phase 57: Digital Miner ----
	public static final RegistryObject<Block> DIGITAL_MINER = register("digital_miner", () -> new com.robvanblerk.tieredpower.block.DigitalMinerBlock(machine().noOcclusion()));

	// ---- Phase 56: greenhouse ----
	public static final RegistryObject<Block> SPRINKLER = register("sprinkler", () -> new com.robvanblerk.tieredpower.greenhouse.GreenhouseBlock(true, machine().noOcclusion()));
	public static final RegistryObject<Block> GROW_LAMP = register("grow_lamp", () -> new com.robvanblerk.tieredpower.greenhouse.GreenhouseBlock(false, machine().noOcclusion()
			.lightLevel(s -> s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) && s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
	public static final RegistryObject<Block> GREENHOUSE_GLASS = register("greenhouse_glass", () -> new net.minecraft.world.level.block.GlassBlock(
			net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.GLASS)));
	public static final RegistryObject<Item> FERTILIZER = ITEMS.register("fertilizer", () -> new com.robvanblerk.tieredpower.greenhouse.FertilizerItem(new Item.Properties()));
	public static final RegistryObject<Block> IRRIGATED_SAND = register("irrigated_sand", () -> new com.robvanblerk.tieredpower.greenhouse.IrrigatedSandBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.6f).sound(SoundType.SAND)));
	public static final RegistryObject<Item> SUGAR_CANE_SEEDS = ITEMS.register("sugar_cane_seeds", () -> new com.robvanblerk.tieredpower.greenhouse.SugarCaneSeedsItem(new Item.Properties()));

	// ---- Phase 55: Fluidic Plenisher, Block Mover, Ore Scanner ----
	public static final RegistryObject<Block> FLUIDIC_PLENISHER = register("fluidic_plenisher", () -> new com.robvanblerk.tieredpower.block.FluidicPlenisherBlock(machine()));
	public static final RegistryObject<Item> BLOCK_MOVER = ITEMS.register("block_mover", () -> new com.robvanblerk.tieredpower.item.BlockMoverItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> ORE_SCANNER = ITEMS.register("ore_scanner", () -> new com.robvanblerk.tieredpower.item.powered.OreScannerItem(new Item.Properties().stacksTo(1)));

	// ---- Phase 54: automated brewing ----
	public static final RegistryObject<Block> BREWING_MACHINE = register("brewing_machine", () -> new com.robvanblerk.tieredpower.block.BrewingMachineBlock(machine()));

	// ---- Phase 53: radiation ----
	public static final RegistryObject<Item> HAZMAT_HELMET = ITEMS.register("hazmat_helmet", () -> new net.minecraft.world.item.ArmorItem(com.robvanblerk.tieredpower.radiation.HazmatMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties()));
	public static final RegistryObject<Item> HAZMAT_CHESTPLATE = ITEMS.register("hazmat_chestplate", () -> new net.minecraft.world.item.ArmorItem(com.robvanblerk.tieredpower.radiation.HazmatMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties()));
	public static final RegistryObject<Item> HAZMAT_LEGGINGS = ITEMS.register("hazmat_leggings", () -> new net.minecraft.world.item.ArmorItem(com.robvanblerk.tieredpower.radiation.HazmatMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties()));
	public static final RegistryObject<Item> HAZMAT_BOOTS = ITEMS.register("hazmat_boots", () -> new net.minecraft.world.item.ArmorItem(com.robvanblerk.tieredpower.radiation.HazmatMaterial.INSTANCE, net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties()));
	public static final RegistryObject<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter", () -> new com.robvanblerk.tieredpower.radiation.GeigerCounterItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> IODINE_TABLETS = ITEMS.register("iodine_tablets", () -> new com.robvanblerk.tieredpower.radiation.IodineTabletsItem(new Item.Properties().stacksTo(16)));

	// ---- Phase 52: Orbital Mining Laser ----
	public static final RegistryObject<Block> LASER_DRILL = register("laser_drill", () -> new com.robvanblerk.tieredpower.block.LaserDrillBlock(machine().noOcclusion()));
	public static final RegistryObject<Item> MINING_SATELLITE = ITEMS.register("mining_satellite", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("A Rocket payload: once in orbit, a Laser Drill claims it and mines ores with its laser", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
	public static final java.util.List<RegistryObject<Item>> LASER_LENSES = java.util.List.of(
			ITEMS.register("iron_laser_lens", () -> new com.robvanblerk.tieredpower.item.LaserLensItem("iron", new Item.Properties().stacksTo(16))),
			ITEMS.register("copper_laser_lens", () -> new com.robvanblerk.tieredpower.item.LaserLensItem("copper", new Item.Properties().stacksTo(16))),
			ITEMS.register("gold_laser_lens", () -> new com.robvanblerk.tieredpower.item.LaserLensItem("gold", new Item.Properties().stacksTo(16))),
			ITEMS.register("redstone_laser_lens", () -> new com.robvanblerk.tieredpower.item.LaserLensItem("redstone", new Item.Properties().stacksTo(16))),
			ITEMS.register("diamond_laser_lens", () -> new com.robvanblerk.tieredpower.item.LaserLensItem("diamond", new Item.Properties().stacksTo(16))),
			ITEMS.register("uranium_laser_lens", () -> new com.robvanblerk.tieredpower.item.LaserLensItem("uranium", new Item.Properties().stacksTo(16))));

	// ---- Rocket liquids: buckets and world blocks (handy for testing in creative) ----
	public static final RegistryObject<net.minecraft.world.level.block.LiquidBlock> LIQUID_METHANE_BLOCK = BLOCKS.register("liquid_methane",
			() -> new net.minecraft.world.level.block.LiquidBlock(ModFluids.LIQUID_METHANE, net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WATER).noLootTable()));
	public static final RegistryObject<Item> LIQUID_METHANE_BUCKET = ITEMS.register("liquid_methane_bucket",
			() -> new net.minecraft.world.item.BucketItem(ModFluids.LIQUID_METHANE, new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
	public static final RegistryObject<net.minecraft.world.level.block.LiquidBlock> LIQUID_OXYGEN_BLOCK = BLOCKS.register("liquid_oxygen",
			() -> new net.minecraft.world.level.block.LiquidBlock(ModFluids.LIQUID_OXYGEN, net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WATER).noLootTable()));
	public static final RegistryObject<Item> LIQUID_OXYGEN_BUCKET = ITEMS.register("liquid_oxygen_bucket",
			() -> new net.minecraft.world.item.BucketItem(ModFluids.LIQUID_OXYGEN, new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
	public static final RegistryObject<net.minecraft.world.level.block.LiquidBlock> ROCKET_FUEL_BLOCK = BLOCKS.register("rocket_fuel",
			() -> new net.minecraft.world.level.block.LiquidBlock(ModFluids.ROCKET_FUEL, net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WATER).noLootTable()));
	public static final RegistryObject<Item> ROCKET_FUEL_BUCKET = ITEMS.register("rocket_fuel_bucket",
			() -> new net.minecraft.world.item.BucketItem(ModFluids.ROCKET_FUEL, new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));

	// ---- Phase 51: rocket program, part 2 ----
	public static final RegistryObject<Block> LAUNCH_PAD = register("launch_pad", () -> new com.robvanblerk.tieredpower.block.LaunchPadBlock(false, machine().noOcclusion()));
	public static final RegistryObject<Block> LAUNCH_TOWER = register("launch_tower", () -> new com.robvanblerk.tieredpower.block.LaunchPadBlock(true, machine().noOcclusion()));
	public static final RegistryObject<Block> LAUNCH_CONTROLLER = register("launch_controller", () -> new com.robvanblerk.tieredpower.block.LaunchControllerBlock(false, machine().noOcclusion()));
	public static final RegistryObject<Block> RECEIVER_DISH = register("receiver_dish", () -> new com.robvanblerk.tieredpower.block.LaunchControllerBlock(true, machine().noOcclusion()));

	// ---- Phase 50: rocket program, part 1 ----
	public static final RegistryObject<Block> CRYOGENIC_CONDENSER = register("cryogenic_condenser", () -> new com.robvanblerk.tieredpower.block.CryogenicCondenserBlock(machine()));
	public static final RegistryObject<Block> FUEL_REFINERY = register("fuel_refinery", () -> new com.robvanblerk.tieredpower.block.FuelRefineryBlock(machine()));
	public static final RegistryObject<Item> HULL_PLATE = ITEMS.register("hull_plate", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("Rocket structure - pressed from steel plates in a Compressor", new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> ROCKET_NOZZLE = ITEMS.register("rocket_nozzle", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("Upgrade it to a Rocket Engine in a Smithing Press", new Item.Properties().stacksTo(16).rarity(Rarity.COMMON)));
	public static final RegistryObject<Item> ROCKET_ENGINE = ITEMS.register("rocket_engine", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("A methalox engine - part of a Rocket", new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
	public static final RegistryObject<Item> FUEL_TANK_SECTION = ITEMS.register("fuel_tank_section", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("Holds the Rocket's fuel - part of a Rocket", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> GUIDANCE_COMPUTER = ITEMS.register("guidance_computer", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("Steers the Rocket into orbit - part of a Rocket", new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
	public static final RegistryObject<Item> NOSE_CONE = ITEMS.register("nose_cone", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("The top of a Rocket", new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> ROCKET = ITEMS.register("rocket", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("Put it in a Launch Controller with a payload and 16,000 mB of Rocket Fuel", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
	public static final RegistryObject<Item> SOLAR_SATELLITE = ITEMS.register("solar_satellite", () -> new com.robvanblerk.tieredpower.item.RocketPartItem("A Rocket payload: once in orbit, a Receiver Dish can claim it for 100,000 FE/t", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

	// ---- Phase 49: Quantum Suit modules ----
	public static final java.util.Map<com.robvanblerk.tieredpower.item.powered.SuitModules.Module, RegistryObject<Item>> SUIT_MODULES =
			new java.util.EnumMap<>(com.robvanblerk.tieredpower.item.powered.SuitModules.Module.class);
	static {
		for (var m : com.robvanblerk.tieredpower.item.powered.SuitModules.Module.values())
			SUIT_MODULES.put(m, ITEMS.register(m.id + "_module", () -> new com.robvanblerk.tieredpower.item.SuitModuleItem(m, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))));
	}

	// ---- Phase 48: new logistics ----
	public static final RegistryObject<Block> WIRELESS_SENDER = register("wireless_sender", () -> new com.robvanblerk.tieredpower.block.WirelessTransportBlock(false, machine()));
	public static final RegistryObject<Block> WIRELESS_RECEIVER = register("wireless_receiver", () -> new com.robvanblerk.tieredpower.block.WirelessTransportBlock(true, machine()));
	public static final RegistryObject<Block> ITEM_BUFFER = register("item_buffer", () -> new com.robvanblerk.tieredpower.block.BufferBlock(machine()));
	public static final RegistryObject<Block> FLUID_BUFFER = register("fluid_buffer", () -> new com.robvanblerk.tieredpower.block.BufferBlock(machine()));

	// ---- Phase 47: antimatter, fluid filters, Quantum Drill ----
	public static final RegistryObject<Block> PARTICLE_COLLIDER = register("particle_collider", () -> new com.robvanblerk.tieredpower.block.ParticleColliderBlock(machine()));
	public static final RegistryObject<Block> ANTIMATTER_REACTOR = register("antimatter_reactor", () -> new com.robvanblerk.tieredpower.block.AntimatterReactorBlock(
			machine().lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.block.AntimatterReactorBlock.LIT) ? 12 : 2)));
	public static final RegistryObject<Item> FLUID_FILTER = ITEMS.register("fluid_filter", () -> new com.robvanblerk.tieredpower.item.FluidFilterItem(new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Item> QUANTUM_DRILL = ITEMS.register("quantum_drill",
			() -> new com.robvanblerk.tieredpower.item.powered.QuantumDrillItem(new Item.Properties().rarity(Rarity.EPIC)));

	// ---- Phase 46: fusion upgrades ----
	/** Plasma Coil Mk I, II, III - index 0 is Mk I. */
	public static final java.util.List<RegistryObject<Item>> PLASMA_COILS = java.util.List.of(
			ITEMS.register("plasma_coil_mk1", () -> new com.robvanblerk.tieredpower.item.PlasmaCoilItem(1, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))),
			ITEMS.register("plasma_coil_mk2", () -> new com.robvanblerk.tieredpower.item.PlasmaCoilItem(2, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))),
			ITEMS.register("plasma_coil_mk3", () -> new com.robvanblerk.tieredpower.item.PlasmaCoilItem(3, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))));

	// ---- Phase 45: Assembly Matrix and Machine Connector ----
	public static final RegistryObject<Block> MATRIX_CASING = register("matrix_casing", () -> new com.robvanblerk.tieredpower.block.matrix.MatrixPartBlock(com.robvanblerk.tieredpower.block.matrix.MatrixPartBlock.Kind.CASING, machine()));
	public static final RegistryObject<Block> MATRIX_GLASS = register("matrix_glass", () -> new com.robvanblerk.tieredpower.block.matrix.MatrixPartBlock(com.robvanblerk.tieredpower.block.matrix.MatrixPartBlock.Kind.GLASS,
			machine().noOcclusion().sound(SoundType.GLASS).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
	public static final RegistryObject<Block> MATRIX_CONTROLLER = register("matrix_controller", () -> new com.robvanblerk.tieredpower.block.matrix.MatrixControllerBlock(machine()));
	public static final RegistryObject<Block> PATTERN_BANK = register("pattern_bank", () -> new com.robvanblerk.tieredpower.block.matrix.PatternBankBlock(machine()));
	public static final RegistryObject<Block> OVERCLOCK_ACCELERATOR = register("overclock_accelerator", () -> new com.robvanblerk.tieredpower.block.matrix.MatrixPartBlock(com.robvanblerk.tieredpower.block.matrix.MatrixPartBlock.Kind.ACCELERATOR, machine().lightLevel(s -> 7)));
	public static final RegistryObject<Block> CRAFTING_ACCELERATOR = register("crafting_accelerator", () -> new com.robvanblerk.tieredpower.block.matrix.CraftingAcceleratorBlock(machine()));
	/** Crafting Upgrades: raise a Crafting CPU or Crafting Accelerator to Advanced (1), Elite (2) or Quantum (3). */
	public static final java.util.List<RegistryObject<Item>> CRAFTING_UPGRADES = java.util.List.of(
			ITEMS.register("crafting_upgrade_advanced", () -> new com.robvanblerk.tieredpower.item.CraftingUpgradeItem(1, new Item.Properties().stacksTo(16))),
			ITEMS.register("crafting_upgrade_elite", () -> new com.robvanblerk.tieredpower.item.CraftingUpgradeItem(2, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))),
			ITEMS.register("crafting_upgrade_quantum", () -> new com.robvanblerk.tieredpower.item.CraftingUpgradeItem(3, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))));
	public static final RegistryObject<Block> ASSEMBLER_PANEL = register("assembler_panel", () -> new com.robvanblerk.tieredpower.block.AssemblerPanelBlock(machine().noOcclusion()));
	// ---- Planet resources (Stargate planets only) ----
	private static BlockBehaviour.Properties planetOre(MapColor colour, float hardness, SoundType sound) {
		return BlockBehaviour.Properties.of().mapColor(colour).strength(hardness, 3.0f).requiresCorrectToolForDrops().sound(sound);
	}

	public static final RegistryObject<Block> CRYONITE_ORE = register("cryonite_ore", () -> new DropExperienceBlock(planetOre(MapColor.ICE, 3.0f, SoundType.GLASS).lightLevel(s -> 4), UniformInt.of(2, 5)));
	public static final RegistryObject<Item> CRYONITE_SHARD = ITEMS.register("cryonite_shard", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Block> CRYONITE_BLOCK = register("cryonite_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(3.0f).requiresCorrectToolForDrops().sound(SoundType.GLASS).lightLevel(s -> 6)));
	public static final RegistryObject<Block> SOLARITE_SAND = register("solarite_sand", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.6f).sound(SoundType.SAND).lightLevel(s -> 5)));
	public static final RegistryObject<Block> SOLAR_GLASS = register("solar_glass", () -> new GlassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.5f).sound(SoundType.GLASS)
			.noOcclusion().lightLevel(s -> 8).isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
	public static final RegistryObject<Block> LIVINGWOOD_LOG = register("livingwood_log", () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f).sound(SoundType.WOOD).lightLevel(s -> 3)));
	public static final RegistryObject<Block> LIVINGWOOD_PLANKS = register("livingwood_planks", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f, 3.0f).sound(SoundType.WOOD)));
	public static final RegistryObject<Block> NAQUADAH_ORE = register("naquadah_ore", () -> new DropExperienceBlock(planetOre(MapColor.COLOR_BLACK, 4.5f, SoundType.NETHER_ORE), UniformInt.of(3, 6)));
	public static final RegistryObject<Item> RAW_NAQUADAH = ITEMS.register("raw_naquadah", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> NAQUADAH_DUST = ITEMS.register("naquadah_dust", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> NAQUADAH_INGOT = ITEMS.register("naquadah_ingot", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Block> NAQUADAH_BLOCK = register("naquadah_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(6.0f, 1200.0f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
	public static final RegistryObject<Block> ABYSSAL_PEARL_ORE = register("abyssal_pearl_ore", () -> new DropExperienceBlock(planetOre(MapColor.WARPED_WART_BLOCK, 3.0f, SoundType.STONE).lightLevel(s -> 3), UniformInt.of(2, 5)));
	public static final RegistryObject<Item> ABYSSAL_PEARL = ITEMS.register("abyssal_pearl", () -> new com.robvanblerk.tieredpower.planet.AbyssalPearlItem(new Item.Properties().stacksTo(16)));
	public static final RegistryObject<Block> AETHER_CRYSTAL_ORE = register("aether_crystal_ore", () -> new DropExperienceBlock(planetOre(MapColor.COLOR_PINK, 3.0f, SoundType.AMETHYST).lightLevel(s -> 5), UniformInt.of(2, 5)));
	public static final RegistryObject<Item> AETHER_CRYSTAL = ITEMS.register("aether_crystal", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Block> SPORECAP = register("sporecap", () -> new com.robvanblerk.tieredpower.planet.SporecapBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
			.noCollission().instabreak().sound(SoundType.FUNGUS).lightLevel(s -> 7).offsetType(BlockBehaviour.OffsetType.XZ)));
	public static final RegistryObject<Block> RESONANCE_CRYSTAL_ORE = register("resonance_crystal_ore", () -> new DropExperienceBlock(planetOre(MapColor.COLOR_LIGHT_BLUE, 4.0f, SoundType.AMETHYST).lightLevel(s -> 6), UniformInt.of(3, 6)));
	public static final RegistryObject<Item> RESONANCE_CRYSTAL = ITEMS.register("resonance_crystal", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SPEED_UPGRADE_2 = ITEMS.register("speed_upgrade_2", () -> new Item(new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> EFFICIENCY_UPGRADE_2 = ITEMS.register("efficiency_upgrade_2", () -> new Item(new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> NAQUADAH_INSTALLER = ITEMS.register("naquadah_installer", () -> new com.robvanblerk.tieredpower.item.TierInstallerItem(5, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
	public static final RegistryObject<Block> STELLAR_SOLAR_PANEL = register("stellar_solar_panel", () -> new SolarPanelBlock(36_000, panel()));
	// hidden worlds
	public static final RegistryObject<Block> LIFEBLOOM = register("lifebloom", () -> new com.robvanblerk.tieredpower.planet.SporecapBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK)
			.noCollission().instabreak().sound(SoundType.GRASS).lightLevel(s -> 4).offsetType(BlockBehaviour.OffsetType.XZ)));
	public static final RegistryObject<Block> SUNSTONE_ORE = register("sunstone_ore", () -> new DropExperienceBlock(planetOre(MapColor.COLOR_ORANGE, 3.0f, SoundType.STONE).lightLevel(s -> 7), UniformInt.of(2, 5)));
	public static final RegistryObject<Item> SUNSTONE = ITEMS.register("sunstone", () -> new com.robvanblerk.tieredpower.planet.SunstoneItem(new Item.Properties()));
	public static final RegistryObject<Block> AMBER_ORE = register("amber_ore", () -> new DropExperienceBlock(planetOre(MapColor.COLOR_ORANGE, 3.0f, SoundType.STONE), UniformInt.of(2, 5)));
	public static final RegistryObject<Item> AMBER = ITEMS.register("amber", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Block> WITCHROOT = register("witchroot", () -> new com.robvanblerk.tieredpower.planet.SporecapBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
			.noCollission().instabreak().sound(SoundType.ROOTS).lightLevel(s -> 3).offsetType(BlockBehaviour.OffsetType.XZ)));
	public static final RegistryObject<Block> GRAVITITE_ORE = register("gravitite_ore", () -> new DropExperienceBlock(planetOre(MapColor.COLOR_LIGHT_GRAY, 4.0f, SoundType.STONE), UniformInt.of(3, 6)));
	public static final RegistryObject<Item> GRAVITITE = ITEMS.register("gravitite", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Block> SOUL_CRYSTAL_ORE = register("soul_crystal_ore", () -> new DropExperienceBlock(planetOre(MapColor.COLOR_CYAN, 3.0f, SoundType.SOUL_SOIL).lightLevel(s -> 6), UniformInt.of(2, 5)));
	public static final RegistryObject<Item> SOUL_CRYSTAL = ITEMS.register("soul_crystal", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Block> UMBRAL_ORE = register("umbral_ore", () -> new DropExperienceBlock(planetOre(MapColor.COLOR_BLACK, 4.5f, SoundType.DEEPSLATE).lightLevel(s -> 2), UniformInt.of(3, 7)));
	public static final RegistryObject<Item> UMBRAL_SHARD = ITEMS.register("umbral_shard", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Block> VOIDSTONE_ORE = register("voidstone_ore", () -> new DropExperienceBlock(planetOre(MapColor.SAND, 4.0f, SoundType.STONE).lightLevel(s -> 4), UniformInt.of(3, 7)));
	public static final RegistryObject<Item> VOID_SHARD = ITEMS.register("void_shard", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> VOID_PEARL = ITEMS.register("void_pearl", () -> new com.robvanblerk.tieredpower.planet.VoidPearlItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

	// ---- Stargate ----
	public static final RegistryObject<Block> STARGATE_FRAME = register("stargate_frame", () -> new com.robvanblerk.tieredpower.stargate.StargateFrameBlock(reactor().noOcclusion().lightLevel(s -> 3)));
	public static final RegistryObject<Block> GUARDIAN_ALTAR = register("guardian_altar", () -> new com.robvanblerk.tieredpower.planet.GuardianAltarBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(30f, 1200f).requiresCorrectToolForDrops()
					.lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.planet.GuardianAltarBlock.SPENT) ? 0 : 10)));
	public static final RegistryObject<Item> GUARDIAN_HEART = ITEMS.register("guardian_heart", () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)) {
		@Override public boolean isFoil(ItemStack stack) { return true; }
	});
	public static final RegistryObject<Item> ADDRESS_TABLET = ITEMS.register("address_tablet", () -> new com.robvanblerk.tieredpower.stargate.AddressTabletItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	public static final RegistryObject<Block> STARGATE_DIALER = register("stargate_dialer", () -> new com.robvanblerk.tieredpower.stargate.StargateDialerBlock(
			reactor().noOcclusion().lightLevel(state -> state.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 12 : 5)));
	public static final RegistryObject<Block> QUANTUM_CHEST = register("quantum_chest", () -> new com.robvanblerk.tieredpower.quantum.QuantumChestBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.NETHERITE_BLOCK).lightLevel(state -> 7)));
	public static final RegistryObject<Item> QUANTUM_LINKER = ITEMS.register("quantum_linker", () -> new com.robvanblerk.tieredpower.quantum.QuantumLinkerItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Block> EVENT_HORIZON = BLOCKS.register("event_horizon", () -> new com.robvanblerk.tieredpower.stargate.EventHorizonBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollission().noOcclusion().strength(-1.0f, 3_600_000.0f)
					.lightLevel(s -> 14).noLootTable().sound(SoundType.GLASS).isValidSpawn((s, l, p, e) -> false)));

	// ---- Elevators ----
	public static final RegistryObject<Block> ELEVATOR = register("elevator", () -> new com.robvanblerk.tieredpower.elevator.ElevatorBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL)));

	// ---- Item Magnets ----
	public static final RegistryObject<Item> ITEM_MAGNET = ITEMS.register("item_magnet", () -> new com.robvanblerk.tieredpower.item.MagnetItem(false, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> ADVANCED_ITEM_MAGNET = ITEMS.register("advanced_item_magnet", () -> new com.robvanblerk.tieredpower.item.MagnetItem(true, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

	// ---- Spatial Capture ----
	public static final RegistryObject<Block> SPATIAL_PROJECTOR = register("spatial_projector", () -> new com.robvanblerk.tieredpower.spatial.SpatialProjectorBlock(machine()));
	public static final RegistryObject<Item> SPATIAL_CELL_SMALL = ITEMS.register("spatial_cell_small", () -> new com.robvanblerk.tieredpower.spatial.SpatialCellItem(3, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> SPATIAL_CELL_MEDIUM = ITEMS.register("spatial_cell_medium", () -> new com.robvanblerk.tieredpower.spatial.SpatialCellItem(7, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> SPATIAL_CELL_LARGE = ITEMS.register("spatial_cell_large", () -> new com.robvanblerk.tieredpower.spatial.SpatialCellItem(15, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

	// ---- Industrial Turbine (multiblock) ----
	public static final RegistryObject<Block> TURBINE_CASING = register("turbine_casing", () -> new Block(reactor()));
	public static final RegistryObject<Block> TURBINE_GLASS = register("turbine_glass", () -> new GlassBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(5.0f, 1200.0f).sound(SoundType.GLASS)
					.noOcclusion().isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
					.isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
	public static final RegistryObject<Block> TURBINE_VALVE = register("turbine_valve", () -> new com.robvanblerk.tieredpower.turbine.TurbineValveBlock(reactor()));
	public static final RegistryObject<Block> TURBINE_CONTROLLER = register("turbine_controller", () -> new com.robvanblerk.tieredpower.turbine.TurbineControllerBlock(
			reactor().lightLevel(state -> state.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) ? 10 : 0)));
	public static final RegistryObject<Block> TURBINE_ROTOR = register("turbine_rotor", () -> new com.robvanblerk.tieredpower.turbine.TurbineRotorBlock(machine().noOcclusion()));
	/** Only used to draw the spinning blades. */
	public static final RegistryObject<Item> TURBINE_BLADES = ITEMS.register("turbine_blades", () -> new Item(new Item.Properties()));

	// ---- Utility Drones ----
	public static final RegistryObject<Block> DRONE_STATION = register("drone_station", () -> new com.robvanblerk.tieredpower.drone.DroneStationBlock(machine()));
	public static final RegistryObject<Item> UTILITY_DRONE = ITEMS.register("utility_drone", () -> new Item(new Item.Properties().stacksTo(4)));
	/** Only used to draw a drone's spinning rotors. */
	public static final RegistryObject<Item> DRONE_ROTOR = ITEMS.register("drone_rotor", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> DRONE_REMOTE = ITEMS.register("drone_remote", () -> new com.robvanblerk.tieredpower.drone.DroneRemoteItem(new Item.Properties().stacksTo(1)));

	// ---- Conveyor belts ----
	private static BlockBehaviour.Properties belt() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.METAL).noOcclusion()
				.isValidSpawn((s, l, p, e) -> false);
	}

	public static final RegistryObject<Block> CONVEYOR_BELT = register("conveyor_belt", () -> new com.robvanblerk.tieredpower.conveyor.ConveyorBlock(0, belt()));
	public static final RegistryObject<Block> FAST_CONVEYOR_BELT = register("fast_conveyor_belt", () -> new com.robvanblerk.tieredpower.conveyor.ConveyorBlock(1, belt()));
	public static final RegistryObject<Block> EXPRESS_CONVEYOR_BELT = register("express_conveyor_belt", () -> new com.robvanblerk.tieredpower.conveyor.ConveyorBlock(2, belt()));
	public static final RegistryObject<Block> BELT_SPLITTER = register("belt_splitter", () -> new com.robvanblerk.tieredpower.conveyor.SplitterConveyorBlock(2, belt()));
	public static final RegistryObject<Block> FILTER_BELT = register("filter_belt", () -> new com.robvanblerk.tieredpower.conveyor.FilterConveyorBlock(2, belt()));

	public static Block[] conveyorBlocks() {
		return new Block[] {CONVEYOR_BELT.get(), FAST_CONVEYOR_BELT.get(), EXPRESS_CONVEYOR_BELT.get(), BELT_SPLITTER.get(), FILTER_BELT.get()};
	}

	public static final RegistryObject<Block> MACHINE_CONNECTOR = register("machine_connector", () -> new com.robvanblerk.tieredpower.block.MachineConnectorBlock(machine().noOcclusion()));

	// ---- Phase 44: storage monitor ----
	public static final RegistryObject<Block> STORAGE_MONITOR = register("storage_monitor", () -> new com.robvanblerk.tieredpower.block.StorageMonitorBlock(machine().noOcclusion()));

	// ---- Phase 43: more automation ----
	public static final RegistryObject<Block> BLOCK_BREAKER = register("block_breaker", () -> new com.robvanblerk.tieredpower.block.BlockBreakerBlock(machine()));
	public static final RegistryObject<Block> BLOCK_PLACER = register("block_placer", () -> new com.robvanblerk.tieredpower.block.BlockPlacerBlock(machine()));
	public static final RegistryObject<Block> TREE_FARM = register("tree_farm", () -> new com.robvanblerk.tieredpower.block.TreeFarmBlock(machine()));
	public static final RegistryObject<Block> ANIMAL_RANCH = register("animal_ranch", () -> new com.robvanblerk.tieredpower.block.AnimalRanchBlock(machine()));

	// ---- Phase 42: wireless power ----
	public static final RegistryObject<Block> POWER_TRANSMITTER = register("power_transmitter", () -> new com.robvanblerk.tieredpower.block.PowerTransmitterBlock(machine()));
	public static final RegistryObject<Block> POWER_RECEIVER = register("power_receiver", () -> new com.robvanblerk.tieredpower.block.PowerReceiverBlock(machine()));
	public static final RegistryObject<Item> POWER_LINKER = ITEMS.register("power_linker", () -> new com.robvanblerk.tieredpower.item.PowerLinkerItem(new Item.Properties().stacksTo(1)));

	// ---- Phase 41: smithing and charging in autocrafting ----
	public static final RegistryObject<Block> SMITHING_PRESS = register("smithing_press", () -> new com.robvanblerk.tieredpower.block.SmithingPressBlock(machine()));
	public static final RegistryObject<Item> QUANTUM_POWER_CORE = ITEMS.register("quantum_power_core", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

	// ---- Phase 40: Quantum Suit ----
	public static final RegistryObject<Item> QUANTUM_HELMET = ITEMS.register("quantum_helmet",
			() -> new com.robvanblerk.tieredpower.item.powered.QuantumArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC)));
	public static final RegistryObject<Item> QUANTUM_CHESTPLATE = ITEMS.register("quantum_chestplate",
			() -> new com.robvanblerk.tieredpower.item.powered.QuantumChestplateItem(new Item.Properties().rarity(Rarity.EPIC)));
	public static final RegistryObject<Item> QUANTUM_LEGGINGS = ITEMS.register("quantum_leggings",
			() -> new com.robvanblerk.tieredpower.item.powered.QuantumArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.EPIC)));
	public static final RegistryObject<Item> QUANTUM_BOOTS = ITEMS.register("quantum_boots",
			() -> new com.robvanblerk.tieredpower.item.powered.QuantumArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC)));

	// ---- Phase 38: chemical ore processing ----
	public static final RegistryObject<Block> SALT_EVAPORATOR = register("salt_evaporator", () -> new com.robvanblerk.tieredpower.block.SaltEvaporatorBlock(machine()));
	public static final RegistryObject<Block> BRINE_ELECTROLYZER = register("brine_electrolyzer", () -> new com.robvanblerk.tieredpower.block.BrineElectrolyzerBlock(machine()));
	public static final RegistryObject<Block> CHEMICAL_WASHER = register("chemical_washer", () -> new com.robvanblerk.tieredpower.block.ChemicalWasherBlock(machine()));
	public static final RegistryObject<Item> SALT = ITEMS.register("salt", () -> new Item(new Item.Properties()));

	// ---- Phase 37: reactor extras ----
	public static final RegistryObject<Block> REACTOR_GAUGE = register("reactor_gauge", () -> new com.robvanblerk.tieredpower.block.ReactorGaugeBlock(machine()));
	public static final RegistryObject<Item> NEUTRON_REFLECTOR = ITEMS.register("neutron_reflector", () -> new com.robvanblerk.tieredpower.item.NeutronReflectorItem(new Item.Properties().stacksTo(16)));

	// ---- Phase 36: disk partitioning and storage security ----
	public static final RegistryObject<Block> DISK_WORKBENCH = register("disk_workbench", () -> new com.robvanblerk.tieredpower.block.DiskWorkbenchBlock(machine()));
	public static final RegistryObject<Block> SECURITY_TERMINAL = register("security_terminal", () -> new com.robvanblerk.tieredpower.block.SecurityTerminalBlock(machine()));

	// ---- Phase 35: machine upgrades ----
	public static final RegistryObject<Item> QUARRY_PLANNER = ITEMS.register("quarry_planner", () -> new com.robvanblerk.tieredpower.item.QuarryPlannerItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> MUFFLER = ITEMS.register("muffler", () -> new com.robvanblerk.tieredpower.item.MufflerItem(new Item.Properties().stacksTo(16)));
	/** Advanced, Elite, Ultimate, Quantum - index 0 is the Advanced Installer. */
	public static final java.util.List<RegistryObject<Item>> TIER_INSTALLERS = java.util.List.of(
			ITEMS.register("advanced_installer", () -> new com.robvanblerk.tieredpower.item.TierInstallerItem(1, new Item.Properties().stacksTo(16))),
			ITEMS.register("elite_installer", () -> new com.robvanblerk.tieredpower.item.TierInstallerItem(2, new Item.Properties().stacksTo(16))),
			ITEMS.register("ultimate_installer", () -> new com.robvanblerk.tieredpower.item.TierInstallerItem(3, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))),
			ITEMS.register("quantum_installer", () -> new com.robvanblerk.tieredpower.item.TierInstallerItem(4, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))));

	/** Every installer in tier order (Naquadah is registered with the planet resources). */
	public static java.util.List<Item> allInstallers() {
		java.util.List<Item> out = new java.util.ArrayList<>();
		for (var i : TIER_INSTALLERS) out.add(i.get());
		out.add(NAQUADAH_INSTALLER.get());
		return out;
	}

	// ---- Phase 34: gas system part 2 - oxygen steel, biogas ----
	public static final RegistryObject<Block> OXYGEN_FURNACE = register("oxygen_furnace", () -> new com.robvanblerk.tieredpower.block.OxygenFurnaceBlock(machine()));
	public static final RegistryObject<Block> BIO_DIGESTER = register("bio_digester", () -> new com.robvanblerk.tieredpower.block.BioDigesterBlock(machine()));

	// ---- Phase 33: gas system - fusion fuel, air separation, nitrogen cooling ----
	public static final RegistryObject<Block> ISOTOPE_SEPARATOR = register("isotope_separator", () -> new com.robvanblerk.tieredpower.block.IsotopeSeparatorBlock(machine()));
	public static final RegistryObject<Block> AIR_SEPARATOR = register("air_separator", () -> new com.robvanblerk.tieredpower.block.AirSeparatorBlock(machine()));
	public static final RegistryObject<Block> TRITIUM_BREEDER = register("tritium_breeder", () -> new com.robvanblerk.tieredpower.block.TritiumBreederBlock(machine()));
	public static final RegistryObject<Block> CRYO_INJECTOR = register("cryo_injector", () -> new com.robvanblerk.tieredpower.block.CryoInjectorBlock(machine()));

	// ---- Phase 32: storm caller ----
	public static final RegistryObject<Block> STORM_CALLER = register("storm_caller", () -> new com.robvanblerk.tieredpower.block.StormCallerBlock(machine()));
	public static final RegistryObject<Item> STORM_CHARGE = ITEMS.register("storm_charge", () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

	// ---- Phase 31: new power generation ----
	public static final RegistryObject<Block> LIGHTNING_COLLECTOR = register("lightning_collector", () -> new com.robvanblerk.tieredpower.block.LightningCollectorBlock(
			machine().lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.block.LightningCollectorBlock.LIT) ? 15 : 3)));
	public static final RegistryObject<Block> GEOTHERMAL_GENERATOR = register("geothermal_generator", () -> new com.robvanblerk.tieredpower.block.GeothermalGeneratorBlock(machine()));
	public static final RegistryObject<Block> WATER_WHEEL = register("water_wheel", () -> new com.robvanblerk.tieredpower.block.WaterWheelBlock(machine()));

	// ---- Phase 30: fluids in autocrafting ----
	/** Display-only stand-in for "this much fluid" in pattern slots and plans. Not in the creative tab. */
	public static final RegistryObject<Item> FLUID_DROP = ITEMS.register("fluid_drop", () -> new com.robvanblerk.tieredpower.item.FluidDropItem(new Item.Properties().stacksTo(1)));

	// ---- Phase 29: stock keeper ----
	public static final RegistryObject<Block> STOCK_KEEPER = register("stock_keeper", () -> new com.robvanblerk.tieredpower.block.StockKeeperBlock(machine()));

	// ---- Phase 28: autocrafting ----
	public static final RegistryObject<Item> BLANK_PATTERN = ITEMS.register("blank_pattern", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> CRAFTING_PATTERN = ITEMS.register("crafting_pattern", () -> new com.robvanblerk.tieredpower.item.CraftingPatternItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Block> PATTERN_ENCODER = register("pattern_encoder", () -> new com.robvanblerk.tieredpower.block.PatternEncoderBlock(machine()));
	public static final RegistryObject<Block> MOLECULAR_ASSEMBLER = register("molecular_assembler", () -> new com.robvanblerk.tieredpower.block.MolecularAssemblerBlock(machine().noOcclusion()));
	public static final RegistryObject<Block> CRAFTING_CPU = register("crafting_cpu", () -> new com.robvanblerk.tieredpower.block.CraftingCpuBlock(machine()));

	// ---- Phase 26: access points and creative tools ----
	public static final RegistryObject<Block> ACCESS_POINT = register("wireless_access_point", () -> new com.robvanblerk.tieredpower.block.AccessPointBlock(machine().noOcclusion().forceSolidOn()
			.lightLevel(s -> s.getValue(com.robvanblerk.tieredpower.block.AccessPointBlock.LIT) ? 7 : 0)));
	public static final RegistryObject<Block> CREATIVE_ENERGY_CELL = register("creative_energy_cell", () -> new com.robvanblerk.tieredpower.block.CreativeEnergyCellBlock(machine()));
	/** Effectively unlimited (about 9 quadrillion), yet over a thousand of them can be added up without overflowing. */
	public static final RegistryObject<Item> CREATIVE_STORAGE_DISK = ITEMS.register("creative_storage_disk",
			() -> new com.robvanblerk.tieredpower.item.StorageDiskItem(Long.MAX_VALUE / 1_024, new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
	public static final RegistryObject<Item> CREATIVE_FLUID_DISK = ITEMS.register("creative_fluid_disk",
			() -> new com.robvanblerk.tieredpower.item.FluidDiskItem(Long.MAX_VALUE / 1_024_000, new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

	// ---- Phase 25: wireless terminals ----
	public static final RegistryObject<Item> WIRELESS_TERMINAL = ITEMS.register("wireless_terminal", () -> new com.robvanblerk.tieredpower.item.WirelessTerminalItem(false, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> WIRELESS_CRAFTING_TERMINAL = ITEMS.register("wireless_crafting_terminal", () -> new com.robvanblerk.tieredpower.item.WirelessTerminalItem(true, new Item.Properties().stacksTo(1)));

	// ---- Phase 24: fluid storage, panels, interface ----
	public static final RegistryObject<Block> STORAGE_TERMINAL_PANEL = register("storage_terminal_panel", () -> new com.robvanblerk.tieredpower.block.TerminalPanelBlock(machine().noOcclusion().forceSolidOn(), false));
	public static final RegistryObject<Block> CRAFTING_TERMINAL_PANEL = register("crafting_terminal_panel", () -> new com.robvanblerk.tieredpower.block.TerminalPanelBlock(machine().noOcclusion().forceSolidOn(), true));
	public static final RegistryObject<Block> STORAGE_INTERFACE = register("storage_interface", () -> new com.robvanblerk.tieredpower.block.StorageInterfaceBlock(machine()));
	public static final RegistryObject<Item> FLUID_DISK_4K = ITEMS.register("fluid_disk_4k", () -> new com.robvanblerk.tieredpower.item.FluidDiskItem(4_096, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> FLUID_DISK_16K = ITEMS.register("fluid_disk_16k", () -> new com.robvanblerk.tieredpower.item.FluidDiskItem(16_384, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> FLUID_DISK_64K = ITEMS.register("fluid_disk_64k", () -> new com.robvanblerk.tieredpower.item.FluidDiskItem(65_536, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> FLUID_DISK_256K = ITEMS.register("fluid_disk_256k", () -> new com.robvanblerk.tieredpower.item.FluidDiskItem(262_144, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> FLUID_DISK_1M = ITEMS.register("fluid_disk_1m", () -> new com.robvanblerk.tieredpower.item.FluidDiskItem(1_048_576, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> FLUID_DISK_4M = ITEMS.register("fluid_disk_4m", () -> new com.robvanblerk.tieredpower.item.FluidDiskItem(4_194_304, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

	// ---- Phase 23: storage expansion ----
	public static final RegistryObject<Block> CRAFTING_TERMINAL = register("crafting_terminal", () -> new com.robvanblerk.tieredpower.block.StorageTerminalBlock(machine(), true));
	public static final RegistryObject<Block> IMPORT_BUS = register("import_bus", () -> new com.robvanblerk.tieredpower.block.StorageBusBlock(machine().noOcclusion().forceSolidOn(), true));
	public static final RegistryObject<Block> EXPORT_BUS = register("export_bus", () -> new com.robvanblerk.tieredpower.block.StorageBusBlock(machine().noOcclusion().forceSolidOn(), false));

	// ---- Phase 22: item storage network ----
	public static final RegistryObject<Block> STORAGE_CONTROLLER = register("storage_controller", () -> new com.robvanblerk.tieredpower.block.StorageControllerBlock(machine()));
	public static final RegistryObject<Block> STORAGE_CABLE = register("storage_cable", () -> new com.robvanblerk.tieredpower.block.StorageCableBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.METAL).noOcclusion().forceSolidOn()));
	public static final RegistryObject<Block> DRIVE_BAY = register("drive_bay", () -> new com.robvanblerk.tieredpower.block.DriveBayBlock(machine()));
	public static final RegistryObject<Block> STORAGE_TERMINAL = register("storage_terminal", () -> new com.robvanblerk.tieredpower.block.StorageTerminalBlock(machine()));
	public static final RegistryObject<Item> STORAGE_DISK_4K = ITEMS.register("storage_disk_4k", () -> new com.robvanblerk.tieredpower.item.StorageDiskItem(4_096, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> STORAGE_DISK_16K = ITEMS.register("storage_disk_16k", () -> new com.robvanblerk.tieredpower.item.StorageDiskItem(16_384, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> STORAGE_DISK_64K = ITEMS.register("storage_disk_64k", () -> new com.robvanblerk.tieredpower.item.StorageDiskItem(65_536, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> STORAGE_DISK_256K = ITEMS.register("storage_disk_256k", () -> new com.robvanblerk.tieredpower.item.StorageDiskItem(262_144, new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> STORAGE_DISK_1M = ITEMS.register("storage_disk_1m", () -> new com.robvanblerk.tieredpower.item.StorageDiskItem(1_048_576, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final RegistryObject<Item> STORAGE_DISK_4M = ITEMS.register("storage_disk_4m", () -> new com.robvanblerk.tieredpower.item.StorageDiskItem(4_194_304, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

	// ---- Phase 21: monitoring ----
	public static final RegistryObject<Block> ENERGY_METER = register("energy_meter", () -> new com.robvanblerk.tieredpower.block.EnergyMeterBlock(machine()));
	public static final RegistryObject<Block> POWER_MONITOR = register("power_monitor", () -> new com.robvanblerk.tieredpower.block.PowerMonitorBlock(machine()));
	public static final RegistryObject<Item> MULTIMETER = ITEMS.register("multimeter", () -> new com.robvanblerk.tieredpower.item.MultimeterItem(new Item.Properties().stacksTo(1)));

	// ---- Phase 20: gases ----
	public static final RegistryObject<Block> GAS_ELECTROLYZER = register("gas_electrolyzer", () -> new GasElectrolyzerBlock(machine()));
	public static final RegistryObject<Block> GAS_BURNER_GENERATOR = register("gas_burner_generator", () -> new GasBurnerGeneratorBlock(machine()));
	public static final RegistryObject<Block> STEAM_ENGINE = register("steam_engine", () -> new SteamEngineBlock(machine()));
	public static final RegistryObject<Block> STEAM_HAMMER = register("steam_hammer", () -> new SteamHammerBlock(machine()));
	public static final RegistryObject<Item> HYDROGEN_JETPACK = ITEMS.register("hydrogen_jetpack",
			() -> new com.robvanblerk.tieredpower.item.powered.HydrogenJetpackItem(com.robvanblerk.tieredpower.item.powered.JetpackMaterial.ADVANCED, 64_000, 2, 0.14, 0.7, new Item.Properties()));

	/** One cable block per tier: copper_cable, gold_cable, diamond_cable, netherite_cable. */
	public static final Map<CableTier, RegistryObject<Block>> CABLES = new EnumMap<>(CableTier.class);

	// Thin blocks (cables, pipes, buses, panels) use forceSolidOn() so water and lava stop at them instead of washing
	// them away - by default Minecraft lets fluids break any block smaller than about three quarters of a block.
	static {
		for (CableTier tier : CableTier.values()) {
			CABLES.put(tier, register(tier.getName() + "_cable", () -> new CableBlock(tier,
					BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f)
							.sound(SoundType.COPPER).noOcclusion().forceSolidOn())));
		}
	}

	/** Which creative tab an item belongs in, from its registry name. */
	private static String category(ItemStack stack) {
		String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
		String[] storage = {"storage", "disk", "drive_bay", "terminal", "import_bus", "export_bus", "access_point", "pattern", "assembler", "crafting_cpu", "stock_keeper", "matrix", "pattern_bank", "crafting_accelerator", "machine_connector", "assembler_panel"};
		String[] tools = {"drill", "chainsaw", "jetpack", "wrench", "multimeter", "guide", "upgrade", "charger", "portable_battery", "linker", "installer", "muffler", "planner", "helmet", "chestplate", "leggings", "boots", "_module", "geiger", "iodine", "block_mover", "ore_scanner"};
		String[] power = {"generator", "solar", "turbine", "battery_box", "reactor", "fusion", "fission", "energy_cell", "bank", "lightning", "geothermal",
				"water_wheel", "boiler", "steam_engine", "wind", "magnet_coil", "coolant", "fuel", "casing", "port", "energy_meter", "power_monitor", "storm_caller",
				"breeder", "cryo", "reactor_gauge", "reflector", "plasma_coil", "antimatter", "particle_collider", "rocket", "nose_cone", "hull_plate", "fuel_tank_section", "guidance_computer", "satellite", "cryogenic", "fuel_refinery", "launch_", "receiver_dish", "laser_drill", "laser_lens", "mining_satellite", "power_transmitter", "power_receiver", "power_linker"};
		String[] logistics = {"_cable", "pipe", "fluid_tank", "filter", "sink", "wireless_sender", "wireless_receiver", "buffer", "gas_tank", "gas_cylinder"};
		String[] materials = {"ingot", "dust", "plate", "ore", "nugget", "raw_", "alloy", "storm_charge", "_cell", "wire", "gear", "blank", "power_core"};
		if (id.equals("salt")) return "tools"; // the item - not the Salt Evaporator
		for (String k : storage) if (id.contains(k)) return "storage";
		for (String k : tools) if (id.contains(k)) return "tools";
		for (String k : power) if (id.contains(k)) return "power";
		for (String k : logistics) if (id.contains(k)) return "logistics";
		for (String k : materials) if (id.contains(k)) return "tools";
		return "machines";
	}

	/** Everything, in the order it's listed (each tab shows its own share of this list). */
	private static void allItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
				output.accept(GUIDE_BOOK.get());
				output.accept(WRENCH.get());
				for (var installer : TIER_INSTALLERS) output.accept(installer.get());
				output.accept(MUFFLER.get());
				output.accept(QUARRY_PLANNER.get());
				output.accept(MULTIMETER.get());
				output.accept(STORAGE_CONTROLLER.get());
				output.accept(STORAGE_CABLE.get());
				output.accept(DRIVE_BAY.get());
				output.accept(DISK_WORKBENCH.get());
				output.accept(STORAGE_MONITOR.get());
				output.accept(DIGITAL_FACTORY.get());
				output.accept(FACTORY_CELL.get());
				output.accept(FACTORY_PORT.get());
				output.accept(LOGIC_CONTROLLER.get());
				output.accept(REDSTONE_TRANSMITTER.get());
				output.accept(REDSTONE_RECEIVER.get());
				output.accept(MACHINE_STATUS_DISPLAY.get());
				output.accept(SECURITY_TERMINAL.get());
				output.accept(STORAGE_TERMINAL.get());
				output.accept(CRAFTING_TERMINAL.get());
				output.accept(STORAGE_TERMINAL_PANEL.get());
				output.accept(CRAFTING_TERMINAL_PANEL.get());
				output.accept(STORAGE_INTERFACE.get());
				output.accept(WIRELESS_TERMINAL.get());
				output.accept(WIRELESS_CRAFTING_TERMINAL.get());
				output.accept(ACCESS_POINT.get());
				output.accept(PATTERN_ENCODER.get());
				output.accept(MOLECULAR_ASSEMBLER.get());
				output.accept(ASSEMBLER_PANEL.get());
				output.accept(MACHINE_CONNECTOR.get());
				output.accept(MATRIX_CONTROLLER.get());
				output.accept(MATRIX_CASING.get());
				output.accept(MATRIX_GLASS.get());
				output.accept(PATTERN_BANK.get());
				output.accept(CRAFTING_ACCELERATOR.get());
				output.accept(OVERCLOCK_ACCELERATOR.get());
				output.accept(CRAFTING_CPU.get());
				for (var up : CRAFTING_UPGRADES) output.accept(up.get());
				output.accept(STOCK_KEEPER.get());
				output.accept(BLANK_PATTERN.get());
				output.accept(CREATIVE_ENERGY_CELL.get());
				output.accept(CREATIVE_STORAGE_DISK.get());
				output.accept(CREATIVE_FLUID_DISK.get());
				output.accept(FLUID_DISK_4K.get());
				output.accept(FLUID_DISK_16K.get());
				output.accept(FLUID_DISK_64K.get());
				output.accept(FLUID_DISK_256K.get());
				output.accept(FLUID_DISK_1M.get());
				output.accept(FLUID_DISK_4M.get());
				output.accept(IMPORT_BUS.get());
				output.accept(EXPORT_BUS.get());
				output.accept(STORAGE_DISK_4K.get());
				output.accept(STORAGE_DISK_16K.get());
				output.accept(STORAGE_DISK_64K.get());
				output.accept(STORAGE_DISK_256K.get());
				output.accept(STORAGE_DISK_1M.get());
				output.accept(STORAGE_DISK_4M.get());
				output.accept(ENERGY_METER.get());
				output.accept(POWER_MONITOR.get());
				output.accept(SINK.get());
				output.accept(ELECTRIC_PUMP.get());
				output.accept(BASIC_FLUID_TANK.get());
				output.accept(ADVANCED_FLUID_TANK.get());
				output.accept(ELITE_FLUID_TANK.get());
				output.accept(ULTIMATE_FLUID_TANK.get());
				output.accept(QUANTUM_FLUID_TANK.get());
				output.accept(ROCK_CRUSHER.get());
				output.accept(FREEZER.get());
				output.accept(ORE_PURIFIER.get());
				output.accept(QUARRY.get());
				output.accept(CROP_FARMER.get());
				output.accept(AUTO_CRAFTER.get());
				output.accept(CHUNK_LOADER.get());
				output.accept(DRONE_STATION.get());
				for (var b : new RegistryObject<?>[] {CRYONITE_ORE, CRYONITE_SHARD, CRYONITE_BLOCK, SOLARITE_SAND, SOLAR_GLASS, LIVINGWOOD_LOG, LIVINGWOOD_PLANKS,
						NAQUADAH_ORE, RAW_NAQUADAH, NAQUADAH_DUST, NAQUADAH_INGOT, NAQUADAH_BLOCK, ABYSSAL_PEARL_ORE, ABYSSAL_PEARL, AETHER_CRYSTAL_ORE, AETHER_CRYSTAL,
						SPORECAP, RESONANCE_CRYSTAL_ORE, RESONANCE_CRYSTAL, SPEED_UPGRADE_2, EFFICIENCY_UPGRADE_2, NAQUADAH_INSTALLER, STELLAR_SOLAR_PANEL,
						LIFEBLOOM, SUNSTONE_ORE, SUNSTONE, AMBER_ORE, AMBER, WITCHROOT, GRAVITITE_ORE, GRAVITITE, SOUL_CRYSTAL_ORE, SOUL_CRYSTAL, UMBRAL_ORE, UMBRAL_SHARD, VOIDSTONE_ORE, VOID_SHARD, VOID_PEARL})
					output.accept((net.minecraft.world.level.ItemLike) b.get());
				output.accept(STARGATE_FRAME.get());
				output.accept(STARGATE_DIALER.get());
				output.accept(QUANTUM_CHEST.get());
				output.accept(QUANTUM_LINKER.get());
				output.accept(GUARDIAN_ALTAR.get());
				output.accept(GUARDIAN_HEART.get());
				for (com.robvanblerk.tieredpower.stargate.Planet p : com.robvanblerk.tieredpower.stargate.Planet.hidden())
					output.accept(com.robvanblerk.tieredpower.stargate.AddressTabletItem.of(p));
				output.accept(ELEVATOR.get());
				output.accept(ITEM_MAGNET.get());
				output.accept(ADVANCED_ITEM_MAGNET.get());
				output.accept(SPATIAL_PROJECTOR.get());
				output.accept(SPATIAL_CELL_SMALL.get());
				output.accept(SPATIAL_CELL_MEDIUM.get());
				output.accept(SPATIAL_CELL_LARGE.get());
				output.accept(TURBINE_CONTROLLER.get());
				output.accept(TURBINE_CASING.get());
				output.accept(TURBINE_GLASS.get());
				output.accept(TURBINE_VALVE.get());
				output.accept(TURBINE_ROTOR.get());
				output.accept(UTILITY_DRONE.get());
				output.accept(DRONE_REMOTE.get());
				output.accept(WIRELESS_CHARGER.get());
				output.accept(ENCHANTING_MACHINE.get());
				output.accept(MOB_GRINDER.get());
				output.accept(SPAWNER_CONTROLLER.get());
				output.accept(FLUID_MIXER.get());
				output.accept(TELEPORTER.get());
				output.accept(CONDENSER.get());
				output.accept(GAS_ELECTROLYZER.get());
				output.accept(GAS_BURNER_GENERATOR.get());
				output.accept(STEAM_ENGINE.get());
				output.accept(STEAM_HAMMER.get());
				output.accept(HYDROGEN_JETPACK.get());
				output.accept(QUANTUM_POWER_CORE.get());
				for (var mod : SUIT_MODULES.values()) output.accept(mod.get());
				output.accept(HAZMAT_HELMET.get());
				output.accept(HAZMAT_CHESTPLATE.get());
				output.accept(HAZMAT_LEGGINGS.get());
				output.accept(HAZMAT_BOOTS.get());
				output.accept(GEIGER_COUNTER.get());
				output.accept(BLOCK_MOVER.get());
				output.accept(ORE_SCANNER.get());
				output.accept(IODINE_TABLETS.get());
				for (var piece : java.util.List.of(QUANTUM_HELMET, QUANTUM_CHESTPLATE, QUANTUM_LEGGINGS, QUANTUM_BOOTS)) {
					output.accept(piece.get());
					ItemStack full = new ItemStack(piece.get());
					com.robvanblerk.tieredpower.item.powered.ItemEnergy.set(full, piece == QUANTUM_CHESTPLATE ? 20_000_000 : 10_000_000);
					output.accept(full);
				}
				output.accept(TELEPORTER_LINKER.get());
				output.accept(SOLAR_PANEL.get());
				output.accept(ADVANCED_SOLAR_PANEL.get());
				output.accept(ELITE_SOLAR_PANEL.get());
				output.accept(ULTIMATE_SOLAR_PANEL.get());
				output.accept(LIGHTNING_COLLECTOR.get());
				output.accept(POWER_TRANSMITTER.get());
				output.accept(POWER_RECEIVER.get());
				output.accept(POWER_LINKER.get());
				output.accept(GAS_TANK.get());
				output.accept(GAS_CYLINDER.get());
				for (var gas : java.util.List.of(ModFluids.OXYGEN, ModFluids.HYDROGEN, ModFluids.NITROGEN, ModFluids.METHANE, ModFluids.CHLORINE,
						ModFluids.DEUTERIUM, ModFluids.TRITIUM, ModFluids.STEAM, ModFluids.ANTIMATTER))
					output.accept(com.robvanblerk.tieredpower.item.GasCylinderItem.filled(GAS_CYLINDER.get(), gas.get()));
				output.accept(WIRELESS_SENDER.get());
				output.accept(WIRELESS_RECEIVER.get());
				output.accept(ITEM_BUFFER.get());
				output.accept(FLUID_BUFFER.get());
				output.accept(STORM_CALLER.get());
				output.accept(ISOTOPE_SEPARATOR.get());
				output.accept(AIR_SEPARATOR.get());
				output.accept(TRITIUM_BREEDER.get());
				output.accept(CRYO_INJECTOR.get());
				output.accept(REACTOR_GAUGE.get());
				for (var coil : PLASMA_COILS) output.accept(coil.get());
				output.accept(PARTICLE_COLLIDER.get());
				output.accept(ANTIMATTER_REACTOR.get());
				output.accept(ENERGY_CORE.get());
				output.accept(INPUT_PYLON.get());
				output.accept(OUTPUT_PYLON.get());
				for (var up : CORE_UPGRADES) output.accept(up.get());
				output.accept(CRYOGENIC_CONDENSER.get());
				output.accept(FUEL_REFINERY.get());
				output.accept(LAUNCH_PAD.get());
				output.accept(LAUNCH_TOWER.get());
				output.accept(LAUNCH_CONTROLLER.get());
				output.accept(RECEIVER_DISH.get());
				output.accept(LASER_DRILL.get());
				output.accept(MINING_SATELLITE.get());
				for (var lens : LASER_LENSES) output.accept(lens.get());
				output.accept(LIQUID_METHANE_BUCKET.get());
				output.accept(LIQUID_OXYGEN_BUCKET.get());
				output.accept(ROCKET_FUEL_BUCKET.get());
				output.accept(HULL_PLATE.get());
				output.accept(ROCKET_NOZZLE.get());
				output.accept(ROCKET_ENGINE.get());
				output.accept(FUEL_TANK_SECTION.get());
				output.accept(GUIDANCE_COMPUTER.get());
				output.accept(NOSE_CONE.get());
				output.accept(ROCKET.get());
				output.accept(SOLAR_SATELLITE.get());
				output.accept(NEUTRON_REFLECTOR.get());
				output.accept(OXYGEN_FURNACE.get());
				output.accept(BIO_DIGESTER.get());
				output.accept(SMITHING_PRESS.get());
				output.accept(BREWING_MACHINE.get());
				output.accept(COKE_OVEN.get());
				output.accept(INDUSTRIAL_BLAST_FURNACE.get());
				output.accept(BIO_REFINERY.get());
				output.accept(FLUIDIC_PLENISHER.get());
				output.accept(SPRINKLER.get());
				output.accept(GROW_LAMP.get());
				output.accept(GREENHOUSE_GLASS.get());
				output.accept(FERTILIZER.get());
				output.accept(SUGAR_CANE_SEEDS.get());
				output.accept(IRRIGATED_SAND.get());
				output.accept(BLOCK_BREAKER.get());
				output.accept(BLOCK_PLACER.get());
				output.accept(TREE_FARM.get());
				output.accept(ANIMAL_RANCH.get());
				output.accept(DIGITAL_MINER.get());
				output.accept(SALT_EVAPORATOR.get());
				output.accept(BRINE_ELECTROLYZER.get());
				output.accept(CHEMICAL_WASHER.get());
				output.accept(SALT.get());
				output.accept(STORM_CHARGE.get());
				output.accept(GEOTHERMAL_GENERATOR.get());
				output.accept(WATER_WHEEL.get());
				output.accept(QUANTUM_SOLAR_PANEL.get());
				output.accept(WIND_TURBINE.get());
				output.accept(FISSION_REACTOR.get());
				output.accept(FISSION_CONTROLLER.get());
				output.accept(FISSION_CASING.get());
				output.accept(FISSION_GLASS.get());
				output.accept(FUEL_ASSEMBLY.get());
				output.accept(COOLANT_CHANNEL.get());
				output.accept(FISSION_FUEL_PORT.get());
				output.accept(FISSION_WASTE_PORT.get());
				output.accept(FISSION_COOLANT_PORT.get());
				output.accept(FISSION_POWER_PORT.get());
				output.accept(URANIUM_ORE.get());
				output.accept(DEEPSLATE_URANIUM_ORE.get());
				output.accept(RAW_URANIUM.get());
				output.accept(URANIUM_INGOT.get());
				output.accept(URANIUM_DUST.get());
				output.accept(URANIUM_FUEL_ROD.get());
				output.accept(DEPLETED_FUEL_ROD.get());
				output.accept(PLUTONIUM_DUST.get());
				output.accept(PLUTONIUM_INGOT.get());
				output.accept(MOX_FUEL_ROD.get());
				output.accept(ELECTRIC_DRILL.get());
				output.accept(ADVANCED_DRILL.get());
				output.accept(QUANTUM_DRILL.get());
				ItemStack fullDrill = new ItemStack(QUANTUM_DRILL.get());
				com.robvanblerk.tieredpower.item.powered.ItemEnergy.set(fullDrill, com.robvanblerk.tieredpower.item.powered.QuantumDrillItem.CAPACITY);
				output.accept(fullDrill);
				output.accept(FLUID_FILTER.get());
				output.accept(ELECTRIC_CHAINSAW.get());
				output.accept(JETPACK.get());
				output.accept(ADVANCED_JETPACK.get());
				output.accept(PORTABLE_BATTERY.get());
				output.accept(ADVANCED_PORTABLE_BATTERY.get());
				output.accept(COMPRESSOR.get());
				output.accept(ELECTRIC_SAWMILL.get());
				output.accept(IRON_PLATE.get());
				output.accept(GOLD_PLATE.get());
				output.accept(COPPER_PLATE.get());
				output.accept(STEEL_PLATE.get());
				output.accept(SAWDUST.get());
				output.accept(COAL_GENERATOR.get());
				output.accept(DIESEL_GENERATOR.get());
				output.accept(BATTERY_BOX.get());
				output.accept(ADVANCED_BATTERY_BOX.get());
				output.accept(ELITE_BATTERY_BOX.get());
				output.accept(ULTIMATE_BATTERY_BOX.get());
				output.accept(QUANTUM_BATTERY_BOX.get());
				output.accept(ELECTRIC_FURNACE.get());
				output.accept(BOILER.get());
				output.accept(STEAM_TURBINE.get());
				output.accept(STEEL_INGOT.get());
				output.accept(COAL_COKE.get());
				output.accept(CREOSOTE_BUCKET.get());
				output.accept(BIODIESEL_BUCKET.get());
				output.accept(EXPERIENCE_BUCKET.get());
				output.accept(TREATED_PLANKS.get());
				output.accept(LITHIUM_ORE.get());
				output.accept(DEEPSLATE_LITHIUM_ORE.get());
				output.accept(RAW_LITHIUM.get());
				output.accept(LITHIUM_INGOT.get());
				output.accept(EMPTY_FUEL_CELL.get());
				output.accept(DEUTERIUM_CELL.get());
				output.accept(TRITIUM_CELL.get());
				output.accept(ELECTROLYZER.get());
				output.accept(FUSION_REACTOR.get());
				output.accept(PULVERIZER.get());
				output.accept(ALLOY_SMELTER.get());
				output.accept(CHARGER.get());
				output.accept(IRON_DUST.get());
				output.accept(GOLD_DUST.get());
				output.accept(COPPER_DUST.get());
				output.accept(LITHIUM_DUST.get());
				output.accept(SUPERCONDUCTOR_INGOT.get());
				output.accept(QUANTUM_ALLOY_INGOT.get());
				output.accept(SPEED_UPGRADE.get());
				output.accept(FUSION_CONTROLLER.get());
				output.accept(REACTOR_CASING.get());
				output.accept(REACTOR_GLASS.get());
				output.accept(MAGNET_COIL.get());
				output.accept(REACTOR_FUEL_PORT.get());
				output.accept(REACTOR_POWER_PORT.get());
				output.accept(BANK_CONTROLLER.get());
				output.accept(BANK_CASING.get());
				output.accept(BANK_GLASS.get());
				output.accept(BANK_PORT.get());
				output.accept(ENERGY_CELL.get());
				output.accept(ADVANCED_ENERGY_CELL.get());
				output.accept(ULTIMATE_ENERGY_CELL.get());
				output.accept(QUANTUM_ENERGY_CELL.get());
				output.accept(EFFICIENCY_UPGRADE.get());
				for (CableTier tier : CableTier.values()) output.accept(CABLES.get(tier).get());
				for (PipeTier.Kind kind : PipeTier.Kind.values()) for (PipeTier tier : PipeTier.values()) output.accept(PIPES.get(kind).get(tier).get());
				for (ItemPipeTier tier : ItemPipeTier.values()) output.accept(ITEM_PIPES.get(tier).get());
				for (Block belt : conveyorBlocks()) output.accept(belt);
				output.accept(ITEM_FILTER.get());
	}

	private static CreativeModeTab.Output only(String tab, CreativeModeTab.Output output) {
		return (stack, visibility) -> {
			if (category(stack).equals(tab)) output.accept(stack, visibility);
		};
	}

	private static RegistryObject<CreativeModeTab> tab(String name, String category, java.util.function.Supplier<ItemStack> icon, @org.jetbrains.annotations.Nullable String after) {
		return TABS.register(name, () -> {
			CreativeModeTab.Builder b = CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tieredpower." + name))
					.icon(icon)
					.displayItems((params, output) -> allItems(params, only(category, output)));
			if (after != null) b.withTabsBefore(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, after));
			return b.build();
		});
	}

	public static final RegistryObject<CreativeModeTab> TAB = tab("main", "power", () -> new ItemStack(COAL_GENERATOR.get()), null);
	public static final RegistryObject<CreativeModeTab> TAB_MACHINES = tab("machines", "machines", () -> new ItemStack(PULVERIZER.get()), "main");
	public static final RegistryObject<CreativeModeTab> TAB_LOGISTICS = tab("logistics", "logistics", () -> new ItemStack(CABLES.get(CableTier.GOLD).get()), "machines");
	public static final RegistryObject<CreativeModeTab> TAB_STORAGE = tab("storage", "storage", () -> new ItemStack(STORAGE_TERMINAL.get()), "logistics");
	public static final RegistryObject<CreativeModeTab> TAB_TOOLS = tab("tools", "tools", () -> new ItemStack(WRENCH.get()), "storage");

	private static RegistryObject<Block> register(String name, Supplier<Block> block) {
		RegistryObject<Block> registered = BLOCKS.register(name, block);
		ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
		return registered;
	}

	/** All cable blocks, used when creating the cable block entity type. */
	public static Block[] cableBlocks() {
		return CABLES.values().stream().map(RegistryObject::get).toArray(Block[]::new);
	}

	private ModBlocks() {}
}
