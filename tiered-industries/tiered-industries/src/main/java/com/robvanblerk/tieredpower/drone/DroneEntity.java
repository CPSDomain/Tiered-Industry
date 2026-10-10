package com.robvanblerk.tieredpower.drone;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import com.robvanblerk.tieredpower.registry.ModEntities;

/**
 * A Utility Drone out of its Drone Station. It flies (through anything) to a job, does it, and brings the results
 * home. Drones are not saved with the world: the station launches them again when it loads, and takes back their
 * cargo when it unloads or is broken.
 */
public class DroneEntity extends Entity {
	private static final EntityDataAccessor<ItemStack> CARRY = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.ITEM_STACK);
	public static final double SPEED = 0.35;
	private static final int HOME = 0, GOING = 1, RETURNING = 2;

	private @Nullable BlockPos station;
	private int slot;
	private int phase = HOME;
	private @Nullable DroneStationBlockEntity.Job job;
	private final List<ItemStack> cargo = new ArrayList<>();
	private Vec3 lastMove = Vec3.ZERO;

	public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	public DroneEntity(Level level, BlockPos station, int slot) {
		this(ModEntities.DRONE.get(), level);
		this.station = station;
		this.slot = slot;
	}

	public boolean isHome() {
		return phase == HOME;
	}

	/** What the drone is carrying (shown under it). */
	public ItemStack carried() {
		return entityData.get(CARRY);
	}

	public List<ItemStack> takeCargo() {
		List<ItemStack> out = new ArrayList<>(cargo);
		cargo.clear();
		syncCarry();
		return out;
	}

	private void syncCarry() {
		entityData.set(CARRY, cargo.isEmpty() ? ItemStack.EMPTY : cargo.get(0).copyWithCount(1));
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(CARRY, ItemStack.EMPTY);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			Vec3 v = getDeltaMovement();
			setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
			return;
		}
		ServerLevel server = (ServerLevel) level();
		if (station == null || !(server.getBlockEntity(station) instanceof DroneStationBlockEntity be)) {
			for (ItemStack s : cargo) Block.popResource(server, blockPosition(), s);
			cargo.clear();
			discard();
			return;
		}
		boolean wanted = be.wantsDrone(slot, this);
		Vec3 dock = be.dock(slot);
		switch (phase) {
			case HOME -> {
				flyTo(dock, station);
				if (position().distanceTo(dock) > 0.3) break;
				if (!cargo.isEmpty()) {
					List<ItemStack> left = be.unload(cargo);
					cargo.clear();
					cargo.addAll(left);
					syncCarry();
				}
				if (!wanted) {
					if (cargo.isEmpty()) { be.forget(this); discard(); }
					break;
				}
				if (cargo.isEmpty()) {
					job = be.nextJob(this, cargo);
					syncCarry();
					if (job != null) phase = GOING;
				}
			}
			case GOING -> {
				if (!wanted || job == null) { be.release(job); job = null; phase = RETURNING; break; }
				Vec3 target = Vec3.atCenterOf(job.pos()).add(0, 0.9, 0);
				if (job.kind() == DroneStationBlockEntity.COLLECT && job.item() != null && server.getEntity(job.item()) instanceof ItemEntity ie)
					target = ie.position().add(0, 0.6, 0);
				flyTo(target, station);
				if (position().distanceTo(target) < 0.5) {
					work(server, be);
					be.release(job);
					job = null;
					phase = RETURNING;
				}
			}
			default -> {
				flyTo(dock, station);
				if (position().distanceTo(dock) < 0.3) phase = HOME;
			}
		}
	}

	private void work(ServerLevel server, DroneStationBlockEntity be) {
		if (job == null) return;
		switch (job.kind()) {
			case DroneStationBlockEntity.HARVEST -> cargo.addAll(DroneStationBlockEntity.harvest(server, job.pos()));
			case DroneStationBlockEntity.COLLECT -> {
				if (job.item() != null && server.getEntity(job.item()) instanceof ItemEntity ie && ie.isAlive() && ie.distanceTo(this) < 2.5) {
					cargo.add(ie.getItem().copy());
					ie.discard();
				}
			}
			case DroneStationBlockEntity.FETCH -> {
				IItemHandler h = be.linkHandler();
				if (h != null) {
					for (int i = 0; i < h.getSlots(); i++) {
						ItemStack got = h.extractItem(i, 64, false);
						if (!got.isEmpty()) { cargo.add(got); break; }
					}
				}
			}
			case DroneStationBlockEntity.DELIVER -> {
				IItemHandler h = be.linkHandler();
				if (h != null) {
					List<ItemStack> left = new ArrayList<>();
					for (ItemStack c : cargo) {
						ItemStack rest = ItemHandlerHelper.insertItemStacked(h, c, false);
						if (!rest.isEmpty()) left.add(rest);
					}
					cargo.clear();
					cargo.addAll(left);
				}
			}
			default -> {}
		}
		syncCarry();
	}

	/** Heads for 'target', climbing above the station's height first on longer trips so it doesn't skim the ground. */
	private void flyTo(Vec3 target, BlockPos home) {
		Vec3 pos = position();
		Vec3 aim = target;
		double flat = Math.hypot(target.x - pos.x, target.z - pos.z);
		if (flat > 2.5) aim = new Vec3(target.x, Math.max(Math.max(target.y, pos.y), home.getY() + 3.5), target.z);
		Vec3 d = aim.subtract(pos);
		double dist = d.length();
		Vec3 move = dist <= SPEED ? d : d.scale(SPEED / dist);
		setPos(pos.x + move.x, pos.y + move.y, pos.z + move.z);
		if (move.distanceToSqr(lastMove) > 1.0E-4) {
			setDeltaMovement(move);
			hasImpulse = true;
			lastMove = move;
		}
		if (move.horizontalDistanceSqr() > 1.0E-4) setYRot((float) (Math.atan2(-move.x, move.z) * 180 / Math.PI));
	}

	// ---- not a mob: can't be hit, pushed or saved ----

	@Override public boolean isPickable() { return false; }
	@Override public boolean isPushable() { return false; }
	@Override public boolean hurt(DamageSource source, float amount) { return false; }
	@Override public boolean shouldBeSaved() { return false; }
	@Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 96 * 96; }

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
