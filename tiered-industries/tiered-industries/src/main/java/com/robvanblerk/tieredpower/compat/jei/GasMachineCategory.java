package com.robvanblerk.tieredpower.compat.jei;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * One JEI page layout shared by the gas and fluid machines (Chemical Washer, Particle Collider, Oxygen Furnace...):
 * up to three inputs and three outputs, each an item or a fluid, an arrow, and a line of notes. Each machine has its own
 * tab; the Display carries the machine so JEI's + in the Pattern Encoder knows which machine to put on the pattern.
 */
public class GasMachineCategory implements IRecipeCategory<GasMachineCategory.Display> {
	/** itemIns: each entry is one slot's alternatives. */
	public record Display(ItemStack machine, List<List<ItemStack>> itemIns, List<FluidStack> fluidIns, List<ItemStack> itemOuts, List<FluidStack> fluidOuts, String note) {
		public static Display of(ItemStack machine, List<List<ItemStack>> itemIns, List<FluidStack> fluidIns, List<ItemStack> itemOuts, List<FluidStack> fluidOuts, String note) {
			return new Display(machine, itemIns, fluidIns, itemOuts, fluidOuts, note);
		}
	}

	private final RecipeType<Display> type;
	private final Component title;
	private final IDrawable icon;

	public GasMachineCategory(IGuiHelper helper, RecipeType<Display> type, ItemStack machine) {
		this.type = type;
		this.title = machine.getHoverName();
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, machine);
	}

	@Override public RecipeType<Display> getRecipeType() { return type; }
	@Override public Component getTitle() { return title; }
	@Override public int getWidth() { return 170; }
	@Override public int getHeight() { return 40; }
	@Override public IDrawable getIcon() { return icon; }

	private static final int[] IN_X = {6, 26, 46}, OUT_X = {100, 120, 140};
	private static final int SLOT_Y = 6;

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Display r, IFocusGroup focuses) {
		int i = 0;
		for (List<ItemStack> options : r.itemIns()) if (i < 3) builder.addSlot(RecipeIngredientRole.INPUT, IN_X[i++], SLOT_Y).addItemStacks(options);
		for (FluidStack f : r.fluidIns()) if (i < 3) builder.addSlot(RecipeIngredientRole.INPUT, IN_X[i++], SLOT_Y)
				.addIngredient(ForgeTypes.FLUID_STACK, f).setFluidRenderer(Math.max(1, f.getAmount()), false, 16, 16);
		int o = 0;
		for (ItemStack s : r.itemOuts()) if (o < 3) builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X[o++], SLOT_Y).addItemStack(s);
		for (FluidStack f : r.fluidOuts()) if (o < 3) builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X[o++], SLOT_Y)
				.addIngredient(ForgeTypes.FLUID_STACK, f).setFluidRenderer(Math.max(1, f.getAmount()), false, 16, 16);
	}

	@Override
	public void draw(Display r, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
		int ins = Math.min(3, r.itemIns().size() + r.fluidIns().size());
		int outs = Math.min(3, r.itemOuts().size() + r.fluidOuts().size());
		for (int i = 0; i < ins; i++) JeiDraw.slot(g, IN_X[i], SLOT_Y);
		for (int i = 0; i < outs; i++) JeiDraw.slot(g, OUT_X[i], SLOT_Y);
		JeiDraw.emptyArrow(g, 68, SLOT_Y - 1);
		JeiDraw.text(g, r.note(), 6, 28);
	}
}
