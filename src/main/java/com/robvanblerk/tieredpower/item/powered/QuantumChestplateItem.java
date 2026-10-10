package com.robvanblerk.tieredpower.item.powered;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** The Quantum Suit chestplate: heavy armour with a built-in jetpack (the fastest one), and part of the energy shield. */
public class QuantumChestplateItem extends JetpackItem {
	public static final int CAPACITY = 20_000_000;

	public QuantumChestplateItem(Properties properties) {
		super(QuantumArmorMaterial.INSTANCE, CAPACITY, 120, 0.22, 1.1, properties.fireResistant());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("Energy shield: each charged piece absorbs 22.5% of damage").withStyle(ChatFormatting.GRAY));
	}
}
