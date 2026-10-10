package com.robvanblerk.tieredpower.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/** Moving items into neighbouring inventories (chests, pipes, other mods' storage). */
public final class ItemUtil {
	/** Tries to put the stack into any neighbouring inventory. Returns whatever didn't fit. */
	public static ItemStack pushToNeighbours(Level level, BlockPos pos, ItemStack stack) {
		ItemStack remaining = stack;
		for (Direction dir : Direction.values()) {
			if (remaining.isEmpty()) break;
			IItemHandler target = neighbour(level, pos, dir);
			if (target != null) remaining = ItemHandlerHelper.insertItemStacked(target, remaining, false);
		}
		return remaining;
	}

	public static IItemHandler neighbour(Level level, BlockPos pos, Direction dir) {
		BlockPos next = pos.relative(dir);
		if (!level.isLoaded(next)) return null;
		BlockEntity be = level.getBlockEntity(next);
		if (be == null) return null;
		return be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
	}

	private ItemUtil() {}
}
