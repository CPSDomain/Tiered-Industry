package com.robvanblerk.tieredpower.recipe;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.registry.ModRecipes;

/**
 * One input -> one output, used by the Pulverizer. Defined in JSON files under data/<namespace>/recipes, e.g.
 * { "type": "tieredpower:pulverizing", "ingredient": { "tag": "forge:ores/iron" },
 *   "result": { "item": "tieredpower:iron_dust", "count": 2 }, "time": 100 }
 */
public class PulverizingRecipe implements Recipe<SimpleContainer> {
	public static final int DEFAULT_TIME = 100;

	private final ResourceLocation id;
	private final Ingredient ingredient;
	private final ItemStack result;
	private final int time;

	public PulverizingRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result, int time) {
		this.id = id;
		this.ingredient = ingredient;
		this.result = result;
		this.time = time;
	}

	public Ingredient getIngredient() { return ingredient; }
	public ItemStack getResult() { return result; }
	public int getTime() { return time; }

	@Override
	public boolean matches(SimpleContainer container, Level level) {
		return ingredient.test(container.getItem(0));
	}

	@Override
	public ItemStack assemble(SimpleContainer container, RegistryAccess access) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(RegistryAccess access) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, ingredient);
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.PULVERIZING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return ModRecipes.PULVERIZING.get();
	}

	@Override
	public boolean isSpecial() {
		return true; // keeps it out of the vanilla recipe book
	}

	public static class Serializer implements RecipeSerializer<PulverizingRecipe> {
		@Override
		public PulverizingRecipe fromJson(ResourceLocation id, JsonObject json) {
			Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
			ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
			int time = GsonHelper.getAsInt(json, "time", DEFAULT_TIME);
			return new PulverizingRecipe(id, ingredient, result, time);
		}

		@Override
		public @Nullable PulverizingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			Ingredient ingredient = Ingredient.fromNetwork(buf);
			ItemStack result = buf.readItem();
			int time = buf.readVarInt();
			return new PulverizingRecipe(id, ingredient, result, time);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, PulverizingRecipe recipe) {
			recipe.ingredient.toNetwork(buf);
			buf.writeItem(recipe.result);
			buf.writeVarInt(recipe.time);
		}
	}
}
