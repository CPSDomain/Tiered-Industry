package com.robvanblerk.tieredarsenal.event;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.config.ArsenalConfig;
import com.robvanblerk.tieredarsenal.item.DaggerItem;
import com.robvanblerk.tieredarsenal.item.TieredShieldItem;
import com.robvanblerk.tieredarsenal.registry.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeaponEvents {
    private WeaponEvents() {}

    /** Dagger backstab: extra damage (1.5x by default) when the attacker is behind the target. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return; // melee only, not arrows
        if (!(attacker.getMainHandItem().getItem() instanceof DaggerItem)) return;

        LivingEntity target = event.getEntity();
        Vec3 facing = target.getViewVector(1.0F).multiply(1, 0, 1);
        Vec3 toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1);
        if (facing.lengthSqr() < 1.0E-4 || toAttacker.lengthSqr() < 1.0E-4) return;

        // Negative dot product = the attacker is behind where the target is looking.
        if (facing.normalize().dot(toAttacker.normalize()) < -0.3) {
            int assassin = ModEnchantments.level(ModEnchantments.ASSASSIN, attacker.getMainHandItem());
            event.setAmount(event.getAmount()
                    * (ArsenalConfig.DAGGER_BACKSTAB.get().floatValue() + 0.25F * assassin));
        }
    }

    /** Shield perks against melee attackers: Titanium repels, Infernium burns, Aetherium reflects. */
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        ItemStack shield = blocker.getUseItem();
        if (blocker.level().isClientSide()) return;
        if (!(event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker)) return;

        // Counter enchantment works on any shield, vanilla included
        int counter = ModEnchantments.level(ModEnchantments.COUNTER, shield);
        if (counter > 0) {
            attacker.hurt(blocker.damageSources().thorns(blocker), 2.0F * counter);
        }
        if (!(shield.getItem() instanceof TieredShieldItem tiered)) return;

        switch (tiered.getMaterial()) {
            case "titanium" -> attacker.knockback(0.8, blocker.getX() - attacker.getX(), blocker.getZ() - attacker.getZ());
            case "infernium" -> attacker.setSecondsOnFire(4);
            case "aetherium" -> attacker.hurt(blocker.damageSources().thorns(blocker),
                    event.getBlockedDamage() * ArsenalConfig.AETHERIUM_SHIELD_REFLECT.get().floatValue());
            default -> { }
        }
    }
}
