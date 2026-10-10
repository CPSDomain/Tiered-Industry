package com.robvanblerk.tieredarsenal.entity;

import com.robvanblerk.tieredarsenal.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * The Aetherium Guardian: an End boss summoned with a Void Sigil.
 *   300 health, 12 armour, immune to fire, falls and knockback
 *   Shockwave every 6 s when players are within 6 blocks (8 damage + knockback)
 *   Homing shulker bullets (levitation) at range
 *   Teleports next to its target if it gets more than 16 blocks away
 * Drops Aetherium Ingots, Aetherium Upgrade Templates and a Guardian Core (loot table).
 */
public class AetheriumGuardian extends Monster implements RangedAttackMob {
    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    private int shockwaveCooldown = 100;
    private int bulletCooldown = 60;
    private int teleportCooldown = 0;

    public AetheriumGuardian(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 250;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GREATSWORDS.get("aetherium").get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (shockwaveCooldown > 0) shockwaveCooldown--;
        if (bulletCooldown > 0) bulletCooldown--;
        if (teleportCooldown > 0) teleportCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double distSq = distanceToSqr(target);

        if (distSq < 36 && shockwaveCooldown == 0) {
            shockwave();
            shockwaveCooldown = 120;
        }
        if (distSq > 49 && bulletCooldown == 0 && hasLineOfSight(target)) {
            performRangedAttack(target, 1.0F);
            bulletCooldown = 50;
        }
        if (distSq > 256 && teleportCooldown == 0) {
            teleportNear(target);
            teleportCooldown = 160;
        }
    }

    private void shockwave() {
        if (!(level() instanceof ServerLevel server)) return;
        for (Player player : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(6.0))) {
            if (player.isCreative() || player.isSpectator()) continue;
            player.hurt(damageSources().mobAttack(this), 8.0F);
            player.knockback(1.5, getX() - player.getX(), getZ() - player.getZ());
        }
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 8, 2.5, 0.2, 2.5, 0.0);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0, getZ(), 60, 3.0, 0.5, 3.0, 0.1);
        playSound(SoundEvents.GENERIC_EXPLODE, 1.5F, 0.6F);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ShulkerBullet bullet = new ShulkerBullet(level(), this, target, Direction.Axis.Y);
        level().addFreshEntity(bullet);
        playSound(SoundEvents.SHULKER_SHOOT, 2.0F, 0.8F);
    }

    private void teleportNear(LivingEntity target) {
        for (int i = 0; i < 16; i++) {
            double x = target.getX() + (getRandom().nextDouble() - 0.5) * 8.0;
            double z = target.getZ() + (getRandom().nextDouble() - 0.5) * 8.0;
            if (randomTeleport(x, target.getY(), z, true)) {
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.6F);
                return;
            }
        }
    }

    // ---- Boss bar ----

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bossEvent.setName(getDisplayName());
    }

    // ---- Boss rules ----

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
