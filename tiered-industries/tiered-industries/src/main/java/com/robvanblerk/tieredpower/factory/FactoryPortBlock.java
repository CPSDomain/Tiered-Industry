package com.robvanblerk.tieredpower.factory;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Factory Port: connects pipes, cables and hoppers to a Digital Factory from anywhere on its structure. */
public class FactoryPortBlock extends BaseEntityBlock {
	public FactoryPortBlock(Properties properties) {
		super(properties);
	}

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FactoryPortBlockEntity(pos, state); }
}
