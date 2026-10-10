package com.robvanblerk.tieredpower.space;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The rocket on the Launch Pad. It stands still until launched, then climbs faster and faster with flame and smoke
 * below it; once it's out of sight it's gone and (if it carried one) its satellite is in orbit.
 */
public class RocketEntity extends Entity {
	private static final EntityDataAccessor<Boolean> LAUNCHING = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);
	public static final int FLIGHT_TICKS = 280;
	private int launchTicks;
	private @Nullable UUID owner;
	private String ownerName = "";
	/** "solar", "mining", or "" for no payload. */
	private String payload = "";

	public RocketEntity(EntityType<?> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	public boolean isLaunching() {
		return entityData.get(LAUNCHING);
	}

	public int launchTicks() {
		return launchTicks;
	}

	public void launch(UUID owner, String ownerName, String payload) {
		this.owner = owner;
		this.ownerName = ownerName;
		this.payload = payload;
		entityData.set(LAUNCHING, true);
		level().playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 6.0f, 0.4f);
		level().playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 0.5f);
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(LAUNCHING, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (!isLaunching()) return;
		launchTicks++;
		double vy = Math.min(2.2, 0.0035 * launchTicks * launchTicks);
		setPos(getX(), getY() + vy, getZ());
		if (level().isClientSide()) {
			var r = level().random;
			for (int i = 0; i < 8; i++) {
				level().addParticle(ParticleTypes.FLAME, getX() + (r.nextDouble() - 0.5) * 0.6, getY() - 0.2, getZ() + (r.nextDouble() - 0.5) * 0.6,
						(r.nextDouble() - 0.5) * 0.1, -0.4 - r.nextDouble() * 0.3, (r.nextDouble() - 0.5) * 0.1);
			}
			for (int i = 0; i < 3; i++) {
				level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, getX() + (r.nextDouble() - 0.5) * 1.2, getY() - 0.6,
						getZ() + (r.nextDouble() - 0.5) * 1.2, (r.nextDouble() - 0.5) * 0.08, 0.02, (r.nextDouble() - 0.5) * 0.08);
			}
		} else if (launchTicks > FLIGHT_TICKS || getY() > level().getMaxBuildHeight() + 200) {
			if (!payload.isEmpty() && owner != null && level() instanceof ServerLevel server) {
				SatelliteData.get(server.getServer()).launch(owner, ownerName, payload);
				String what = payload.equals("mining") ? "Mining Satellite" : "Solar Satellite";
				for (var p : server.players())
					p.displayClientMessage(Component.literal(ownerName + "'s " + what + " has reached orbit!").withStyle(ChatFormatting.GOLD), false);
			}
			discard();
		}
	}

	@Override public boolean hurt(DamageSource source, float amount) { return false; }
	@Override public boolean isPickable() { return false; }
	@Override public boolean isPushable() { return false; }
	@Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 512 * 512; }

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		entityData.set(LAUNCHING, tag.getBoolean("launching"));
		launchTicks = tag.getInt("launchTicks");
		owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
		ownerName = tag.getString("ownerName");
		payload = tag.contains("payloadType") ? tag.getString("payloadType") : tag.getBoolean("payload") ? "solar" : "";
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putBoolean("launching", isLaunching());
		tag.putInt("launchTicks", launchTicks);
		if (owner != null) tag.putUUID("owner", owner);
		tag.putString("ownerName", ownerName);
		tag.putString("payloadType", payload);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
