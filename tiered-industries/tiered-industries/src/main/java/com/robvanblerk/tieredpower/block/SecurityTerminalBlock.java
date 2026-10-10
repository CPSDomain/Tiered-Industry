package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.block.entity.SecurityTerminalBlockEntity;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** Locks the storage network it's on to its owner and the players they trust. */
public class SecurityTerminalBlock extends BaseEntityBlock implements StorageNetworkBlock {
	public SecurityTerminalBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && placer instanceof Player p && level.getBlockEntity(pos) instanceof SecurityTerminalBlockEntity be) {
			be.setOwner(p);
			p.displayClientMessage(Component.literal("This storage network is now locked to you - right-click to add players").withStyle(ChatFormatting.GREEN), true);
		}
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SecurityTerminalBlockEntity be) {
			if (be.isOwner(player)) player.openMenu(be);
			else player.displayClientMessage(Component.literal("Only " + be.getOwnerName() + " can change who has access").withStyle(ChatFormatting.RED), true);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecurityTerminalBlockEntity(pos, state);
	}
}
