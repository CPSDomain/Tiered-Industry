package com.robvanblerk.tieredpower.item.powered;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Quantum Drill: a drill, shovel, axe and hoe in one, very fast, mining a 1x1, 3x3, 5x5 or 7x7 area on the face you
 * look at (shift + right-click to switch). 50,000,000 FE; 400 FE per block.
 */
public class QuantumDrillItem extends ElectricDrillItem {
	public static final int CAPACITY = 50_000_000, COST = 400, MAX_RADIUS = 3;
	public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("tieredpower", "mineable/quantum_drill"));

	public QuantumDrillItem(Properties properties) {
		super(PoweredTier.QUANTUM, MINEABLE, CAPACITY, COST, true, properties.fireResistant());
	}

	@Override
	protected int cost() {
		return com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.QUANTUM_DRILL_FE_PER_BLOCK);
	}

	@Override
	protected int areaRadius(ItemStack stack) {
		return stack.getTag() == null ? 0 : Math.max(0, Math.min(MAX_RADIUS, stack.getTag().getInt("Radius")));
	}

	@Override
	protected void cycleMode(ItemStack stack) {
		stack.getOrCreateTag().putInt("Radius", (areaRadius(stack) + 1) % (MAX_RADIUS + 1));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("Mines anything a pickaxe, shovel, axe or hoe can").withStyle(ChatFormatting.AQUA));
	}
}
