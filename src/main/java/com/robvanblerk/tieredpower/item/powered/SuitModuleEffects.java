package com.robvanblerk.tieredpower.item.powered;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.item.powered.SuitModules.Module;

/**
 * What the Quantum Suit modules do, every tick. Movement (water walking) runs on both sides so it feels smooth; the
 * rest runs on the server, paid for from the piece the module is fitted to.
 */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SuitModuleEffects {
	private static final String FLIGHT_FLAG = "tieredpowerSuitFlight";

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Player p = event.player;
		ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD), chest = p.getItemBySlot(EquipmentSlot.CHEST);
		ItemStack legs = p.getItemBySlot(EquipmentSlot.LEGS), feet = p.getItemBySlot(EquipmentSlot.FEET);

		// Water walking: keep the player on the surface unless they sneak.
		if (SuitModules.active(feet, Module.WATER_WALKING) && !p.isShiftKeyDown()) {
			BlockPos at = p.blockPosition();
			boolean waterHere = p.level().getFluidState(at).is(FluidTags.WATER);
			boolean waterBelow = p.level().getFluidState(BlockPos.containing(p.getX(), p.getY() - 0.05, p.getZ())).is(FluidTags.WATER);
			boolean airAbove = p.level().getFluidState(at.above()).isEmpty();
			Vec3 v = p.getDeltaMovement();
			if (waterHere && airAbove) {
				p.setDeltaMovement(v.x, Math.max(v.y, 0.12), v.z); // float up to the surface
				p.fallDistance = 0;
			} else if (!waterHere && waterBelow && v.y < 0) {
				p.setDeltaMovement(v.x, 0, v.z); // stand on it
				p.setOnGround(true);
				p.fallDistance = 0;
			}
			if (!p.level().isClientSide() && p.tickCount % 20 == 0 && (waterHere || waterBelow)) ItemEnergy.use(feet, 100);
		}

		if (p.level().isClientSide()) return;

		// Flight: like creative flight while the chestplate has charge.
		boolean wantFlight = SuitModules.active(chest, Module.FLIGHT) && ItemEnergy.get(chest) >= 2_000;
		var data = p.getPersistentData();
		if (wantFlight) {
			if (!p.getAbilities().mayfly) {
				p.getAbilities().mayfly = true;
				p.onUpdateAbilities();
			}
			data.putBoolean(FLIGHT_FLAG, true);
			if (p.getAbilities().flying && p.tickCount % 20 == 0) // 100 FE/t while flying, 25 with the Aether Stabilizer
				ItemEnergy.use(chest, SuitModules.active(chest, Module.AETHER_STABILIZER) ? 500 : 2_000);
		} else if (data.getBoolean(FLIGHT_FLAG)) {
			data.remove(FLIGHT_FLAG);
			if (!p.isCreative() && !p.isSpectator()) {
				p.getAbilities().mayfly = false;
				p.getAbilities().flying = false;
				p.onUpdateAbilities();
			}
		}

		// Magnet: pull items and XP to the player.
		if (SuitModules.active(chest, Module.MAGNET) && p.tickCount % 2 == 0 && !p.isShiftKeyDown()) {
			AABB area = p.getBoundingBox().inflate(8);
			int pulled = 0;
			for (ItemEntity item : p.level().getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.hasPickUpDelay())) {
				Vec3 to = p.position().add(0, 0.5, 0).subtract(item.position());
				if (to.lengthSqr() > 1) item.setDeltaMovement(to.normalize().scale(0.45));
				pulled++;
			}
			for (ExperienceOrb orb : p.level().getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
				Vec3 to = p.position().add(0, 0.5, 0).subtract(orb.position());
				if (to.lengthSqr() > 1) orb.setDeltaMovement(to.normalize().scale(0.45));
				pulled++;
			}
			if (pulled > 0) ItemEnergy.use(chest, 20 * Math.min(pulled, 20));
		}

		// Jump boost.
		if (SuitModules.active(legs, Module.JUMP_BOOST) && p.tickCount % 20 == 0 && ItemEnergy.use(legs, 200))
			p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 2, true, false, false));

		// Hidden-world modules (once a second).
		if (!p.level().isClientSide() && p.tickCount % 20 == 0) {
			if (SuitModules.active(chest, Module.REGENERATION) && p.getHealth() < p.getMaxHealth() && ItemEnergy.use(chest, 800))
				p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, false));
			if (SuitModules.active(legs, Module.RESISTANCE) && ItemEnergy.use(legs, 200))
				p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false, false));
			if (SuitModules.active(head, Module.HASTE) && ItemEnergy.use(head, 200))
				p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, true, false, false));
			if (SuitModules.active(legs, Module.FIRE_IMMUNITY) && (p.isOnFire() || p.isInLava() || p.hasEffect(MobEffects.FIRE_RESISTANCE)) && ItemEnergy.use(legs, 300))
				p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, false));
			if (SuitModules.active(head, Module.ANTIDOTE)) {
				for (var bad : new net.minecraft.world.effect.MobEffect[] {MobEffects.POISON, MobEffects.WITHER, MobEffects.HUNGER, MobEffects.CONFUSION,
						MobEffects.BLINDNESS, MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DARKNESS})
					if (p.hasEffect(bad) && ItemEnergy.use(head, 500)) p.removeEffect(bad);
			}
		}
		if (!p.level().isClientSide() && SuitModules.active(feet, Module.STEALTH) && p.isShiftKeyDown() && p.tickCount % 10 == 0 && ItemEnergy.use(feet, 100))
			p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, false, false));

		// Auto-feed: eat the most filling safe food when hungry.
		if (SuitModules.active(head, Module.AUTO_FEED) && p.tickCount % 40 == 0 && p.getFoodData().getFoodLevel() <= 16) {
			ItemStack best = ItemStack.EMPTY;
			int bestNutrition = 0;
			for (ItemStack s : p.getInventory().items) {
				var food = s.getFoodProperties(p);
				if (food == null || !food.getEffects().isEmpty()) continue; // skip rotten flesh, spider eyes...
				if (food.getNutrition() > bestNutrition) { best = s; bestNutrition = food.getNutrition(); }
			}
			if (!best.isEmpty() && ItemEnergy.use(head, 1_000)) {
				p.getFoodData().eat(best.getItem(), best, p);
				best.shrink(1);
			}
		}
	}

	private SuitModuleEffects() {}
}
