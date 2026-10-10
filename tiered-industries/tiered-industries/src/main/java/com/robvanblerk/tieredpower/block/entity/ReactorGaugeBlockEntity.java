package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Shows a fission reactor on its face: heat, output, status and add-ons. Build it into a multiblock reactor's wall in
 * place of a Casing or Glass block (the controller links it), or place it against a single-block Fission Reactor or a
 * Fission Controller. A comparator reads the heat (0-15; 15 = about to SCRAM).
 */
public class ReactorGaugeBlockEntity extends BlockEntity {
	public static final int STATUS_NONE = 0, STATUS_RUNNING = 1, STATUS_IDLE = 2, STATUS_SCRAM = 3;
	private int heatPercent, generating, status;
	private boolean reflector, cryo;
	/** Fusion: plasma tier + 1 (0 = this is a fission reactor or none). */
	private int plasma;
	private int ticks;
	/** Set by the Fission Controller when this gauge is built into its walls. */
	private @Nullable BlockPos controllerPos;

	public void setController(BlockPos pos) {
		if (!pos.equals(controllerPos)) {
			controllerPos = pos;
			setChanged();
		}
	}

	public ReactorGaugeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REACTOR_GAUGE.get(), pos, state);
	}

	public int getHeatPercent() { return heatPercent; }
	public int getGenerating() { return generating; }
	public int getStatus() { return status; }
	public boolean hasReflector() { return reflector; }
	public boolean isCryo() { return cryo; }
	/** 0 for fission, else 1 + the fusion reactor's plasma tier. */
	public int getPlasma() { return plasma; }

	public int comparatorSignal() {
		return status == STATUS_NONE ? 0 : Math.min(15, heatPercent * 15 / 100);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ReactorGaugeBlockEntity be) {
		if (++be.ticks % 10 != 0) return;
		int heat = 0, gen = 0, st = STATUS_NONE;
		boolean refl = false, cool = false;
		boolean linked = false;
		int pl = 0;
		// Built into a multiblock's wall: read the controller it was linked to (if that reactor is still formed).
		if (be.controllerPos != null && level.getBlockEntity(be.controllerPos) instanceof FissionControllerBlockEntity c && c.isFormed()) {
			heat = c.getHeat() * 100 / FissionControllerBlockEntity.MAX_HEAT;
			gen = c.getGenerating();
			st = c.isScrammed() ? STATUS_SCRAM : gen > 0 ? STATUS_RUNNING : STATUS_IDLE;
			refl = c.hasReflector();
			cool = c.isCryoBoosted();
			linked = true;
		} else if (be.controllerPos != null && level.getBlockEntity(be.controllerPos) instanceof FusionControllerBlockEntity f && f.isFormed()) {
			heat = f.getTemperature() * 100 / FusionControllerBlockEntity.MAX_TEMP;
			gen = f.getGenerated();
			st = gen > 0 ? STATUS_RUNNING : STATUS_IDLE;
			pl = 1 + f.getPlasmaTier();
			linked = true;
		}
		for (Direction d : Direction.values()) {
			if (linked) break;
			BlockEntity n = level.getBlockEntity(pos.relative(d));
			if (n instanceof FusionReactorBlockEntity fr) {
				heat = fr.getTemperature() * 100 / FusionReactorBlockEntity.MAX_TEMP;
				gen = fr.getGenerated();
				st = gen > 0 ? STATUS_RUNNING : STATUS_IDLE;
				pl = 1 + fr.getPlasmaTier();
				break;
			}
			if (n instanceof FusionControllerBlockEntity fc) {
				heat = fc.getTemperature() * 100 / FusionControllerBlockEntity.MAX_TEMP;
				gen = fc.getGenerated();
				st = gen > 0 ? STATUS_RUNNING : STATUS_IDLE;
				pl = 1 + fc.getPlasmaTier();
				break;
			}
			if (n instanceof FissionReactorBlockEntity r) {
				heat = r.getHeat() * 100 / FissionReactorBlockEntity.MAX_HEAT;
				gen = r.getGenerating();
				st = r.isScrammed() ? STATUS_SCRAM : gen > 0 ? STATUS_RUNNING : STATUS_IDLE;
				refl = r.hasReflector();
				cool = r.isCryoBoosted();
				break;
			}
			if (n instanceof FissionControllerBlockEntity c) {
				heat = c.getHeat() * 100 / FissionControllerBlockEntity.MAX_HEAT;
				gen = c.getGenerating();
				st = c.isScrammed() ? STATUS_SCRAM : gen > 0 ? STATUS_RUNNING : STATUS_IDLE;
				refl = c.hasReflector();
				cool = c.isCryoBoosted();
				break;
			}
		}
		boolean changed = heat != be.heatPercent || gen != be.generating || st != be.status || refl != be.reflector || cool != be.cryo || pl != be.plasma;
		int oldSignal = be.comparatorSignal();
		be.heatPercent = heat;
		be.generating = gen;
		be.status = st;
		be.reflector = refl;
		be.cryo = cool;
		be.plasma = pl;
		if (changed) {
			be.setChanged();
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
		if (be.comparatorSignal() != oldSignal) level.updateNeighbourForOutputSignal(pos, state.getBlock());
	}

	private void write(CompoundTag tag) {
		tag.putInt("heat", heatPercent);
		tag.putInt("gen", generating);
		tag.putInt("status", status);
		tag.putBoolean("reflector", reflector);
		tag.putBoolean("cryo", cryo);
		tag.putInt("plasma", plasma);
	}

	private void read(CompoundTag tag) {
		heatPercent = tag.getInt("heat");
		generating = tag.getInt("gen");
		status = tag.getInt("status");
		reflector = tag.getBoolean("reflector");
		cryo = tag.getBoolean("cryo");
		plasma = tag.getInt("plasma");
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		write(tag);
		if (controllerPos != null) tag.putLong("controller", controllerPos.asLong());
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		read(tag);
		controllerPos = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		write(tag);
		return tag;
	}

	@Override public void handleUpdateTag(CompoundTag tag) { read(tag); }
	@Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
