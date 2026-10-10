package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.ReactorPortBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** A face block of the multiblock reactor that passes items (Fuel Port) or power (Power Port) to the controller. */
public class ReactorPortBlock extends BaseEntityBlock {
	public enum Kind { FUEL, POWER }

	private final Kind kind;

	public ReactorPortBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind getKind() {
		return kind;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ReactorPortBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || kind != Kind.POWER ? null
				: createTickerHelper(type, ModBlockEntities.REACTOR_PORT.get(), ReactorPortBlockEntity::tick);
	}
}
