package com.robvanblerk.tieredpower.stargate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Held by the bottom-middle block of a formed Stargate: dialling and open state, for the renderer. */
public class StargateRingBlockEntity extends BlockEntity {
	public static final int DIAL_TICKS = 50;
	/** How long the "kawoosh" (the unstable vortex that bursts out of a gate as it opens) lasts, and how far it reaches. */
	public static final int KAWOOSH_TICKS = 30;
	/** "Open until told to close": the dialler that opened the gate closes it (or it shuts if nobody keeps it alive). */
	public static final int HELD = 1_000_000_000;
	public static final float KAWOOSH_LENGTH = 5.5f;
	/** Ticks left of dialling (chevrons locking, inner ring spinning), then ticks left open. */
	private int dialing, open;
	/** Which side of the ring the vortex bursts out of: +1 or -1 along the ring's normal (towards the dialler). */
	private int side = 1;
	private int serverOpenAge;
	/** Server: the last game time a dialler (at either end) said the gate is still in use. */
	private long lastKeep;
	/** Client only: inner ring angle and how many ticks it's been open (for the opening surge). */
	public float clientSpin;
	public int clientOpenAge;

	public StargateRingBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STARGATE_RING.get(), pos, state);
	}

	public int getDialing() { return dialing; }
	public int getOpen() { return open; }
	public int getSide() { return side; }

	/** Start dialling; the gate opens for 'openTicks' when dialling finishes, bursting out on 'side' (+1 / -1). */
	public void dial(int openTicks, int side) {
		dialing = DIAL_TICKS;
		open = openTicks;
		this.side = side >= 0 ? 1 : -1;
		serverOpenAge = 0;
		if (level != null) lastKeep = level.getGameTime();
		sync();
	}

	/** How far the vortex reaches out of the gate 'age' ticks after it opened (0 when it's over). */
	public static float kawooshLength(float age) {
		if (age < 0 || age >= KAWOOSH_TICKS) return 0;
		if (age < 8) return KAWOOSH_LENGTH * (float) Math.sin(age / 8.0 * Math.PI / 2);
		if (age < 13) return KAWOOSH_LENGTH;
		float f = 1 - (age - 13) / (KAWOOSH_TICKS - 13f);
		return KAWOOSH_LENGTH * f * f;
	}

	/** Called every tick by the dialler that opened this gate (or the one at the other end). */
	public void keepAlive() {
		if (level != null) lastKeep = level.getGameTime();
	}

	/** Dialling or open, and still being kept alive by a dialler (a stale state from before a reload doesn't count). */
	public boolean isActive() {
		return (dialing > 0 || open > 0) && level != null && level.getGameTime() - lastKeep <= 60;
	}

	public void close() {
		dialing = 0;
		open = 0;
		sync();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StargateRingBlockEntity be) {
		if (be.dialing > 0) be.dialing--;
		else if (be.open > 0) be.open--;
		if (!level.isClientSide()) {
			be.serverOpenAge = be.dialing == 0 && be.open > 0 ? be.serverOpenAge + 1 : 0;
			// nothing keeping it open any more (dialler gone, unloaded, or a world reload): shut down
			if ((be.dialing > 0 || be.open > 0) && level.getGameTime() - be.lastKeep > 60) { be.close(); return; }
			if (be.serverOpenAge > 0 && be.serverOpenAge <= 14) be.vortexHits(level, state);
		} else {
			if (be.dialing > 0) be.clientSpin += 6f;
			be.clientOpenAge = be.dialing == 0 && be.open > 0 ? be.clientOpenAge + 1 : 0;
		}
	}

	/** Like on TV, whatever stands in front of the gate as the vortex bursts out is vaporised - mobs at least. Players
	 * (and named or tamed animals) are just thrown clear. */
	private void vortexHits(Level level, BlockState state) {
		if (!state.hasProperty(StargateFrameBlock.AXIS)) return;
		boolean axisX = state.getValue(StargateFrameBlock.AXIS) == net.minecraft.core.Direction.Axis.X;
		float len = kawooshLength(serverOpenAge);
		if (len <= 0.2f) return;
		double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 3.5, cz = worldPosition.getZ() + 0.5;
		double r = 2.3;
		AABB box = axisX ? new AABB(cx - r, cy - r, cz, cx + r, cy + r, cz + side * len)
				: new AABB(cx, cy - r, cz - r, cx + side * len, cy + r, cz + r);
		for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
			boolean spare = e instanceof net.minecraft.world.entity.player.Player || e.hasCustomName()
					|| (e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())
					|| e instanceof net.minecraft.world.entity.npc.AbstractVillager;
			if (spare) {
				double push = 1.2;
				e.setDeltaMovement(axisX ? e.getDeltaMovement().x : side * push, 0.35, axisX ? side * push : e.getDeltaMovement().z);
				e.hurtMarked = true;
			} else if (e.isAlive()) {
				e.hurt(level.damageSources().magic(), 1000f);
			}
		}
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag t = super.getUpdateTag();
		t.putInt("dialing", dialing);
		t.putInt("open", open);
		t.putInt("side", side);
		return t;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		dialing = tag.getInt("dialing");
		open = tag.getInt("open");
		side = tag.getInt("side") < 0 ? -1 : 1;
		// arriving while a gate is already open: don't replay the opening vortex
		if (dialing == 0 && open > 0 && clientOpenAge == 0) clientOpenAge = KAWOOSH_TICKS;
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
		if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		// not saved: a gate always starts closed after a reload (the dialler that opened it has forgotten it too)
		tag.putInt("side", side);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		side = tag.getInt("side") < 0 ? -1 : 1;
	}

	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(7, 0, 7).expandTowards(0, 7, 0);
	}
}
