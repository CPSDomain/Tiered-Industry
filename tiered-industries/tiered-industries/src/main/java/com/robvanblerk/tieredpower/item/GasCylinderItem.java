package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

/**
 * A portable gas bottle: holds 8,000 mB of one gas. Right-click a machine, tank or anything with a gas tank to fill it
 * from there, or to empty it in. Its shoulder is coloured by the gas inside.
 */
public class GasCylinderItem extends Item {
	public static final int CAPACITY = 8_000;

	public GasCylinderItem(Properties properties) {
		super(properties);
	}

	public static FluidStack contents(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null || !tag.contains(FluidHandlerItemStack.FLUID_NBT_KEY) ? FluidStack.EMPTY : FluidStack.loadFluidStackFromNBT(tag.getCompound(FluidHandlerItemStack.FLUID_NBT_KEY));
	}

	/** A cylinder full of this gas (for the creative tab). */
	public static ItemStack filled(Item cylinder, Fluid gas) {
		ItemStack s = new ItemStack(cylinder);
		s.getOrCreateTag().put(FluidHandlerItemStack.FLUID_NBT_KEY, new FluidStack(gas, CAPACITY).writeToNBT(new CompoundTag()));
		return s;
	}

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new FluidHandlerItemStack(stack, CAPACITY) {
			@Override
			public boolean canFillFluidType(FluidStack fluid) {
				return fluid.getFluid().getFluidType().isLighterThanAir(); // gases only
			}
		};
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		if (ctx.getPlayer() == null) return InteractionResult.PASS;
		if (ctx.getLevel().getBlockEntity(ctx.getClickedPos()) == null) return InteractionResult.PASS;
		boolean done = FluidUtil.interactWithFluidHandler(ctx.getPlayer(), ctx.getHand(), ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace());
		return done ? InteractionResult.sidedSuccess(ctx.getLevel().isClientSide()) : InteractionResult.PASS;
	}

	@Override public boolean isBarVisible(ItemStack stack) { return !contents(stack).isEmpty(); }
	@Override public int getBarWidth(ItemStack stack) { return Math.round(13f * contents(stack).getAmount() / CAPACITY); }
	@Override public int getBarColor(ItemStack stack) { return 0x9FD8FF; }

	@Override
	public Component getName(ItemStack stack) {
		FluidStack f = contents(stack);
		return f.isEmpty() ? super.getName(stack) : Component.translatable("item.tieredpower.gas_cylinder").append(" (").append(f.getDisplayName()).append(")");
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		FluidStack f = contents(stack);
		tooltip.add(Component.literal(f.isEmpty() ? "Empty" : String.format("%,d / %,d mB %s", f.getAmount(), CAPACITY, f.getDisplayName().getString())).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.literal("Right-click a machine or tank to fill or empty it (gases only)").withStyle(ChatFormatting.GRAY));
	}
}
