package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Not a real item: stands for "this much of this fluid" in pattern slots and plans (so fluids can use the normal item
 * slots and lists). Screens draw the fluid itself over it.
 */
public class FluidDropItem extends Item {
	public FluidDropItem(Properties properties) {
		super(properties);
	}

	public static ItemStack of(FluidStack fluid) {
		ItemStack stack = new ItemStack(ModBlocks.FLUID_DROP.get());
		if (!fluid.isEmpty()) stack.getOrCreateTag().put("Fluid", fluid.writeToNBT(new net.minecraft.nbt.CompoundTag()));
		return stack;
	}

	public static FluidStack fluid(ItemStack stack) {
		if (!(stack.getItem() instanceof FluidDropItem) || stack.getTag() == null) return FluidStack.EMPTY;
		return FluidStack.loadFluidStackFromNBT(stack.getTag().getCompound("Fluid"));
	}

	@Override
	public Component getName(ItemStack stack) {
		FluidStack f = fluid(stack);
		return f.isEmpty() ? super.getName(stack) : f.getDisplayName();
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		FluidStack f = fluid(stack);
		if (!f.isEmpty()) tooltip.add(Component.literal(String.format("%,d mB", f.getAmount())).withStyle(ChatFormatting.AQUA));
	}
}
