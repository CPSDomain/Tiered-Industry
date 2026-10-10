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

/**
 * Plants saplings on a 9x9 patch in front of it, speeds them up with bone meal if it has some, and fells grown trees -
 * logs and leaves - keeping the wood, saplings, sticks and apples. 200 FE per job.
 */
public class TreeFarmBlockEntity extends WorkerBlockEntity {
	public static final int RADIUS = 4, MAX_TREE = 256, SPECIAL_NO_SAPLINGS = 1;
	private int cursor;

	public TreeFarmBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.TREE_FARM.get(), pos, state); }
	@Override protected int costPerAction() { return 200; }
	@Override protected int interval() { return 5; }
	@Override public boolean isValidInput(ItemStack stack) { return stack.is(ItemTags.SAPLINGS) || stack.is(Items.BONE_MEAL); }
	@Override protected MenuType<WorkerMenu> menuType() { return ModMenus.TREE_FARM.get(); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.tree_farm"); }

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, Direction facing) {
		BlockPos centre = pos.relative(facing, RADIUS + 1);
		int side = RADIUS * 2 + 1;
		for (int tries = 0; tries < side * side; tries++) { // look at the next spot until something needs doing
			cursor = (cursor + 1) % (side * side);
			BlockPos spot = centre.offset(cursor % side - RADIUS, 0, cursor / side - RADIUS);
			BlockState s = level.getBlockState(spot);
			if (s.is(BlockTags.LOGS)) { fell(level, spot); return true; }
			if (s.is(BlockTags.SAPLINGS)) {
				int meal = findInput(st -> st.is(Items.BONE_MEAL));
				if (meal >= 0 && BoneMealItem.growCrop(items.get(meal), level, spot)) { setChanged(); return true; }
				continue;
			}
			if (s.isAir()) {
				int sap = findInput(st -> st.is(ItemTags.SAPLINGS));
				if (sap < 0) { status = STATUS_SPECIAL; special = SPECIAL_NO_SAPLINGS; continue; }
				BlockState sapling = ((BlockItem) items.get(sap).getItem()).getBlock().defaultBlockState();
				if (sapling.canSurvive(level, spot)) {
					level.setBlock(spot, sapling, Block.UPDATE_ALL);
					items.get(sap).shrink(1);
					setChanged();
					return true;
				}
			}
		}
		return false;
	}

	/** Breaks the whole tree: connected logs and leaves, up to 256 blocks. */
	private void fell(ServerLevel level, BlockPos start) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		var player = fakePlayer(level);
		while (!queue.isEmpty() && seen.size() < MAX_TREE) {
			BlockPos p = queue.poll();
			if (!seen.add(p)) continue;
			BlockState s = level.getBlockState(p);
			if (!s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES)) continue;
			for (ItemStack d : Block.getDrops(s, level, p, null, player, ItemStack.EMPTY)) outputOrDrop(level, p, d);
			level.destroyBlock(p, false);
			for (int dx = -1; dx <= 1; dx++)
				for (int dy = 0; dy <= 1; dy++)
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos n = p.offset(dx, dy, dz);
						if (!seen.contains(n) && Math.abs(n.getX() - start.getX()) <= 12 && Math.abs(n.getZ() - start.getZ()) <= 12) queue.add(n);
					}
		}
	}
}
