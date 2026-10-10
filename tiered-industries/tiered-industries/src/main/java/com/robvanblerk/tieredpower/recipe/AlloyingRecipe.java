package com.robvanblerk.tieredpower.recipe;

import com.google.gson.JsonArray;
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
 * Two inputs (each with a count) -> one output, used by the Alloy Smelter. Inputs can go in either slot.
 * { "type": "tieredpower:alloying",
 *   "inputs": [ { "ingredient": { "tag": "forge:ingots/iron" }, "count": 1 },
 *               { "ingredient": { "tag": "minecraft:coals" },   "count": 2 } ],
 *   "result": { "item": "tieredpower:steel_ingot" }, "time": 200 }
 */
public class AlloyingRecipe implements Recipe<SimpleContainer> {
	public static final int DEFAULT_TIME = 200;

	private final ResourceLocation id;
	private final Ingredient first, second;
	private final int firstCount, secondCount;
	private final ItemStack result;
	private final int time;

	public AlloyingRecipe(ResourceLocation id, Ingredient first, int firstCount, Ingredient second, int secondCount, ItemStack result, int time) {
		this.id = id;
		this.first = first;
		this.firstCount = firstCount;
		this.second = second;
		this.secondCount = secondCount;
		this.result = result;
		this.time = time;
	}

	public Ingredient getFirst() { return first; }
	public Ingredient getSecond() { return second; }
	public int getFirstCount() { return firstCount; }
	public int getSecondCount() { return secondCount; }
	public ItemStack getResult() { return result; }
	public int getTime() { return time; }

	/** How many items to take from slot 0 and slot 1, or null if the slots don't match this recipe. */
	@Nullable
	public int[] consumption(ItemStack a, ItemStack b) {
		if (fits(first, firstCount, a) && fits(second, secondCount, b)) return new int[]{firstCount, secondCount};
		if (fits(first, firstCount, b) && fits(second, secondCount, a)) return new int[]{secondCount, firstCount};
		return null;
	}

	private static boolean fits(Ingredient ingredient, int count, ItemStack stack) {
		return ingredient.test(stack) && stack.getCount() >= count;
	}

	@Override
	public boolean matches(SimpleContainer container, Level level) {
		return consumption(container.getItem(0), container.getItem(1)) != null;
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
		return NonNullList.of(Ingredient.EMPTY, first, second);
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.ALLOYING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return ModRecipes.ALLOYING.get();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	public static class Serializer implements RecipeSerializer<AlloyingRecipe> {
		@Override
		public AlloyingRecipe fromJson(ResourceLocation id, JsonObject json) {
			JsonArray inputs = GsonHelper.getAsJsonArray(json, "inputs");
			if (inputs.size() != 2) throw new IllegalArgumentException("Alloying recipe " + id + " needs exactly 2 inputs");
			JsonObject a = inputs.get(0).getAsJsonObject(), b = inputs.get(1).getAsJsonObject();
			return new AlloyingRecipe(id,
					Ingredient.fromJson(a.get("ingredient")), GsonHelper.getAsInt(a, "count", 1),
					Ingredient.fromJson(b.get("ingredient")), GsonHelper.getAsInt(b, "count", 1),
					ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")),
					GsonHelper.getAsInt(json, "time", DEFAULT_TIME));
		}

		@Override
		public @Nullable AlloyingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			Ingredient first = Ingredient.fromNetwork(buf);
			int firstCount = buf.readVarInt();
			Ingredient second = Ingredient.fromNetwork(buf);
			int secondCount = buf.readVarInt();
			ItemStack result = buf.readItem();
			int time = buf.readVarInt();
			return new AlloyingRecipe(id, first, firstCount, second, secondCount, result, time);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, AlloyingRecipe recipe) {
			recipe.first.toNetwork(buf);
			buf.writeVarInt(recipe.firstCount);
			recipe.second.toNetwork(buf);
			buf.writeVarInt(recipe.secondCount);
			buf.writeItem(recipe.result);
			buf.writeVarInt(recipe.time);
		}
	}
}
