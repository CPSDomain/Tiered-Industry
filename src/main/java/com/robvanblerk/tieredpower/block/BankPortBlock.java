package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.block.entity.BankPortBlockEntity;
import com.robvanblerk.tieredpower.energy.SideMode;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Wall block of the Energy Bank. Input or Output; shift + right-click with an empty hand to switch. */
public class BankPortBlock extends BaseEntityBlock {
	public static final EnumProperty<SideMode> MODE = EnumProperty.create("mode", SideMode.class, SideMode.INPUT, SideMode.OUTPUT);

	public BankPortBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(MODE, SideMode.INPUT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MODE);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			if (!level.isClientSide()) {
				SideMode mode = state.getValue(MODE) == SideMode.INPUT ? SideMode.OUTPUT : SideMode.INPUT;
				level.setBlock(pos, state.setValue(MODE, mode), Block.UPDATE_ALL);
				player.displayClientMessage(Component.literal("Bank Port: " + mode.displayName()), true);
			}
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		return InteractionResult.PASS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BankPortBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BANK_PORT.get(), BankPortBlockEntity::tick);
	}
}
