package com.robvanblerk.tieredpower.item.powered;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/** A jetpack that burns hydrogen instead of FE. Refuel it in the Gas Electrolyzer's fuel slot. */
public class HydrogenJetpackItem extends JetpackItem {
	public HydrogenJetpackItem(JetpackMaterial material, int capacityMb, int mbPerTick, double thrust, double maxRise, Properties properties) {
		super(material, capacityMb, mbPerTick, thrust, maxRise, properties);
	}

	public static int getHydrogen(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getInt("Hydrogen");
	}

	public static void setHydrogen(ItemStack stack, int mb) {
		stack.getOrCreateTag().putInt("Hydrogen", Math.max(0, mb));
	}

	/** Adds up to 'mb' of hydrogen; returns how much fitted. */
	public int fill(ItemStack stack, int mb) {
		int fit = Math.max(0, Math.min(mb, capacity - getHydrogen(stack)));
		if (fit > 0) setHydrogen(stack, getHydrogen(stack) + fit);
		return fit;
	}

	@Override
	public int fuel(ItemStack stack) {
		return getHydrogen(stack);
	}

	@Override
	protected boolean consume(ItemStack stack) {
		int h = getHydrogen(stack);
		if (h < costPerTick) return false;
		setHydrogen(stack, h - costPerTick);
		return true;
	}

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return null; // not charged with FE
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13f * Math.min(1f, (float) getHydrogen(stack) / capacity));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xDDE8FF;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(String.format("%,d / %,d mB hydrogen", getHydrogen(stack), capacity)).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.literal("Wear it, then hold jump to fly. " + costPerTick + " mB/t while thrusting.").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Refuel in a Gas Electrolyzer's fuel slot").withStyle(ChatFormatting.GRAY));
	}
}
