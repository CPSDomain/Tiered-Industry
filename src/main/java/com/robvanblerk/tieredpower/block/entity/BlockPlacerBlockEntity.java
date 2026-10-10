package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.robvanblerk.tieredpower.menu.WorkerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Places blocks from its input slots in front of it. 100 FE per block. */
public class BlockPlacerBlockEntity extends WorkerBlockEntity {
	public static final int SPECIAL_NO_BLOCKS = 1;
	public BlockPlacerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.BLOCK_PLACER.get(), pos, state); }
	@Override protected int costPerAction() { return 100; }
	@Override protected int interval() { return 10; }
	@Override public boolean isValidInput(ItemStack stack) { return stack.getItem() instanceof BlockItem; }
	@Override protected MenuType<WorkerMenu> menuType() { return ModMenus.BLOCK_PLACER.get(); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.block_placer"); }

	@Override
	protected boolean outputHasRoom() { return true; } // it has nothing to output

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, Direction facing) {
		BlockPos front = pos.relative(facing);
		if (!level.getBlockState(front).canBeReplaced()) return false;
		int slot = findInput(s -> s.getItem() instanceof BlockItem);
		if (slot < 0) {
			status = STATUS_SPECIAL;
			special = SPECIAL_NO_BLOCKS;
			return false;
		}
		ItemStack stack = items.get(slot);
		var player = fakePlayer(level);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		var ctx = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(front), facing.getOpposite(), front, false));
		boolean placed = ((BlockItem) stack.getItem()).place(ctx).consumesAction();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		setChanged();
		return placed;
	}
}
