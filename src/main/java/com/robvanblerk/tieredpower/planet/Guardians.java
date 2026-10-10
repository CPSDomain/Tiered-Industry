package com.robvanblerk.tieredpower.planet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.stargate.AddressTabletItem;
import com.robvanblerk.tieredpower.stargate.Planet;

/**
 * Planet guardians: a much tougher version of a mob that fits each planet, with a boss bar, called from a Guardian
 * Altar. They drop a Guardian Heart (a crafting shortcut to Naquadah-tier gear) and a pile of loot.
 */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID)
public final class Guardians {
	public static final String TAG = "tpGuardian";
	public static final float HEALTH = 300f;
	private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
	private static final Map<UUID, ServerLevel> WHERE = new HashMap<>();

	private Guardians() {}

	private static EntityType<? extends Mob> typeFor(Planet p) {
		return switch (p) {
			case FROST -> EntityType.STRAY;
			case DUNE -> EntityType.HUSK;
			case VERDANT, EDEN, REDWOOD -> EntityType.VINDICATOR;
			case INFERNO, WRAITH, UMBRA -> EntityType.WITHER_SKELETON;
			case ABYSS -> EntityType.DROWNED;
			case SKYLANDS -> EntityType.BLAZE;
			case MYCELIA, KAROO, TITAN -> EntityType.RAVAGER;
			case CRYSTAL -> EntityType.PIGLIN_BRUTE;
			case MURK -> EntityType.WITCH;
			case VOID_REACH -> EntityType.EVOKER;
		};
	}

	/** Calls the planet's guardian above the altar. */
	public static boolean summon(ServerLevel level, BlockPos altar, Planet planet) {
		Mob mob = typeFor(planet).create(level);
		if (mob == null) return false;
		BlockPos at = altar.above();
		mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		String name = planet.title + " Guardian";
		mob.setCustomName(Component.literal(name).withStyle(ChatFormatting.GOLD));
		mob.setCustomNameVisible(true);
		mob.setPersistenceRequired();
		mob.getPersistentData().putBoolean(TAG, true);
		mob.getPersistentData().putString("tpGuardianPlanet", planet.id);
		boost(mob, Attributes.MAX_HEALTH, HEALTH);
		boost(mob, Attributes.ARMOR, 12);
		boost(mob, Attributes.KNOCKBACK_RESISTANCE, 0.8);
		AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() + 8);
		AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (follow != null) follow.setBaseValue(48);
		mob.setHealth(mob.getMaxHealth());
		if (mob.getMainHandItem().isEmpty()) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
		if (mob instanceof net.minecraft.world.entity.monster.Drowned) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
		for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0f);
		if (mob instanceof AbstractPiglin piglin) piglin.setImmuneToZombification(true);
		mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
		mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
		level.addFreshEntity(mob);
		level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1f, 1.2f);
		ServerBossEvent bar = new ServerBossEvent(Component.literal(name).withStyle(ChatFormatting.GOLD), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
		BARS.put(mob.getUUID(), bar);
		WHERE.put(mob.getUUID(), level);
		return true;
	}

	private static void boost(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attr, double value) {
		AttributeInstance a = mob.getAttribute(attr);
		if (a != null) a.setBaseValue(Math.max(a.getBaseValue(), value));
	}

	/** Keeps boss bars up to date (players within 48 blocks see it). */
	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || BARS.isEmpty()) return;
		Iterator<Map.Entry<UUID, ServerBossEvent>> it = BARS.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			ServerLevel level = WHERE.get(e.getKey());
			Entity ent = level == null ? null : level.getEntity(e.getKey());
			ServerBossEvent bar = e.getValue();
			if (!(ent instanceof Mob mob) || !mob.isAlive()) {
				if (ent == null && level != null && level.getServer().getTickCount() % 200 != 0) continue; // unloaded: keep a while
				bar.removeAllPlayers();
				it.remove();
				WHERE.remove(e.getKey());
				continue;
			}
			bar.setProgress(mob.getHealth() / mob.getMaxHealth());
			for (ServerPlayer p : level.players()) {
				boolean near = p.distanceToSqr(mob) < 48 * 48;
				if (near && !bar.getPlayers().contains(p)) bar.addPlayer(p);
				else if (!near && bar.getPlayers().contains(p)) bar.removePlayer(p);
			}
		}
	}

	/** A guardian's reward: a Guardian Heart, planet riches and an address. */
	@SubscribeEvent
	public static void onDrops(LivingDropsEvent event) {
		Entity e = event.getEntity();
		if (!e.getPersistentData().getBoolean(TAG) || !(e.level() instanceof ServerLevel level)) return;
		Planet planet = Planet.byId(e.getPersistentData().getString("tpGuardianPlanet"));
		var rnd = level.random;
		java.util.List<ItemStack> loot = new java.util.ArrayList<>();
		loot.add(new ItemStack(ModBlocks.GUARDIAN_HEART.get()));
		loot.add(new ItemStack(ModBlocks.NAQUADAH_INGOT.get(), 6 + rnd.nextInt(7)));
		loot.add(new ItemStack(Items.DIAMOND, 3 + rnd.nextInt(5)));
		loot.add(new ItemStack(Items.EMERALD, 4 + rnd.nextInt(9)));
		loot.add(new ItemStack(rnd.nextBoolean() ? ModBlocks.SPEED_UPGRADE_2.get() : ModBlocks.EFFICIENCY_UPGRADE_2.get(), 1 + rnd.nextInt(2)));
		loot.add(AddressTabletItem.random(rnd));
		if (planet != null) {
			ItemStack mat = planetMaterial(planet);
			if (!mat.isEmpty()) loot.add(mat.copyWithCount(8 + rnd.nextInt(9)));
		}
		for (ItemStack s : loot) event.getDrops().add(new ItemEntity(level, e.getX(), e.getY() + 0.5, e.getZ(), s));
		e.level().playSound(null, e.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
		if (event.getSource().getEntity() instanceof ServerPlayer p)
			p.displayClientMessage(Component.literal((planet == null ? "Guardian" : planet.title + " Guardian") + " defeated!").withStyle(ChatFormatting.GOLD), false);
	}

	/** Each planet's own material. */
	public static ItemStack planetMaterial(Planet p) {
		return switch (p) {
			case FROST -> new ItemStack(ModBlocks.CRYONITE_SHARD.get());
			case DUNE -> new ItemStack(ModBlocks.SOLARITE_SAND.get());
			case VERDANT -> new ItemStack(ModBlocks.LIVINGWOOD_LOG.get());
			case INFERNO -> new ItemStack(ModBlocks.RAW_NAQUADAH.get());
			case ABYSS -> new ItemStack(ModBlocks.ABYSSAL_PEARL.get());
			case SKYLANDS -> new ItemStack(ModBlocks.AETHER_CRYSTAL.get());
			case MYCELIA -> new ItemStack(ModBlocks.SPORECAP.get());
			case CRYSTAL -> new ItemStack(ModBlocks.RESONANCE_CRYSTAL.get());
			case EDEN -> new ItemStack(ModBlocks.LIFEBLOOM.get());
			case KAROO -> new ItemStack(ModBlocks.SUNSTONE.get());
			case REDWOOD -> new ItemStack(ModBlocks.AMBER.get());
			case MURK -> new ItemStack(ModBlocks.WITCHROOT.get());
			case TITAN -> new ItemStack(ModBlocks.GRAVITITE.get());
			case WRAITH -> new ItemStack(ModBlocks.SOUL_CRYSTAL.get());
			case UMBRA -> new ItemStack(ModBlocks.UMBRAL_SHARD.get());
			case VOID_REACH -> new ItemStack(ModBlocks.VOID_SHARD.get());
		};
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		BARS.values().forEach(ServerBossEvent::removeAllPlayers);
		BARS.clear();
		WHERE.clear();
	}
}
