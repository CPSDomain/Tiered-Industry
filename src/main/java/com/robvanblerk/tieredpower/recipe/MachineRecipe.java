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
 * Shared recipe format for the Ore Purifier, Compressor, Electric Sawmill and Rock Crusher:
 * { "type": "tieredpower:compressing", "ingredient": { "tag": "forge:ingots/iron" }, "count": 1,
 *   "result": { "item": "tieredpower:iron_plate" }, "secondary": { "item": "..." }, "time": 100 }
 * "count" (how many inputs are used) and "secondary" (a bonus output) are optional.
 */
public class MachineRecipe implements Recipe<SimpleContainer> {
	public enum Kind {
		PURIFYING(100), COMPRESSING(100), SAWING(60), CRUSHING(60);

		public final int defaultTime;

		Kind(int defaultTime) {
			this.defaultTime = defaultTime;
		}

		public RecipeType<MachineRecipe> type() {
			return switch (this) {
				case PURIFYING -> ModRecipes.PURIFYING.get();
				case COMPRESSING -> ModRecipes.COMPRESSING.get();
				case SAWING -> ModRecipes.SAWING.get();
				case CRUSHING -> ModRecipes.CRUSHING.get();
			};
		}

		public RecipeSerializer<MachineRecipe> serializer() {
			return switch (this) {
				case PURIFYING -> ModRecipes.PURIFYING_SERIALIZER.get();
				case COMPRESSING -> ModRecipes.COMPRESSING_SERIALIZER.get();
				case SAWING -> ModRecipes.SAWING_SERIALIZER.get();
				case CRUSHING -> ModRecipes.CRUSHING_SERIALIZER.get();
			};
		}
	}

	private final Kind kind;
	private final ResourceLocation id;
	private final Ingredient ingredient;
	private final int count;
	private final ItemStack result;
	private final ItemStack secondary;
	private final int time;

	public MachineRecipe(Kind kind, ResourceLocation id, Ingredient ingredient, int count, ItemStack result, ItemStack secondary, int time) {
		this.kind = kind;
		this.id = id;
		this.ingredient = ingredient;
		this.count = count;
		this.result = result;
		this.secondary = secondary;
		this.time = time;
	}

	public Kind getKind() { return kind; }
	public Ingredient getIngredient() { return ingredient; }
	public int getCount() { return count; }
	public ItemStack getResult() { return result; }
	public ItemStack getSecondary() { return secondary; }
	public int getTime() { return time; }

	@Override
	public boolean matches(SimpleContainer container, Level level) {
		ItemStack input = container.getItem(0);
		return ingredient.test(input) && input.getCount() >= count;
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
		return kind.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return kind.type();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	public static class Serializer implements RecipeSerializer<MachineRecipe> {
		private final Kind kind;

		public Serializer(Kind kind) {
			this.kind = kind;
		}

		@Override
		public MachineRecipe fromJson(ResourceLocation id, JsonObject json) {
			Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
			int count = GsonHelper.getAsInt(json, "count", 1);
			ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
			ItemStack secondary = json.has("secondary") ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "secondary")) : ItemStack.EMPTY;
			int time = GsonHelper.getAsInt(json, "time", kind.defaultTime);
			return new MachineRecipe(kind, id, ingredient, count, result, secondary, time);
		}

		@Override
		public @Nullable MachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			Ingredient ingredient = Ingredient.fromNetwork(buf);
			int count = buf.readVarInt();
			ItemStack result = buf.readItem();
			ItemStack secondary = buf.readItem();
			int time = buf.readVarInt();
			return new MachineRecipe(kind, id, ingredient, count, result, secondary, time);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, MachineRecipe recipe) {
			recipe.ingredient.toNetwork(buf);
			buf.writeVarInt(recipe.count);
			buf.writeItem(recipe.result);
			buf.writeItem(recipe.secondary);
			buf.writeVarInt(recipe.time);
		}
	}
}
