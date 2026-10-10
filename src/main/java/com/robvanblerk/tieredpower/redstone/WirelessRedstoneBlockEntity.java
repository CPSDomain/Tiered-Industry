package com.robvanblerk.tieredpower.redstone;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Wireless Redstone Transmitter / Receiver: one of 64 channels, any distance, any dimension. */
public class WirelessRedstoneBlockEntity extends BlockEntity {
	private int channel = 1;
	private int lastSent = -1;

	public WirelessRedstoneBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIRELESS_REDSTONE.get(), pos, state);
	}

	public int getChannel() { return channel; }

	public void setChannel(int c) {
		if (level != null && !level.isClientSide()) RedstoneChannels.remove(channel, GlobalPos.of(level.dimension(), worldPosition));
		channel = ((c - 1) % RedstoneChannels.CHANNELS + RedstoneChannels.CHANNELS) % RedstoneChannels.CHANNELS + 1;
		lastSent = -1;
		setChanged();
	}

	private boolean receiver() {
		return getBlockState().getBlock() instanceof WirelessRedstoneBlock b && b.receiver;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, WirelessRedstoneBlockEntity be) {
		if (level.getGameTime() % 2 != 0) return;
		long now = level.getGameTime();
		if (!be.receiver()) {
			int s = level.getBestNeighborSignal(pos);
			RedstoneChannels.report(be.channel, GlobalPos.of(level.dimension(), pos), s, now);
			if (state.getValue(WirelessRedstoneBlock.POWER) != s) level.setBlock(pos, state.setValue(WirelessRedstoneBlock.POWER, s), Block.UPDATE_CLIENTS);
		} else {
			int s = RedstoneChannels.strength(be.channel, now);
			if (state.getValue(WirelessRedstoneBlock.POWER) != s) {
				level.setBlock(pos, state.setValue(WirelessRedstoneBlock.POWER, s), Block.UPDATE_ALL);
				level.updateNeighborsAt(pos, state.getBlock());
			}
		}
	}

	@Override
	public void setRemoved() {
		if (level != null && !level.isClientSide()) RedstoneChannels.remove(channel, GlobalPos.of(level.dimension(), worldPosition));
		super.setRemoved();
	}

	/** Lines for Jade. */
	public List<String> info() {
		List<String> out = new ArrayList<>();
		out.add("Channel " + channel);
		int p = getBlockState().getValue(WirelessRedstoneBlock.POWER);
		out.add(receiver() ? (p > 0 ? "Receiving signal " + p : "No signal on this channel") : (p > 0 ? "Sending signal " + p : "Not sending (no redstone in)"));
		return out;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("channel", channel);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		channel = Math.max(1, tag.getInt("channel"));
	}
}
