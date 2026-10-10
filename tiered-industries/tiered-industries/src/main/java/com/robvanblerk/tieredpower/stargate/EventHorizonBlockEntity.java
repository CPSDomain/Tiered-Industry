package com.robvanblerk.tieredpower.stargate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Where an open gate leads, and how long it stays open. */
public class EventHorizonBlockEntity extends BlockEntity {
	private @Nullable ResourceKey<Level> target;
	private BlockPos targetPos = BlockPos.ZERO;
	private int life;

	public EventHorizonBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.EVENT_HORIZON.get(), pos, state);
	}

	public void open(ResourceKey<Level> target, BlockPos targetPos, int life) {
		this.target = target;
		this.targetPos = targetPos;
		this.life = life;
		setChanged();
	}

	/** Keeps an open surface alive: it closes by itself if nothing renews it (the dialler unloaded or broke). */
	public void keepAlive(int ticks) {
		if (life < ticks) life = ticks;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, EventHorizonBlockEntity be) {
		if (--be.life <= 0) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	/** Sends the player through (with a short cooldown so they don't bounce straight back). */
	public void travel(ServerPlayer player) {
		if (target == null || player.getServer() == null) return;
		long now = player.serverLevel().getGameTime();
		var tag = player.getPersistentData();
		if (now - tag.getLong("tpGateUsed") < 60 && tag.getString("tpGateFrom").equals(player.level().dimension().location().toString())) return;
		ServerLevel dest = player.getServer().getLevel(target);
		if (dest == null) return;
		// Remember where to come back to when leaving a non-planet world.
		if (Planet.of(player.level()) == null) {
			tag.putString("tpGateHomeDim", player.level().dimension().location().toString());
			// A couple of blocks back the way they walked in, so coming home doesn't drop them straight into the gate.
			var v = player.getDeltaMovement();
			var back = player.position();
			if (v.horizontalDistanceSqr() > 1.0E-4) back = back.subtract(new net.minecraft.world.phys.Vec3(v.x, 0, v.z).normalize().scale(2.5));
			tag.putLong("tpGateHomePos", BlockPos.containing(back).asLong());
		}
		BlockPos at = targetPos;
		if (Planet.of(dest) != null && at.equals(BlockPos.ZERO)) at = StargateTravel.arrival(dest);
		tag.putLong("tpGateUsed", dest.getGameTime());
		tag.putString("tpGateFrom", dest.dimension().location().toString());
		player.level().playSound(null, worldPosition, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.3f, 1.4f);
		player.teleportTo(dest, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot(), player.getXRot());
		player.fallDistance = 0;
		dest.playSound(null, at, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (target != null) tag.putString("target", target.location().toString());
		tag.putLong("targetPos", targetPos.asLong());
		tag.putInt("life", life);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		target = tag.contains("target") ? ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(tag.getString("target"))) : null;
		targetPos = BlockPos.of(tag.getLong("targetPos"));
		life = tag.getInt("life");
	}
}
