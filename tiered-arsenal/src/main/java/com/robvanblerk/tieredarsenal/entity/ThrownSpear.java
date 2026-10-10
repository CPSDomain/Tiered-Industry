package com.robvanblerk.tieredarsenal.entity;

import com.robvanblerk.tieredarsenal.config.ArsenalConfig;
import com.robvanblerk.tieredarsenal.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A thrown spear: hits once like a trident, then drops so it can be picked up again. */
public class ThrownSpear extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(ThrownSpear.class, EntityDataSerializers.ITEM_STACK);

    private boolean dealtDamage;

    public ThrownSpear(EntityType<? extends ThrownSpear> type, Level level) {
        super(type, level);
    }

    public ThrownSpear(Level level, LivingEntity thrower, ItemStack spear) {
        super(ModEntities.THROWN_SPEAR.get(), thrower, level);
        setSpear(spear.copy());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ITEM, ItemStack.EMPTY);
    }

    public ItemStack getSpear() {
        return entityData.get(DATA_ITEM);
    }

    private void setSpear(ItemStack stack) {
        entityData.set(DATA_ITEM, stack);
    }

    @Override
    protected ItemStack getPickupItem() {
        return getSpear().copy();
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 start, Vec3 end) {
        return dealtDamage ? null : super.findHitEntity(start, end);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = 1.0F;
        if (getSpear().getItem() instanceof SwordItem spear) {
            damage = (1.0F + spear.getDamage()) * ArsenalConfig.SPEAR_THROW_MULTIPLIER.get().floatValue();
        }
        if (target instanceof LivingEntity living) {
            damage += EnchantmentHelper.getDamageBonus(getSpear(), living.getMobType());
        }

        Entity owner = getOwner();
        DamageSource source = damageSources().trident(this, owner == null ? this : owner);
        dealtDamage = true;
        if (target.hurt(source, damage) && target instanceof LivingEntity living && owner instanceof LivingEntity thrower) {
            EnchantmentHelper.doPostHurtEffects(living, thrower);
            EnchantmentHelper.doPostDamageEffects(thrower, living);
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    /** Like the trident: a spear the thrower can pick up never despawns. */
    @Override
    protected void tickDespawn() {
        if (pickup != Pickup.ALLOWED) {
            super.tickDespawn();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Spear", getSpear().save(new CompoundTag()));
        tag.putBoolean("DealtDamage", dealtDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Spear")) {
            setSpear(ItemStack.of(tag.getCompound("Spear")));
        }
        dealtDamage = tag.getBoolean("DealtDamage");
    }
}
