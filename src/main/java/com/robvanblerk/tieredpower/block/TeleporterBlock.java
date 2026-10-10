package com.robvanblerk.tieredpower.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.robvanblerk.tieredpower.block.entity.TeleporterBlockEntity;

/** Teleporter pad: right-click (empty hand) to name it and choose where it goes; stand on it and crouch to travel. */
public class TeleporterBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);

	public TeleporterBlock(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state,
			net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return level.isClientSide() ? null
				: createTickerHelper(type, com.robvanblerk.tieredpower.registry.ModBlockEntities.TELEPORTER.get(), TeleporterBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TeleporterBlockEntity pad) pad.unregister();
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof TeleporterBlockEntity pad) {
			pad.register();
			com.robvanblerk.tieredpower.network.ModNetwork.openTeleporter(sp, pad);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextFloat() < 0.3f) {
			level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 0.25, pos.getZ() + random.nextDouble(), 0, 0.3, 0);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Link two pads with a Teleporter Linker, then crouch on one to travel").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TeleporterBlockEntity(pos, state);
	}
}
