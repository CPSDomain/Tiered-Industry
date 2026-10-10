package com.robvanblerk.tieredpower.item.powered;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Powered chainsaw: cuts anything an axe can plus leaves. Breaking a log fells the whole tree (sneak to cut just one). */
public class ElectricChainsawItem extends ElectricDrillItem {
	public static final int MAX_LOGS = 128;
	private static boolean felling;

	public ElectricChainsawItem(Tier tier, TagKey<Block> mineable, int capacity, int costPerBlock, Properties properties) {
		super(tier, mineable, capacity, costPerBlock, false, properties);
	}

	@Override
	public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
		if (felling || player.isShiftKeyDown() || !(player instanceof ServerPlayer server)) return false;
		Level level = player.level();
		BlockState start = level.getBlockState(pos);
		if (!start.is(BlockTags.LOGS)) return false;

		// Find every log of the same type connected to this one (including diagonally), going up and sideways.
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(pos);
		seen.add(pos);
		felling = true;
		try {
			int cut = 0;
			while (!queue.isEmpty() && cut < MAX_LOGS) {
				BlockPos p = queue.poll();
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = 0; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos n = p.offset(dx, dy, dz);
							if (seen.contains(n) || !level.getBlockState(n).is(start.getBlock())) continue;
							seen.add(n);
							queue.add(n);
						}
					}
				}
				if (!p.equals(pos)) {
					if (ItemEnergy.get(stack) < cost()) break;
					server.gameMode.destroyBlock(p);
					cut++;
				}
			}
		} finally {
			felling = false;
		}
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("Fells whole trees (sneak to cut one log)").withStyle(ChatFormatting.GRAY));
	}
}
