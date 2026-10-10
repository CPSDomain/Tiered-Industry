package com.robvanblerk.tieredarsenal.event;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.config.ArsenalConfig;
import com.robvanblerk.tieredarsenal.material.ModArmorMaterials;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Full-set bonuses (all four pieces of the same material):
 *   Emerald   - Hero of the Village (cheaper villager trades)
 *   Cobalt    - Haste I
 *   Titanium  - Speed I
 *   Infernium - Fire Resistance
 *   Aetherium - Resistance I and no fall damage
 */
@Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SetBonusHandler {
    private SetBonusHandler() {}

    /** Effects are refreshed once a second and last a little longer, so they never flicker. */
    private static final int EFFECT_TICKS = 60;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;

        if (!ArsenalConfig.SET_BONUSES.get()) return;
        ModArmorMaterials set = getFullSet(player);
        if (set == null) return;

        switch (set) {
            case EMERALD -> apply(player, MobEffects.HERO_OF_THE_VILLAGE, 0);
            case COBALT -> apply(player, MobEffects.DIG_SPEED, 0);
            case TITANIUM -> apply(player, MobEffects.MOVEMENT_SPEED, 0);
            case INFERNIUM -> apply(player, MobEffects.FIRE_RESISTANCE, 0);
            case AETHERIUM -> apply(player, MobEffects.DAMAGE_RESISTANCE, 0);
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (ArsenalConfig.SET_BONUSES.get() && event.getEntity() instanceof Player player
                && getFullSet(player) == ModArmorMaterials.AETHERIUM) {
            event.setCanceled(true);
        }
    }

    /** Returns the material if the player wears a full set of one of our materials, otherwise null. */
    @Nullable
    public static ModArmorMaterials getFullSet(Player player) {
        ModArmorMaterials found = null;
        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem armor)
                    || !(armor.getMaterial() instanceof ModArmorMaterials material)) {
                return null;
            }
            if (found == null) {
                found = material;
            } else if (found != material) {
                return null;
            }
        }
        return found;
    }

    private static void apply(Player player, MobEffect effect, int amplifier) {
        // ambient = true, visible particles = false, show icon = true
        player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, amplifier, true, false, true));
    }
}
