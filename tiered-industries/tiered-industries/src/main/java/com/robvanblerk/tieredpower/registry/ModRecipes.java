package com.robvanblerk.tieredpower.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.recipe.AlloyingRecipe;
import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.recipe.PulverizingRecipe;

/** Custom recipe types, so machine recipes are data files that JEI, datapacks and KubeJS can see and add to. */
public final class ModRecipes {
	public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, TieredPower.MOD_ID);
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TieredPower.MOD_ID);

	public static final RegistryObject<RecipeType<PulverizingRecipe>> PULVERIZING =
			TYPES.register("pulverizing", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "pulverizing")));
	public static final RegistryObject<RecipeType<AlloyingRecipe>> ALLOYING =
			TYPES.register("alloying", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "alloying")));

	public static final RegistryObject<RecipeSerializer<PulverizingRecipe>> PULVERIZING_SERIALIZER =
			SERIALIZERS.register("pulverizing", PulverizingRecipe.Serializer::new);
	public static final RegistryObject<RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER =
			SERIALIZERS.register("alloying", AlloyingRecipe.Serializer::new);

	public static final RegistryObject<RecipeType<MachineRecipe>> PURIFYING =
			TYPES.register("purifying", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "purifying")));
	public static final RegistryObject<RecipeType<MachineRecipe>> COMPRESSING =
			TYPES.register("compressing", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "compressing")));
	public static final RegistryObject<RecipeType<MachineRecipe>> SAWING =
			TYPES.register("sawing", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "sawing")));

	public static final RegistryObject<RecipeSerializer<MachineRecipe>> PURIFYING_SERIALIZER =
			SERIALIZERS.register("purifying", () -> new MachineRecipe.Serializer(MachineRecipe.Kind.PURIFYING));
	public static final RegistryObject<RecipeSerializer<MachineRecipe>> COMPRESSING_SERIALIZER =
			SERIALIZERS.register("compressing", () -> new MachineRecipe.Serializer(MachineRecipe.Kind.COMPRESSING));
	public static final RegistryObject<RecipeSerializer<MachineRecipe>> SAWING_SERIALIZER =
			SERIALIZERS.register("sawing", () -> new MachineRecipe.Serializer(MachineRecipe.Kind.SAWING));

	public static final RegistryObject<RecipeType<MachineRecipe>> CRUSHING =
			TYPES.register("crushing", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "crushing")));
	public static final RegistryObject<RecipeSerializer<MachineRecipe>> CRUSHING_SERIALIZER =
			SERIALIZERS.register("crushing", () -> new MachineRecipe.Serializer(MachineRecipe.Kind.CRUSHING));

	private ModRecipes() {}
}
