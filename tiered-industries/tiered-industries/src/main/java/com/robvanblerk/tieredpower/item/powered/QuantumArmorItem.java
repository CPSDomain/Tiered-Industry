package com.robvanblerk.tieredpower.item.powered;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * Quantum Suit helmet, leggings and boots. Each stores its own energy (charge it in a Charger or Wireless Charger).
 * Helmet: night vision and water breathing. Leggings: speed. Boots: no fall damage, step up full blocks. Every piece
 * also powers the suit's energy shield (see QuantumSuitEvents).
 */
public class QuantumArmorItem extends ArmorItem {
	public static final int CAPACITY = 10_000_000;
	private static final UUID STEP_ID = UUID.fromString("6f1a3c2e-9b44-4f0d-a1b7-5c1d2e7f9a10");

	public QuantumArmorItem(ArmorItem.Type type, Properties properties) {
		super(QuantumArmorMaterial.INSTANCE, type, properties.stacksTo(1).fireResistant());
	}

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return ItemEnergy.provider(stack, CAPACITY, CAPACITY / 50, false);
	}

	/** Boots: step up a full block. */
	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
		if (getType() != ArmorItem.Type.BOOTS || slot != EquipmentSlot.FEET) return base;
		ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
		b.putAll(base);
		b.put(ForgeMod.STEP_HEIGHT_ADDITION.get(), new AttributeModifier(STEP_ID, "Quantum step assist", 0.5, AttributeModifier.Operation.ADDITION));
		return b.build();
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide() || !(entity instanceof Player player) || player.getItemBySlot(getEquipmentSlot()) != stack) return;
		if (player.tickCount % 20 != 0) return; // once a second
		switch (getType()) {
			case HELMET -> {
				if (ItemEnergy.use(stack, 400)) { // 20 FE/t
					player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
					player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false, false));
				}
			}
			case LEGGINGS -> {
				if (ItemEnergy.use(stack, 200)) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false, false)); // 10 FE/t
			}
			default -> {}
		}
	}

	@Override public boolean isBarVisible(ItemStack stack) { return true; }
	@Override public int getBarWidth(ItemStack stack) { return ItemEnergy.barWidth(stack, CAPACITY); }
	@Override public int getBarColor(ItemStack stack) { return ItemEnergy.BAR_COLOUR; }

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		ItemEnergy.tooltip(stack, CAPACITY, tooltip);
		String power = switch (getType()) {
			case HELMET -> "Night vision and water breathing (20 FE/t)";
			case LEGGINGS -> "Speed boost (10 FE/t)";
			case BOOTS -> "No fall damage, and step up full blocks";
			default -> "";
		};
		if (!power.isEmpty()) tooltip.add(Component.literal(power).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.literal("Energy shield: each charged piece absorbs 22.5% of damage").withStyle(ChatFormatting.GRAY));
	}
}
