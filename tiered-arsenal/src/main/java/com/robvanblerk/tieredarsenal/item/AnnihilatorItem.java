package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Creative testing tool with no recipe.
 * Hit: 1,000,000 damage credited to the player (drops and XP work), ignoring the attack cooldown.
 * Right-click: kill every monster within 32 blocks. Sneak + right-click: every mob except players and pets.
 */
public class AnnihilatorItem extends Item {
    public static final float DAMAGE = 1_000_000F;
    public static final double RADIUS = 32.0;

    public AnnihilatorItem(Properties properties) {
        super(properties);
    }

    /** Replaces the normal attack, so there's no cooldown scaling and no sweep. */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (!player.level().isClientSide()) {
            target.hurt(player.damageSources().playerAttack(player), DAMAGE);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            boolean everything = player.isShiftKeyDown();
            AABB area = player.getBoundingBox().inflate(RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> {
                if (e == player || !e.isAlive() || e instanceof Player) return false;
                if (!everything) return e instanceof Enemy;
                if (!(e instanceof Mob)) return false;                    // leaves armour stands alone
                return !(e instanceof OwnableEntity pet && pet.getOwnerUUID() != null);
            });
            int killed = 0;
            for (LivingEntity e : targets) {
                e.hurt(player.damageSources().playerAttack(player), DAMAGE);
                if (!e.isAlive() || e.isDeadOrDying()) killed++;
            }
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
            player.displayClientMessage(Component.translatable("message." + TieredArsenal.MOD_ID + ".annihilator_cleared",
                    killed), true);
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Doesn't break blocks when you miss a mob. */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String key = "tooltip." + TieredArsenal.MOD_ID + ".annihilator.";
        tooltip.add(Component.translatable(key + "hit").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(key + "use").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(key + "sneak").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(key + "note").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
