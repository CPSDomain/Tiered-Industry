package com.robvanblerk.tieredpower.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FissionReactorBlockEntity;
import com.robvanblerk.tieredpower.item.powered.SuitModules;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Optional radiation (off unless enabled in the config). Every second a player's exposure is worked out from running
 * fission reactors nearby and radioactive things they carry, reduced by Hazmat pieces (25% each) or the Quantum Suit's
 * Radiation Shielding module (100%), and added to their dose; the dose slowly fades when they're away from it. A high
 * dose brings nausea, weakness, hunger, poison and finally damage. Dying clears it.
 */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Radiation {
	private static final String KEY = "tieredpowerRads";

	public static boolean enabled() {
		return Config.get(Config.RADIATION_ENABLED);
	}

	public static float dose(Player p) {
		return p.getPersistentData().getFloat(KEY);
	}

	public static void setDose(Player p, float dose) {
		p.getPersistentData().putFloat(KEY, Math.max(0, dose));
	}

	/** Radioactivity of one item (rads/s per item). */
	private static float itemRads(ItemStack s) {
		Item i = s.getItem();
		if (i == ModBlocks.DEPLETED_FUEL_ROD.get()) return 2.0f;
		if (i == ModBlocks.PLUTONIUM_DUST.get() || i == ModBlocks.PLUTONIUM_INGOT.get()) return 1.5f;
		if (i == ModBlocks.MOX_FUEL_ROD.get()) return 1.0f;
		if (i == ModBlocks.URANIUM_FUEL_ROD.get()) return 0.5f;
		String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(i).getPath();
		if (path.contains("uranium")) return path.contains("ore") ? 0.05f : 0.3f;
		return 0;
	}

	/** Shielding from what the player wears: 0 (none) to 1 (fully protected). */
	public static float protection(Player p) {
		float prot = 0;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack a = p.getItemBySlot(slot);
			if (a.getItem() instanceof ArmorItem ai && ai.getMaterial() == HazmatMaterial.INSTANCE) prot += 0.25f;
		}
		if (SuitModules.active(p.getItemBySlot(EquipmentSlot.CHEST), SuitModules.Module.RADIATION_SHIELDING)) prot = 1;
		return Math.min(1, prot);
	}

	/** Raw exposure (rads/s) before protection: carried items plus running fission reactors within 16 blocks. */
	public static float exposure(Player p) {
		float rads = 0;
		for (ItemStack s : p.getInventory().items) if (!s.isEmpty()) rads += itemRads(s) * Math.min(s.getCount(), 64);
		for (ItemStack s : p.getInventory().offhand) if (!s.isEmpty()) rads += itemRads(s) * s.getCount();
		BlockPos at = p.blockPosition();
		ChunkPos centre = new ChunkPos(at);
		for (int cx = -1; cx <= 1; cx++) {
			for (int cz = -1; cz <= 1; cz++) {
				if (!p.level().hasChunk(centre.x + cx, centre.z + cz)) continue;
				for (var be : p.level().getChunk(centre.x + cx, centre.z + cz).getBlockEntities().values()) {
					boolean running = be instanceof FissionReactorBlockEntity r && r.isRunning() || be instanceof FissionControllerBlockEntity c && c.isRunning();
					if (!running) continue;
					double d2 = be.getBlockPos().distSqr(at);
					if (d2 > 16 * 16) continue;
					rads += (float) (40.0 / (1.0 + d2 / 9.0));
				}
			}
		}
		return rads * Config.get(Config.RADIATION_STRENGTH) / 100f;
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || event.player.tickCount % 20 != 0) return;
		Player p = event.player;
		if (!enabled() || p.isCreative() || p.isSpectator()) return;
		float taken = exposure(p) * (1 - protection(p));
		float dose = dose(p);
		dose = taken > 0.05f ? dose + taken : Math.max(0, dose - 1.0f);
		setDose(p, dose);
		if (dose >= 100) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false, true));
		if (dose >= 300) {
			p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, true));
			p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, false, true));
		}
		if (dose >= 600) p.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false, true));
		if (dose >= 1000 && p.tickCount % 100 == 0) p.hurt(p.damageSources().magic(), 2.0f);
	}

	private Radiation() {}
}
