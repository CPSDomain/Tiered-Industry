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

/** Breaks the block in front of it (like a player with a diamond pickaxe) and keeps the drops. 400 FE per block. */
public class BlockBreakerBlockEntity extends WorkerBlockEntity {
	public BlockBreakerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.BLOCK_BREAKER.get(), pos, state); }
	@Override protected int costPerAction() { return 400; }
	@Override protected int interval() { return 20; }
	@Override public boolean isValidInput(ItemStack stack) { return false; }
	@Override protected MenuType<WorkerMenu> menuType() { return ModMenus.BLOCK_BREAKER.get(); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.block_breaker"); }

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, Direction facing) {
		BlockPos front = pos.relative(facing);
		BlockState s = level.getBlockState(front);
		if (s.isAir() || s.getDestroySpeed(level, front) < 0 || !s.getFluidState().isEmpty() && s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) return false;
		var drops = Block.getDrops(s, level, front, level.getBlockEntity(front), fakePlayer(level), new ItemStack(Items.DIAMOND_PICKAXE));
		level.destroyBlock(front, false);
		for (ItemStack d : drops) outputOrDrop(level, front, d);
		return true;
	}
}
