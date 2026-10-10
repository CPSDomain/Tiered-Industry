package com.robvanblerk.tieredarsenal.event;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.config.ArsenalConfig;
import com.robvanblerk.tieredarsenal.item.TieredCrossbowItem;
import com.robvanblerk.tieredarsenal.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/** Things that happen when an entity first appears: crossbow arrow damage, and mobs spawning in gear. */
@Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpawnEvents {
    private SpawnEvents() {}

    private static final EquipmentSlot[] ARMOUR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;

        // Tiered crossbow: add its arrow damage bonus to arrows it fires
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.shotFromCrossbow()
                && arrow.getOwner() instanceof LivingEntity shooter) {
            TieredCrossbowItem crossbow = crossbowIn(shooter.getMainHandItem());
            if (crossbow == null) crossbow = crossbowIn(shooter.getOffhandItem());
            if (crossbow != null) {
                arrow.setBaseDamage(arrow.getBaseDamage() + crossbow.getArrowBonus());
            }
            return;
        }

        if (event.getEntity() instanceof Mob mob) {
            equipMob(mob, event.getLevel());
        }
    }

    private static TieredCrossbowItem crossbowIn(ItemStack stack) {
        return stack.getItem() instanceof TieredCrossbowItem c ? c : null;
    }

    /**
     * Overworld zombies and skeletons sometimes spawn with one armour piece (Copper 60%, Cobalt 35%,
     * Titanium 5%); zombies may also get a sword or dagger. Wither skeletons sometimes carry an
     * Infernium sword. The usual equipment drop chance applies.
     */
    private static void equipMob(Mob mob, Level level) {
        double chance = ArsenalConfig.MOB_GEAR_CHANCE.get();
        if (chance <= 0) return;
        RandomSource random = mob.getRandom();

        if (mob instanceof WitherSkeleton) {
            if (random.nextDouble() < chance * 2) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SWORDS.get("infernium").get()));
            }
            return;
        }
        if (level.dimension() != Level.OVERWORLD || mob instanceof ZombifiedPiglin) return;
        if (!(mob instanceof Zombie || mob instanceof AbstractSkeleton)) return;
        if (random.nextDouble() >= chance) return;

        double roll = random.nextDouble();
        String material = roll < 0.60 ? "copper" : roll < 0.95 ? "cobalt" : "titanium";

        EquipmentSlot slot = ARMOUR[random.nextInt(ARMOUR.length)];
        if (mob.getItemBySlot(slot).isEmpty()) {
            Map<String, RegistryObject<Item>> pieces = switch (slot) {
                case HEAD -> ModItems.HELMETS;
                case CHEST -> ModItems.CHESTPLATES;
                case LEGS -> ModItems.LEGGINGS;
                default -> ModItems.BOOTS;
            };
            mob.setItemSlot(slot, new ItemStack(pieces.get(material).get()));
        }

        if (mob instanceof Zombie && mob.getMainHandItem().isEmpty() && random.nextBoolean()) {
            Map<String, RegistryObject<Item>> weapons = random.nextBoolean() ? ModItems.SWORDS : ModItems.DAGGERS;
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapons.get(material).get()));
        }
    }
}
