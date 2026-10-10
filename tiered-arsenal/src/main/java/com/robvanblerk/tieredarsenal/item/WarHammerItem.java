package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.config.ArsenalConfig;
import com.robvanblerk.tieredarsenal.registry.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Slow, heavy weapon. Every hit sends a shockwave that does 40% of the hit's damage
 * to other mobs within 2.5 blocks of the target and knocks them back.
 * Never hits the wielder, the wielder's pets, or armour stands.
 */
public class WarHammerItem extends SwordItem {
    public static final float SPLASH_FRACTION = 0.4F;
    public static final double SPLASH_RADIUS = 2.5;

    public WarHammerItem(Tier tier, Properties properties) {
        // 1 + 6 + tier bonus damage, attack speed 0.8
        super(tier, 6, -3.2F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (!level.isClientSide()) {
            int shockwave = ModEnchantments.level(ModEnchantments.SHOCKWAVE, stack);
            float splash = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    * (ArsenalConfig.WARHAMMER_SPLASH.get().floatValue() + 0.1F * shockwave);
            double radius = SPLASH_RADIUS + 0.5 * shockwave;
            DamageSource source = attacker instanceof Player player
                    ? attacker.damageSources().playerAttack(player)
                    : attacker.damageSources().mobAttack(attacker);
            AABB area = target.getBoundingBox().inflate(radius, 1.0, radius);

            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e ->
                    e != attacker && e != target && e.isAlive()
                            && !(e instanceof ArmorStand)
                            && !attacker.isAlliedTo(e)
                            && !(e instanceof TamableAnimal pet && pet.isOwnedBy(attacker)));

            for (LivingEntity e : nearby) {
                e.hurt(source, splash);
                e.knockback(0.5, target.getX() - e.getX(), target.getZ() - e.getZ());
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.4F);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return action != ToolActions.SWORD_SWEEP && super.canPerformAction(stack, action);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".warhammer").withStyle(ChatFormatting.GOLD));
    }
}
