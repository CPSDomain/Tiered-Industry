package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** Which machines a Machine Connector or Molecular Assembler touches (shared by the Matrix screen and the connector's status message). */
public final class MachineConnectorBlockEntityHelper {
	private MachineConnectorBlockEntityHelper() {}

	/** The sides an interface reaches machines through: a bus only the block it faces, anything else all six. */
	public static Direction[] sides(Level level, BlockPos iface) {
		var state = level.getBlockState(iface);
		if (state.getBlock() instanceof com.robvanblerk.tieredpower.block.StorageBusBlock)
			return new Direction[] {state.getValue(com.robvanblerk.tieredpower.block.StorageBusBlock.FACING)};
		return Direction.values();
	}

	public static List<BlockPos> touching(Level level, BlockPos iface) {
		List<BlockPos> out = new ArrayList<>();
		for (Direction d : sides(level, iface)) {
			BlockPos n = iface.relative(d);
			if (level.getBlockState(n).getBlock() instanceof StorageNetworkBlock) continue;
			BlockEntity be = level.getBlockEntity(n);
			if (be == null || be instanceof ItemPipeBlockEntity || be instanceof FluidPipeBlockEntity) continue;
			if (!(be instanceof MachineBlockEntity) && !be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) continue;
			out.add(n);
		}
		return out;
	}
}
