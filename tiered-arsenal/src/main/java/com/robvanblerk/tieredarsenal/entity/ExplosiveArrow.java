package com.robvanblerk.tieredarsenal.entity;

import com.robvanblerk.tieredarsenal.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Explodes where it lands (power 2, a little more than a creeper's half). Used up on impact. */
public class ExplosiveArrow extends AbstractArrow {
    public static final float POWER = 2.0F;

    public ExplosiveArrow(EntityType<? extends ExplosiveArrow> type, Level level) {
        super(type, level);
    }

    public ExplosiveArrow(Level level, LivingEntity shooter) {
        super(ModEntities.EXPLOSIVE_ARROW.get(), shooter, level);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        explode();
    }

    private void explode() {
        if (!level().isClientSide() && isAlive()) {
            // MOB interaction: breaks blocks only when the mobGriefing game rule is on
            level().explode(this, getX(), getY(), getZ(), POWER, Level.ExplosionInteraction.MOB);
            discard();
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }
}
