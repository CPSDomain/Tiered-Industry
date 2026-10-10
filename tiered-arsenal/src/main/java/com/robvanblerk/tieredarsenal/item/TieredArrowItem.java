package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.entity.ExplosiveArrow;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Special arrows for bows and crossbows:
 *   Explosive - small explosion where it lands (respects the mobGriefing rule)
 *   Fire      - flies burning and sets what it hits on fire
 *   Heavy     - +1.5 damage and extra knockback
 * Fire and Heavy arrows drop as normal arrows when picked back up.
 */
public class TieredArrowItem extends ArrowItem {
    public enum Kind { EXPLOSIVE, FIRE, HEAVY }

    private final Kind kind;

    public TieredArrowItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return switch (kind) {
            case EXPLOSIVE -> new ExplosiveArrow(level, shooter);
            case FIRE -> {
                Arrow arrow = new Arrow(level, shooter);
                arrow.setSecondsOnFire(100);
                yield arrow;
            }
            case HEAVY -> {
                Arrow arrow = new Arrow(level, shooter);
                arrow.setBaseDamage(arrow.getBaseDamage() + 1.5);
                arrow.setKnockback(arrow.getKnockback() + 1);
                yield arrow;
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".arrow."
                + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GOLD));
    }
}
