package com.robvanblerk.tieredpower.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import com.robvanblerk.tieredpower.menu.FluidFilterMenu;

/**
 * Filters a fluid or gas pipe connection: up to 9 fluids, allow-list or block-list. Right-click in the air to set it up
 * (click its slots with a bucket or tank of each fluid), then right-click a pipe's connection to install it. Sneak +
 * right-click the connection with an empty hand to take it off.
 */
public class FluidFilterItem extends Item {
	public static final int SLOTS = 9;

	public FluidFilterItem(Properties properties) {
		super(properties);
	}

	public static List<FluidStack> getFluids(ItemStack filter) {
		List<FluidStack> out = new ArrayList<>();
		CompoundTag tag = filter.getTag();
		ListTag list = tag == null ? new ListTag() : tag.getList("Fluids", Tag.TAG_COMPOUND);
		for (int i = 0; i < SLOTS; i++) out.add(i < list.size() ? FluidStack.loadFluidStackFromNBT(list.getCompound(i)) : FluidStack.EMPTY);
		return out;
	}

	public static void setFluid(ItemStack filter, int index, FluidStack fluid) {
		List<FluidStack> fluids = getFluids(filter);
		fluids.set(index, fluid.isEmpty() ? FluidStack.EMPTY : new FluidStack(fluid, 1000));
		ListTag list = new ListTag();
		for (FluidStack f : fluids) list.add(f.isEmpty() ? new CompoundTag() : f.writeToNBT(new CompoundTag()));
		filter.getOrCreateTag().put("Fluids", list);
	}

	public static boolean isWhitelist(ItemStack filter) {
		CompoundTag tag = filter.getTag();
		return tag == null || !tag.contains("Whitelist") || tag.getBoolean("Whitelist");
	}

	public static void setWhitelist(ItemStack filter, boolean whitelist) {
		filter.getOrCreateTag().putBoolean("Whitelist", whitelist);
	}

	/** Does this fluid get through? No filter (or an empty allow-list) lets everything through. */
	public static boolean passes(@Nullable ItemStack filter, FluidStack fluid) {
		if (filter == null || filter.isEmpty() || fluid.isEmpty()) return true;
		boolean any = false, match = false;
		for (FluidStack f : getFluids(filter)) {
			if (f.isEmpty()) continue;
			any = true;
			if (f.isFluidEqual(fluid)) match = true;
		}
		if (!any) return true;
		return isWhitelist(filter) == match;
	}

	/** The fluids an allow-list filter wants (to pull those specifically from a tank holding several). */
	public static List<FluidStack> wanted(@Nullable ItemStack filter) {
		List<FluidStack> out = new ArrayList<>();
		if (filter == null || filter.isEmpty() || !isWhitelist(filter)) return out;
		for (FluidStack f : getFluids(filter)) if (!f.isEmpty()) out.add(f);
		return out;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
		if (!level.isClientSide())
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new FluidFilterMenu(id, inventory, stack), Component.translatable("item.tieredpower.fluid_filter")));
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(isWhitelist(stack) ? "Allow only these fluids" : "Block these fluids").withStyle(ChatFormatting.AQUA));
		for (FluidStack f : getFluids(stack)) if (!f.isEmpty()) tooltip.add(Component.literal("  " + f.getDisplayName().getString()).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Right-click a fluid or gas pipe connection to install").withStyle(ChatFormatting.DARK_GRAY));
	}
}
