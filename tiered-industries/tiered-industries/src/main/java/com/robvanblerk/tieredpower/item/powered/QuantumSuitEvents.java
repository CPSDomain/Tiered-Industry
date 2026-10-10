package com.robvanblerk.tieredpower.item.powered;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.TieredPower;

/**
 * The Quantum Suit's energy shield and fall protection. Each worn piece with energy absorbs 22.5% of incoming damage
 * (90% for the full suit), at 1,000 FE per point of damage absorbed. Charged Quantum Boots cancel fall damage.
 */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuantumSuitEvents {
	public static final float PER_PIECE = 0.225f;
	public static final int FE_PER_DAMAGE = 1_000;

	private static boolean isQuantum(ItemStack stack) {
		return stack.getItem() instanceof QuantumArmorItem || stack.getItem() instanceof QuantumChestplateItem;
	}

	@SubscribeEvent
	public static void onHurt(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill, the void
		float amount = event.getAmount();
		if (amount <= 0) return;
		int perPieceCost = Math.max(1, Math.round(amount * PER_PIECE * FE_PER_DAMAGE));
		List<ItemStack> charged = new ArrayList<>();
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack s = player.getItemBySlot(slot);
			if (isQuantum(s) && ItemEnergy.get(s) >= perPieceCost) charged.add(s);
		}
		if (charged.isEmpty()) return;
		for (ItemStack s : charged) ItemEnergy.use(s, perPieceCost);
		event.setAmount(amount * (1 - PER_PIECE * charged.size()));
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
		if (boots.getItem() instanceof QuantumArmorItem q && q.getType() == ArmorItem.Type.BOOTS && ItemEnergy.use(boots, 2_000)) {
			event.setCanceled(true);
		}
	}

	private QuantumSuitEvents() {}
}
