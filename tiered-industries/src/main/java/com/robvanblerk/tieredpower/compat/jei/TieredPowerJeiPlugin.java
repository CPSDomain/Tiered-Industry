package com.robvanblerk.tieredpower.compat.jei;

import java.util.List;
import java.util.Objects;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.entity.ElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.client.screen.AlloySmelterScreen;
import com.robvanblerk.tieredpower.client.screen.BoilerScreen;
import com.robvanblerk.tieredpower.client.screen.CoalGeneratorScreen;
import com.robvanblerk.tieredpower.client.screen.ElectricFurnaceScreen;
import com.robvanblerk.tieredpower.client.screen.ElectrolyzerScreen;
import com.robvanblerk.tieredpower.client.screen.PulverizerScreen;
import com.robvanblerk.tieredpower.energy.CableTier;
import com.robvanblerk.tieredpower.recipe.AlloyingRecipe;
import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.block.entity.OrePurifierBlockEntity;
import com.robvanblerk.tieredpower.block.entity.CompressorBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ElectricSawmillBlockEntity;
import com.robvanblerk.tieredpower.client.screen.ProcessingMachineScreen;
import com.robvanblerk.tieredpower.recipe.PulverizingRecipe;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModRecipes;
import com.robvanblerk.tieredpower.registry.ModTags;

/**
 * JEI integration. JEI finds this class by the @JeiPlugin annotation; if JEI isn't installed it's never loaded,
 * so Tiered Industries works fine without JEI.
 */
@JeiPlugin
public class TieredPowerJeiPlugin implements IModPlugin {
	public static final RecipeType<PulverizingRecipe> PULVERIZING =
			RecipeType.create(TieredPower.MOD_ID, "pulverizing", PulverizingRecipe.class);
	public static final RecipeType<AlloyingRecipe> ALLOYING =
			RecipeType.create(TieredPower.MOD_ID, "alloying", AlloyingRecipe.class);
	public static final RecipeType<ElectrolyzingCategory.Display> ELECTROLYZING =
			RecipeType.create(TieredPower.MOD_ID, "electrolyzing", ElectrolyzingCategory.Display.class);
	public static final RecipeType<FusionCategory.Display> FUSION =
			RecipeType.create(TieredPower.MOD_ID, "fusion", FusionCategory.Display.class);

	public static final RecipeType<GasMachineCategory.Display> CHEMICAL_WASHING = RecipeType.create(TieredPower.MOD_ID, "chemical_washing", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> OXYGEN_STEEL = RecipeType.create(TieredPower.MOD_ID, "oxygen_steel", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> SALT_EVAPORATING = RecipeType.create(TieredPower.MOD_ID, "salt_evaporating", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> BRINE_ELECTROLYSIS = RecipeType.create(TieredPower.MOD_ID, "brine_electrolysis", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> BIO_DIGESTING = RecipeType.create(TieredPower.MOD_ID, "bio_digesting", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> ISOTOPE_SEPARATING = RecipeType.create(TieredPower.MOD_ID, "isotope_separating", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> AIR_SEPARATING = RecipeType.create(TieredPower.MOD_ID, "air_separating", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> TRITIUM_BREEDING = RecipeType.create(TieredPower.MOD_ID, "tritium_breeding", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> PARTICLE_COLLIDING = RecipeType.create(TieredPower.MOD_ID, "particle_colliding", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> ANTIMATTER = RecipeType.create(TieredPower.MOD_ID, "antimatter", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> CRYO_CONDENSING = RecipeType.create(TieredPower.MOD_ID, "cryo_condensing", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> FUEL_REFINING = RecipeType.create(TieredPower.MOD_ID, "fuel_refining", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> COKING = RecipeType.create(TieredPower.MOD_ID, "coking", GasMachineCategory.Display.class);
	public static final RecipeType<GasMachineCategory.Display> STEEL_BLASTING = RecipeType.create(TieredPower.MOD_ID, "steel_blasting", GasMachineCategory.Display.class);
	public static final RecipeType<MachineRecipe> PURIFYING = RecipeType.create(TieredPower.MOD_ID, "purifying", MachineRecipe.class);
	public static final RecipeType<MachineRecipe> COMPRESSING = RecipeType.create(TieredPower.MOD_ID, "compressing", MachineRecipe.class);
	public static final RecipeType<MachineRecipe> SAWING = RecipeType.create(TieredPower.MOD_ID, "sawing", MachineRecipe.class);
	public static final RecipeType<MachineRecipe> CRUSHING = RecipeType.create(TieredPower.MOD_ID, "crushing", MachineRecipe.class);

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), COKING, new ItemStack(ModBlocks.COKE_OVEN.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), STEEL_BLASTING, new ItemStack(ModBlocks.INDUSTRIAL_BLAST_FURNACE.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), CRYO_CONDENSING, new ItemStack(ModBlocks.CRYOGENIC_CONDENSER.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), FUEL_REFINING, new ItemStack(ModBlocks.FUEL_REFINERY.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), CHEMICAL_WASHING, new ItemStack(ModBlocks.CHEMICAL_WASHER.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), OXYGEN_STEEL, new ItemStack(ModBlocks.OXYGEN_FURNACE.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), SALT_EVAPORATING, new ItemStack(ModBlocks.SALT_EVAPORATOR.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), BRINE_ELECTROLYSIS, new ItemStack(ModBlocks.BRINE_ELECTROLYZER.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), BIO_DIGESTING, new ItemStack(ModBlocks.BIO_DIGESTER.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), ISOTOPE_SEPARATING, new ItemStack(ModBlocks.ISOTOPE_SEPARATOR.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), AIR_SEPARATING, new ItemStack(ModBlocks.AIR_SEPARATOR.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), TRITIUM_BREEDING, new ItemStack(ModBlocks.TRITIUM_BREEDER.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), PARTICLE_COLLIDING, new ItemStack(ModBlocks.PARTICLE_COLLIDER.get())));
		registration.addRecipeCategories(new GasMachineCategory(registration.getJeiHelpers().getGuiHelper(), ANTIMATTER, new ItemStack(ModBlocks.ANTIMATTER_REACTOR.get())));

		IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(
				new PulverizingCategory(helper),
				new AlloyingCategory(helper),
				new ElectrolyzingCategory(helper),
				new FusionCategory(helper),
				new MachineRecipeCategory(helper, PURIFYING, ModBlocks.ORE_PURIFIER.get(), "block.tieredpower.ore_purifier",
						OrePurifierBlockEntity.ENERGY_PER_TICK, 250),
				new MachineRecipeCategory(helper, COMPRESSING, ModBlocks.COMPRESSOR.get(), "block.tieredpower.compressor",
						CompressorBlockEntity.ENERGY_PER_TICK, 0),
				new MachineRecipeCategory(helper, SAWING, ModBlocks.ELECTRIC_SAWMILL.get(), "block.tieredpower.electric_sawmill",
						ElectricSawmillBlockEntity.ENERGY_PER_TICK, 0),
				new MachineRecipeCategory(helper, CRUSHING, ModBlocks.ROCK_CRUSHER.get(), "block.tieredpower.rock_crusher",
						com.robvanblerk.tieredpower.block.entity.RockCrusherBlockEntity.ENERGY_PER_TICK, 0));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		RecipeManager recipes = Objects.requireNonNull(Minecraft.getInstance().level).getRecipeManager();
		registration.addRecipes(PULVERIZING, recipes.getAllRecipesFor(ModRecipes.PULVERIZING.get()));
		registration.addRecipes(ALLOYING, recipes.getAllRecipesFor(ModRecipes.ALLOYING.get()));
		registration.addRecipes(PURIFYING, recipes.getAllRecipesFor(ModRecipes.PURIFYING.get()));
		registration.addRecipes(COMPRESSING, recipes.getAllRecipesFor(ModRecipes.COMPRESSING.get()));
		registration.addRecipes(SAWING, recipes.getAllRecipesFor(ModRecipes.SAWING.get()));
		registration.addRecipes(CRUSHING, recipes.getAllRecipesFor(ModRecipes.CRUSHING.get()));

		// ---- gas and fluid machines ----
		{
			java.util.function.Function<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>, List<ItemStack>> tagItems = tag -> {
				List<ItemStack> out = new java.util.ArrayList<>();
				for (var h : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag)) out.add(new ItemStack(h.value()));
				return out;
			};
			java.util.function.BiFunction<net.minecraft.world.level.material.Fluid, Integer, net.minecraftforge.fluids.FluidStack> fl = (f, n) -> new net.minecraftforge.fluids.FluidStack(f, n);
			var water = net.minecraft.world.level.material.Fluids.WATER;
			var CL = com.robvanblerk.tieredpower.registry.ModFluids.CHLORINE.get();
			var H2 = com.robvanblerk.tieredpower.registry.ModFluids.HYDROGEN.get();
			var O2 = com.robvanblerk.tieredpower.registry.ModFluids.OXYGEN.get();
			var N2 = com.robvanblerk.tieredpower.registry.ModFluids.NITROGEN.get();
			var D = com.robvanblerk.tieredpower.registry.ModFluids.DEUTERIUM.get();
			var T = com.robvanblerk.tieredpower.registry.ModFluids.TRITIUM.get();
			var AM = com.robvanblerk.tieredpower.registry.ModFluids.ANTIMATTER.get();
			var CH4 = com.robvanblerk.tieredpower.registry.ModFluids.METHANE.get();
			// Chemical Washer: every Ore Purifier recipe for an ore or raw ore, one more dust; plus depleted fuel rods.
			List<GasMachineCategory.Display> washing = new java.util.ArrayList<>();
			ItemStack washer = new ItemStack(ModBlocks.CHEMICAL_WASHER.get());
			for (var r : recipes.getAllRecipesFor(com.robvanblerk.tieredpower.registry.ModRecipes.PURIFYING.get())) {
				if (r.getCount() != 1 || r.getResult().isEmpty()) continue;
				List<ItemStack> ores = new java.util.ArrayList<>();
				for (ItemStack s : r.getIngredient().getItems())
					if (s.is(net.minecraftforge.common.Tags.Items.ORES) || s.is(net.minecraftforge.common.Tags.Items.RAW_MATERIALS)) ores.add(s.copyWithCount(1));
				if (ores.isEmpty()) continue;
				washing.add(GasMachineCategory.Display.of(washer, List.of(ores), List.of(fl.apply(CL, 100), fl.apply(water, 500)),
						List.of(r.getResult().copyWithCount(r.getResult().getCount() + 1)), List.of(), "6 s, 300 FE/t"));
			}
			washing.add(GasMachineCategory.Display.of(washer, List.of(List.of(new ItemStack(ModBlocks.DEPLETED_FUEL_ROD.get()))), List.of(fl.apply(CL, 100), fl.apply(water, 500)),
					List.of(new ItemStack(ModBlocks.PLUTONIUM_DUST.get())), List.of(), "Reprocessing: 6 s, 300 FE/t"));
			registration.addRecipes(CHEMICAL_WASHING, washing);
			List<ItemStack> iron = new java.util.ArrayList<>(tagItems.apply(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "ingots/iron"))));
			iron.addAll(tagItems.apply(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "dusts/iron"))));
			registration.addRecipes(OXYGEN_STEEL, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.OXYGEN_FURNACE.get()), List.of(iron), List.of(fl.apply(O2, 100)),
					List.of(new ItemStack(ModBlocks.STEEL_INGOT.get())), List.of(), "3 s, 80 FE/t")));
			registration.addRecipes(SALT_EVAPORATING, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.SALT_EVAPORATOR.get()), List.of(), List.of(fl.apply(water, 1000)),
					List.of(new ItemStack(ModBlocks.SALT.get())), List.of(), "5 s, 40 FE/t")));
			registration.addRecipes(BRINE_ELECTROLYSIS, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.BRINE_ELECTROLYZER.get()),
					List.of(List.of(new ItemStack(ModBlocks.SALT.get()))), List.of(fl.apply(water, 500)), List.of(), List.of(fl.apply(CL, 250), fl.apply(H2, 250)), "2 s, 200 FE/t")));
			registration.addRecipes(BIO_DIGESTING, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.BIO_DIGESTER.get()),
					List.of(tagItems.apply(com.robvanblerk.tieredpower.registry.ModTags.BIOMASS)), List.of(), List.of(), List.of(fl.apply(CH4, 80)), "2 s each, no power")));
			registration.addRecipes(ISOTOPE_SEPARATING, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.ISOTOPE_SEPARATOR.get()), List.of(), List.of(fl.apply(water, 50)),
					List.of(), List.of(fl.apply(D, 1)), "Every tick, 400 FE/t")));
			registration.addRecipes(AIR_SEPARATING, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.AIR_SEPARATOR.get()), List.of(), List.of(),
					List.of(), List.of(fl.apply(N2, 16), fl.apply(O2, 4)), "From air, every tick, 120 FE/t")));
			registration.addRecipes(TRITIUM_BREEDING, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.TRITIUM_BREEDER.get()),
					List.of(tagItems.apply(com.robvanblerk.tieredpower.registry.ModTags.LITHIUM_INGOTS)), List.of(), List.of(), List.of(fl.apply(T, 250)), "20 s, next to a running fission reactor")));
			registration.addRecipes(PARTICLE_COLLIDING, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.PARTICLE_COLLIDER.get()), List.of(),
					List.of(fl.apply(D, 10), fl.apply(T, 10)), List.of(), List.of(fl.apply(AM, 1)), "20,000 FE each")));
			var LM = com.robvanblerk.tieredpower.registry.ModFluids.LIQUID_METHANE.get();
			var LOX = com.robvanblerk.tieredpower.registry.ModFluids.LIQUID_OXYGEN.get();
			var RF = com.robvanblerk.tieredpower.registry.ModFluids.ROCKET_FUEL.get();
			ItemStack condenser = new ItemStack(ModBlocks.CRYOGENIC_CONDENSER.get());
			registration.addRecipes(CRYO_CONDENSING, List.of(
					GasMachineCategory.Display.of(condenser, List.of(), List.of(fl.apply(CH4, 40), fl.apply(N2, 4)), List.of(), List.of(fl.apply(LM, 20)), "200 FE each"),
					GasMachineCategory.Display.of(condenser, List.of(), List.of(fl.apply(O2, 40), fl.apply(N2, 4)), List.of(), List.of(fl.apply(LOX, 20)), "200 FE each")));
			registration.addRecipes(FUEL_REFINING, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.FUEL_REFINERY.get()), List.of(),
					List.of(fl.apply(LM, 10), fl.apply(LOX, 20)), List.of(), List.of(fl.apply(RF, 30)), "100 FE each")));
			var CREO = com.robvanblerk.tieredpower.registry.ModFluids.CREOSOTE.get();
			ItemStack oven = new ItemStack(ModBlocks.COKE_OVEN.get());
			registration.addRecipes(COKING, List.of(
					GasMachineCategory.Display.of(oven, List.of(List.of(new ItemStack(net.minecraft.world.item.Items.COAL))), List.of(), List.of(new ItemStack(ModBlocks.COAL_COKE.get())), List.of(fl.apply(CREO, 250)), "30 s, no power"),
					GasMachineCategory.Display.of(oven, List.of(tagItems.apply(net.minecraft.tags.ItemTags.LOGS_THAT_BURN)), List.of(), List.of(new ItemStack(net.minecraft.world.item.Items.CHARCOAL)), List.of(fl.apply(CREO, 125)), "15 s, no power")));
			registration.addRecipes(STEEL_BLASTING, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.INDUSTRIAL_BLAST_FURNACE.get()),
					List.of(iron.stream().filter(st -> st.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "ingots/iron")))).toList(),
							List.of(new ItemStack(ModBlocks.COAL_COKE.get()))), List.of(), List.of(new ItemStack(ModBlocks.STEEL_INGOT.get())), List.of(), "20 s, no power")));
			registration.addRecipes(ANTIMATTER, List.of(GasMachineCategory.Display.of(new ItemStack(ModBlocks.ANTIMATTER_REACTOR.get()), List.of(),
					List.of(fl.apply(AM, 1)), List.of(), List.of(), "= 50,000 FE (up to 20 mB a tick)")));
		}
		registration.addRecipes(ELECTROLYZING, List.of(
				new ElectrolyzingCategory.Display(List.of(), ElectrolyzerBlockEntity.WATER_PER_DEUTERIUM,
						new ItemStack(ModBlocks.DEUTERIUM_CELL.get()), ElectrolyzerBlockEntity.TICKS_DEUTERIUM,
						ElectrolyzerBlockEntity.TICKS_DEUTERIUM * ElectrolyzerBlockEntity.ENERGY_PER_TICK),
				new ElectrolyzingCategory.Display(List.of(Ingredient.of(ModTags.LITHIUM_INGOTS).getItems()), 0,
						new ItemStack(ModBlocks.TRITIUM_CELL.get()), ElectrolyzerBlockEntity.TICKS_TRITIUM,
						ElectrolyzerBlockEntity.TICKS_TRITIUM * ElectrolyzerBlockEntity.ENERGY_PER_TICK)));

		registration.addRecipes(FUSION, List.of(new FusionCategory.Display()));

		// "Information" pages - press U on these items in JEI.
		info(registration, ModBlocks.COAL_GENERATOR, "coal_generator");
		info(registration, ModBlocks.WRENCH, "wrench");
		info(registration, ModBlocks.SINK, "sink");
		info(registration, ModBlocks.QUARRY, "quarry");
		info(registration, ModBlocks.ITEM_FILTER, "item_filter");
		for (var tier : com.robvanblerk.tieredpower.energy.ItemPipeTier.values()) {
			registration.addIngredientInfo(new ItemStack(ModBlocks.ITEM_PIPES.get(tier).get()), VanillaTypes.ITEM_STACK,
					Component.translatable("jei.tieredpower.info.item_pipe", String.format("%,d", tier.getItemsPerSecond())));
		}
		info(registration, ModBlocks.CROP_FARMER, "crop_farmer");
		info(registration, ModBlocks.AUTO_CRAFTER, "auto_crafter");
		info(registration, ModBlocks.CHUNK_LOADER, "chunk_loader");
		info(registration, ModBlocks.WIRELESS_CHARGER, "wireless_charger");
		info(registration, ModBlocks.SOLAR_PANEL, "solar_panel");
		info(registration, ModBlocks.ADVANCED_SOLAR_PANEL, "solar_panel");
		info(registration, ModBlocks.ELITE_SOLAR_PANEL, "solar_panel");
		info(registration, ModBlocks.ULTIMATE_SOLAR_PANEL, "solar_panel");
		info(registration, ModBlocks.QUANTUM_SOLAR_PANEL, "solar_panel");
		info(registration, ModBlocks.WIND_TURBINE, "wind_turbine");
		info(registration, ModBlocks.FISSION_REACTOR, "fission_reactor");
		info(registration, ModBlocks.ENCHANTING_MACHINE, "enchanting_machine");
		info(registration, ModBlocks.MOB_GRINDER, "mob_grinder");
		info(registration, ModBlocks.SPAWNER_CONTROLLER, "spawner_controller");
		info(registration, ModBlocks.FLUID_MIXER, "fluid_mixer");
		info(registration, ModBlocks.TELEPORTER, "teleporter");
		info(registration, ModBlocks.CONDENSER, "condenser");
		info(registration, ModBlocks.ENERGY_METER, "energy_meter");
		info(registration, ModBlocks.POWER_MONITOR, "power_monitor");
		info(registration, ModBlocks.MULTIMETER, "multimeter");
		info(registration, ModBlocks.STORAGE_CONTROLLER, "storage_controller");
		info(registration, ModBlocks.CRAFTING_TERMINAL, "crafting_terminal");
		info(registration, ModBlocks.STORAGE_TERMINAL_PANEL, "storage_terminal_panel");
		info(registration, ModBlocks.CRAFTING_TERMINAL_PANEL, "crafting_terminal_panel");
		info(registration, ModBlocks.STORAGE_INTERFACE, "storage_interface");
		info(registration, ModBlocks.WIRELESS_TERMINAL, "wireless_terminal");
		info(registration, ModBlocks.WIRELESS_CRAFTING_TERMINAL, "wireless_crafting_terminal");
		info(registration, ModBlocks.ACCESS_POINT, "wireless_access_point");
		info(registration, ModBlocks.PATTERN_ENCODER, "pattern_encoder");
		info(registration, ModBlocks.MOLECULAR_ASSEMBLER, "molecular_assembler");
		info(registration, ModBlocks.CRAFTING_CPU, "crafting_cpu");
		info(registration, ModBlocks.STOCK_KEEPER, "stock_keeper");
		for (var part : java.util.List.of(ModBlocks.MATRIX_CONTROLLER, ModBlocks.MATRIX_CASING, ModBlocks.MATRIX_GLASS, ModBlocks.PATTERN_BANK, ModBlocks.CRAFTING_ACCELERATOR))
			info(registration, part, "matrix_controller");
		info(registration, ModBlocks.MACHINE_CONNECTOR, "machine_connector");
		info(registration, ModBlocks.ASSEMBLER_PANEL, "assembler_panel");
		info(registration, ModBlocks.DISK_WORKBENCH, "disk_workbench");
		info(registration, ModBlocks.STORAGE_MONITOR, "storage_monitor");
		info(registration, ModBlocks.SECURITY_TERMINAL, "security_terminal");
		info(registration, ModBlocks.LIGHTNING_COLLECTOR, "lightning_collector");
		info(registration, ModBlocks.POWER_TRANSMITTER, "power_transmitter");
		info(registration, ModBlocks.POWER_RECEIVER, "power_transmitter");
		info(registration, ModBlocks.POWER_LINKER, "power_transmitter");
		info(registration, ModBlocks.STORM_CALLER, "storm_caller");
		info(registration, ModBlocks.ISOTOPE_SEPARATOR, "isotope_separator");
		info(registration, ModBlocks.AIR_SEPARATOR, "air_separator");
		info(registration, ModBlocks.TRITIUM_BREEDER, "tritium_breeder");
		info(registration, ModBlocks.CRYO_INJECTOR, "cryo_injector");
		info(registration, ModBlocks.REACTOR_GAUGE, "reactor_gauge");
		info(registration, ModBlocks.SALT_EVAPORATOR, "salt_evaporator");
		info(registration, ModBlocks.BRINE_ELECTROLYZER, "brine_electrolyzer");
		info(registration, ModBlocks.CHEMICAL_WASHER, "chemical_washer");
		info(registration, ModBlocks.SALT, "salt");
		info(registration, ModBlocks.NEUTRON_REFLECTOR, "neutron_reflector");
		info(registration, ModBlocks.PARTICLE_COLLIDER, "particle_collider");
		info(registration, ModBlocks.ANTIMATTER_REACTOR, "antimatter_reactor");
		info(registration, ModBlocks.FLUID_FILTER, "fluid_filter");
		info(registration, ModBlocks.WIRELESS_SENDER, "wireless_transport");
		info(registration, ModBlocks.WIRELESS_RECEIVER, "wireless_transport");
		info(registration, ModBlocks.ITEM_BUFFER, "buffer");
		info(registration, ModBlocks.FLUID_BUFFER, "buffer");
		info(registration, ModBlocks.QUANTUM_DRILL, "quantum_drill");
		for (var part : java.util.List.of(ModBlocks.LAUNCH_PAD, ModBlocks.LAUNCH_TOWER, ModBlocks.LAUNCH_CONTROLLER)) info(registration, part, "launch_site");
		info(registration, ModBlocks.RECEIVER_DISH, "receiver_dish");
		info(registration, ModBlocks.LASER_DRILL, "laser_drill");
		info(registration, ModBlocks.FLUIDIC_PLENISHER, "fluidic_plenisher");
		info(registration, ModBlocks.DIGITAL_MINER, "digital_miner");
		info(registration, ModBlocks.COKE_OVEN, "coke_oven");
		info(registration, ModBlocks.LOGIC_CONTROLLER, "logic_controller");
		info(registration, ModBlocks.CONVEYOR_BELT, "conveyor_belt");
		info(registration, ModBlocks.DRONE_STATION, "drone_station");
		info(registration, ModBlocks.TURBINE_CONTROLLER, "turbine_controller");
		info(registration, ModBlocks.SPATIAL_PROJECTOR, "spatial_projector");
		info(registration, ModBlocks.ELEVATOR, "elevator");
		info(registration, ModBlocks.STARGATE_DIALER, "stargate_dialer");
		info(registration, ModBlocks.QUANTUM_CHEST, "quantum_chest");
		info(registration, ModBlocks.QUANTUM_LINKER, "quantum_chest");
		info(registration, ModBlocks.ADDRESS_TABLET, "address_tablet");
		for (var r : java.util.List.of(ModBlocks.ENERGY_CORE, ModBlocks.INPUT_PYLON, ModBlocks.OUTPUT_PYLON)) info(registration, r, "energy_core");
		for (var up : ModBlocks.CORE_UPGRADES) info(registration, up, "energy_core");
		info(registration, ModBlocks.BIO_REFINERY, "bio_refinery");
		info(registration, ModBlocks.DIESEL_GENERATOR, "diesel_generator");
		info(registration, ModBlocks.GAS_CYLINDER, "gas_containers");
		info(registration, ModBlocks.GAS_TANK, "gas_containers");
		info(registration, ModBlocks.INDUSTRIAL_BLAST_FURNACE, "industrial_blast_furnace");
		for (var r : java.util.List.of(ModBlocks.SPRINKLER, ModBlocks.GROW_LAMP, ModBlocks.GREENHOUSE_GLASS, ModBlocks.FERTILIZER)) info(registration, r, "greenhouse");
		info(registration, ModBlocks.BLOCK_MOVER, "block_mover");
		info(registration, ModBlocks.ORE_SCANNER, "ore_scanner");
		for (var r : java.util.List.of(ModBlocks.HAZMAT_HELMET, ModBlocks.HAZMAT_CHESTPLATE, ModBlocks.HAZMAT_LEGGINGS, ModBlocks.HAZMAT_BOOTS, ModBlocks.GEIGER_COUNTER, ModBlocks.IODINE_TABLETS))
			info(registration, r, "radiation");
		for (var lens : ModBlocks.LASER_LENSES) info(registration, lens, "laser_drill");
		for (var mod : ModBlocks.SUIT_MODULES.values()) info(registration, mod, "suit_module");
		for (var coil : ModBlocks.PLASMA_COILS) info(registration, coil, "plasma_coil");
		info(registration, ModBlocks.OXYGEN_FURNACE, "oxygen_furnace");
		info(registration, ModBlocks.BIO_DIGESTER, "bio_digester");
		info(registration, ModBlocks.GEOTHERMAL_GENERATOR, "geothermal_generator");
		info(registration, ModBlocks.WATER_WHEEL, "water_wheel");
		info(registration, ModBlocks.BLANK_PATTERN, "blank_pattern");
		info(registration, ModBlocks.CREATIVE_ENERGY_CELL, "creative_energy_cell");
		info(registration, ModBlocks.CREATIVE_STORAGE_DISK, "creative_storage_disk");
		info(registration, ModBlocks.CREATIVE_FLUID_DISK, "creative_fluid_disk");
		info(registration, ModBlocks.FLUID_DISK_4K, "fluid_disk_4k");
		info(registration, ModBlocks.IMPORT_BUS, "import_bus");
		info(registration, ModBlocks.EXPORT_BUS, "export_bus");
		info(registration, ModBlocks.STORAGE_CABLE, "storage_cable");
		info(registration, ModBlocks.DRIVE_BAY, "drive_bay");
		info(registration, ModBlocks.STORAGE_TERMINAL, "storage_terminal");
		info(registration, ModBlocks.STORAGE_DISK_4K, "storage_disk_4k");

		info(registration, ModBlocks.GAS_ELECTROLYZER, "gas_electrolyzer");
		info(registration, ModBlocks.GAS_BURNER_GENERATOR, "gas_burner_generator");
		info(registration, ModBlocks.STEAM_ENGINE, "steam_engine");
		info(registration, ModBlocks.STEAM_HAMMER, "steam_hammer");
		info(registration, ModBlocks.HYDROGEN_JETPACK, "hydrogen_jetpack");
		info(registration, ModBlocks.TELEPORTER_LINKER, "teleporter");
		info(registration, ModBlocks.FISSION_CONTROLLER, "fission_multiblock");
		info(registration, ModBlocks.FISSION_CASING, "fission_multiblock");
		info(registration, ModBlocks.FISSION_GLASS, "fission_multiblock");
		info(registration, ModBlocks.FUEL_ASSEMBLY, "fuel_assembly");
		info(registration, ModBlocks.COOLANT_CHANNEL, "coolant_channel");
		info(registration, ModBlocks.FISSION_FUEL_PORT, "fission_ports");
		info(registration, ModBlocks.FISSION_WASTE_PORT, "fission_ports");
		info(registration, ModBlocks.FISSION_COOLANT_PORT, "fission_ports");
		info(registration, ModBlocks.FISSION_POWER_PORT, "fission_ports");
		info(registration, ModBlocks.URANIUM_ORE, "uranium_ore");
		info(registration, ModBlocks.DEEPSLATE_URANIUM_ORE, "uranium_ore");
		info(registration, ModBlocks.URANIUM_FUEL_ROD, "fission_reactor");
		info(registration, ModBlocks.MOX_FUEL_ROD, "mox_fuel_rod");
		info(registration, ModBlocks.SMITHING_PRESS, "smithing_press");
		info(registration, ModBlocks.BLOCK_BREAKER, "block_breaker");
		info(registration, ModBlocks.BLOCK_PLACER, "block_placer");
		info(registration, ModBlocks.TREE_FARM, "tree_farm");
		info(registration, ModBlocks.ANIMAL_RANCH, "animal_ranch");
		info(registration, ModBlocks.QUANTUM_POWER_CORE, "quantum_power_core");
		for (var piece : java.util.List.of(ModBlocks.QUANTUM_HELMET, ModBlocks.QUANTUM_CHESTPLATE, ModBlocks.QUANTUM_LEGGINGS, ModBlocks.QUANTUM_BOOTS)) info(registration, piece, "quantum_suit");
		info(registration, ModBlocks.PLUTONIUM_DUST, "plutonium_dust");
		info(registration, ModBlocks.ELECTRIC_DRILL, "electric_drill");
		info(registration, ModBlocks.ADVANCED_DRILL, "advanced_drill");
		info(registration, ModBlocks.ELECTRIC_CHAINSAW, "electric_chainsaw");
		info(registration, ModBlocks.JETPACK, "jetpack");
		info(registration, ModBlocks.ADVANCED_JETPACK, "jetpack");
		info(registration, ModBlocks.PORTABLE_BATTERY, "portable_battery");
		info(registration, ModBlocks.ADVANCED_PORTABLE_BATTERY, "portable_battery");
		info(registration, ModBlocks.ELECTRIC_PUMP, "electric_pump");
		info(registration, ModBlocks.ROCK_CRUSHER, "rock_crusher");
		info(registration, ModBlocks.FREEZER, "freezer");
		info(registration, ModBlocks.BASIC_FLUID_TANK, "fluid_tank");
		info(registration, ModBlocks.ADVANCED_FLUID_TANK, "fluid_tank");
		info(registration, ModBlocks.ELITE_FLUID_TANK, "fluid_tank");
		info(registration, ModBlocks.ULTIMATE_FLUID_TANK, "fluid_tank");
		info(registration, ModBlocks.QUANTUM_FLUID_TANK, "fluid_tank");
		info(registration, ModBlocks.BATTERY_BOX, "battery_box");
		info(registration, ModBlocks.ADVANCED_BATTERY_BOX, "battery_box");
		info(registration, ModBlocks.ELITE_BATTERY_BOX, "battery_box");
		info(registration, ModBlocks.ULTIMATE_BATTERY_BOX, "battery_box");
		info(registration, ModBlocks.QUANTUM_BATTERY_BOX, "battery_box");
		info(registration, ModBlocks.BOILER, "boiler");
		info(registration, ModBlocks.STEAM_TURBINE, "steam_turbine");
		info(registration, ModBlocks.CHARGER, "charger");
		info(registration, ModBlocks.FUSION_REACTOR, "fusion_reactor");
		info(registration, ModBlocks.LITHIUM_ORE, "lithium_ore");
		info(registration, ModBlocks.SPEED_UPGRADE, "upgrades");
		info(registration, ModBlocks.FUSION_CONTROLLER, "fusion_multiblock");
		info(registration, ModBlocks.REACTOR_CASING, "fusion_multiblock");
		info(registration, ModBlocks.REACTOR_GLASS, "fusion_multiblock");
		info(registration, ModBlocks.MAGNET_COIL, "magnet_coil");
		info(registration, ModBlocks.BANK_CONTROLLER, "energy_bank");
		info(registration, ModBlocks.BANK_CASING, "energy_bank");
		info(registration, ModBlocks.BANK_GLASS, "energy_bank");
		info(registration, ModBlocks.BANK_PORT, "bank_port");
		info(registration, ModBlocks.ENERGY_CELL, "energy_cell");
		info(registration, ModBlocks.QUANTUM_ENERGY_CELL, "energy_cell");
		info(registration, ModBlocks.ADVANCED_ENERGY_CELL, "energy_cell");
		info(registration, ModBlocks.ULTIMATE_ENERGY_CELL, "energy_cell");
		info(registration, ModBlocks.REACTOR_FUEL_PORT, "reactor_fuel_port");
		info(registration, ModBlocks.REACTOR_POWER_PORT, "reactor_power_port");
		info(registration, ModBlocks.EFFICIENCY_UPGRADE, "upgrades");
		info(registration, ModBlocks.DEEPSLATE_LITHIUM_ORE, "lithium_ore");
		for (var kind : com.robvanblerk.tieredpower.energy.PipeTier.Kind.values()) {
			for (var tier : com.robvanblerk.tieredpower.energy.PipeTier.values()) {
				registration.addIngredientInfo(new ItemStack(ModBlocks.PIPES.get(kind).get(tier).get()), VanillaTypes.ITEM_STACK,
						Component.translatable("jei.tieredpower.info." + kind.suffix, String.format("%,d", tier.getRate())));
			}
		}
		for (CableTier tier : CableTier.values()) {
			registration.addIngredientInfo(new ItemStack(ModBlocks.CABLES.get(tier).get()), VanillaTypes.ITEM_STACK,
					Component.translatable("jei.tieredpower.info.cable", String.format("%,d", tier.getTransferRate())));
		}
	}

	private static void info(IRecipeRegistration registration, RegistryObject<? extends ItemLike> item, String key) {
		registration.addIngredientInfo(new ItemStack(item.get()), VanillaTypes.ITEM_STACK,
				Component.translatable("jei.tieredpower.info." + key));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.PULVERIZER.get()), PULVERIZING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRAFTING_TERMINAL.get()), mezz.jei.api.constants.RecipeTypes.CRAFTING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRAFTING_TERMINAL_PANEL.get()), mezz.jei.api.constants.RecipeTypes.CRAFTING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ALLOY_SMELTER.get()), ALLOYING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ORE_PURIFIER.get()), PURIFYING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.COMPRESSOR.get()), COMPRESSING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTRIC_SAWMILL.get()), SAWING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROCK_CRUSHER.get()), CRUSHING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTROLYZER.get()), ELECTROLYZING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.FUSION_REACTOR.get()), FUSION);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.FUSION_CONTROLLER.get()), FUSION);
		// Our machines also show up on vanilla recipe pages.
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTRIC_FURNACE.get()), RecipeTypes.SMELTING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.SMITHING_PRESS.get()), RecipeTypes.SMITHING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.BREWING_MACHINE.get()), RecipeTypes.BREWING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRYOGENIC_CONDENSER.get()), CRYO_CONDENSING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.FUEL_REFINERY.get()), FUEL_REFINING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.COKE_OVEN.get()), COKING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.INDUSTRIAL_BLAST_FURNACE.get()), STEEL_BLASTING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.CHEMICAL_WASHER.get()), CHEMICAL_WASHING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.OXYGEN_FURNACE.get()), OXYGEN_STEEL);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.SALT_EVAPORATOR.get()), SALT_EVAPORATING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.BRINE_ELECTROLYZER.get()), BRINE_ELECTROLYSIS);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.BIO_DIGESTER.get()), BIO_DIGESTING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ISOTOPE_SEPARATOR.get()), ISOTOPE_SEPARATING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.AIR_SEPARATOR.get()), AIR_SEPARATING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.TRITIUM_BREEDER.get()), TRITIUM_BREEDING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.PARTICLE_COLLIDER.get()), PARTICLE_COLLIDING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.ANTIMATTER_REACTOR.get()), ANTIMATTER);

		registration.addRecipeCatalyst(new ItemStack(ModBlocks.COAL_GENERATOR.get()), RecipeTypes.FUELING);
		registration.addRecipeCatalyst(new ItemStack(ModBlocks.BOILER.get()), RecipeTypes.FUELING);
	}

	@Override
	public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
		registration.addRecipeTransferHandler(new CraftingTerminalTransferHandler(registration.getTransferHelper()), mezz.jei.api.constants.RecipeTypes.CRAFTING);
		registration.addRecipeTransferHandler(new PatternEncoderTransferHandler(), mezz.jei.api.constants.RecipeTypes.CRAFTING);
		registration.addUniversalRecipeTransferHandler(new PatternEncoderUniversalHandler());
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(com.robvanblerk.tieredpower.client.screen.MatrixControllerScreen.class,
				new mezz.jei.api.gui.handlers.IGuiContainerHandler<com.robvanblerk.tieredpower.client.screen.MatrixControllerScreen>() {
					@Override
					public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(com.robvanblerk.tieredpower.client.screen.MatrixControllerScreen screen) {
						return java.util.List.of(screen.machinesPanel());
					}
				});
		registration.addGenericGuiContainerHandler(com.robvanblerk.tieredpower.client.screen.MachineScreen.class,
				new mezz.jei.api.gui.handlers.IGuiContainerHandler<com.robvanblerk.tieredpower.client.screen.MachineScreen<?>>() {
					@Override
					public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(com.robvanblerk.tieredpower.client.screen.MachineScreen<?> screen) {
						return screen.extraAreas();
					}
				});
		registration.addGenericGuiContainerHandler(com.robvanblerk.tieredpower.client.screen.StorageTerminalScreen.class,
				new mezz.jei.api.gui.handlers.IGuiContainerHandler<com.robvanblerk.tieredpower.client.screen.StorageTerminalScreen<?>>() {
					@Override
					public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(com.robvanblerk.tieredpower.client.screen.StorageTerminalScreen<?> screen) {
						var r = screen.queuePanel();
						return r == null ? java.util.List.of() : java.util.List.of(r);
					}
				});
		// Click the progress arrow / flame in a machine GUI to see its recipes.
		registration.addRecipeClickArea(PulverizerScreen.class, 68, 35, 24, 17, PULVERIZING);
		registration.addRecipeClickArea(AlloySmelterScreen.class, 78, 35, 24, 17, ALLOYING);
		registration.addRecipeClickArea(ProcessingMachineScreen.class, ProcessingMachineScreen.ARROW_X, ProcessingMachineScreen.ARROW_Y, 24, 17,
				PURIFYING, COMPRESSING, SAWING, CRUSHING);
		registration.addRecipeClickArea(ElectrolyzerScreen.class, 62, 37, 24, 17, ELECTROLYZING);
		registration.addRecipeClickArea(ElectricFurnaceScreen.class, 68, 35, 24, 17, RecipeTypes.SMELTING);
		registration.addRecipeClickArea(CoalGeneratorScreen.class, 27, 35, 14, 14, RecipeTypes.FUELING);
		registration.addRecipeClickArea(BoilerScreen.class, 27, 35, 14, 14, RecipeTypes.FUELING);
	}

	/** The Fluid Drop is only a stand-in used inside pattern screens - keep it out of JEI's item list. */
	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
		runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, java.util.List.of(new ItemStack(ModBlocks.FLUID_DROP.get())));
	}
}
