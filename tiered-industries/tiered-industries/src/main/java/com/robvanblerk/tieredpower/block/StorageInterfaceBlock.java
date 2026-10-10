package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.StorageInterfaceBlockEntity;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** Lets anything next to it push into, or pull out of, the storage network. */
public class StorageInterfaceBlock extends BaseEntityBlock implements StorageNetworkBlock {
	public StorageInterfaceBlock(Properties properties) {
		super(properties);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageInterfaceBlockEntity(pos, state);
	}
}
