package com.robvanblerk.tieredpower.item.powered;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * Right-click: lists the ores within 16 blocks - how many of each, and the direction and distance to the nearest one.
 * 1,000,000 FE; 5,000 FE per scan.
 */
public class OreScannerItem extends Item {
	public static final int CAPACITY = 1_000_000, PER_SCAN = 5_000, RADIUS = 16;

	public OreScannerItem(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return ItemEnergy.provider(stack, CAPACITY, 50_000, false);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) return InteractionResultHolder.success(stack);
		if (!player.getAbilities().instabuild && !ItemEnergy.use(stack, PER_SCAN)) {
			player.displayClientMessage(Component.literal("Ore Scanner needs charging").withStyle(ChatFormatting.RED), true);
			return InteractionResultHolder.fail(stack);
		}
		BlockPos at = player.blockPosition();
		Map<Block, int[]> found = new LinkedHashMap<>(); // count, nearest distance squared
		Map<Block, BlockPos> nearest = new LinkedHashMap<>();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = -RADIUS; x <= RADIUS; x++)
			for (int y = -RADIUS; y <= RADIUS; y++)
				for (int z = -RADIUS; z <= RADIUS; z++) {
					p.set(at.getX() + x, at.getY() + y, at.getZ() + z);
					var state = level.getBlockState(p);
					if (!state.is(Tags.Blocks.ORES)) continue;
					int d2 = x * x + y * y + z * z;
					int[] e = found.computeIfAbsent(state.getBlock(), b -> new int[]{0, Integer.MAX_VALUE});
					e[0]++;
					if (d2 < e[1]) { e[1] = d2; nearest.put(state.getBlock(), p.immutable()); }
				}
		player.sendSystemMessage(Component.literal("Ore Scanner - within " + RADIUS + " blocks:").withStyle(ChatFormatting.GOLD));
		if (found.isEmpty()) {
			player.sendSystemMessage(Component.literal("  No ores found").withStyle(ChatFormatting.GRAY));
			return InteractionResultHolder.consume(stack);
		}
		List<Map.Entry<Block, int[]>> list = new ArrayList<>(found.entrySet());
		list.sort((a, b) -> Integer.compare(a.getValue()[1], b.getValue()[1]));
		for (int i = 0; i < list.size() && i < 10; i++) {
			Block b = list.get(i).getKey();
			int count = list.get(i).getValue()[0];
			BlockPos n = nearest.get(b);
			player.sendSystemMessage(Component.literal(String.format("  %s x%d - nearest %s", b.getName().getString(), count, describe(at, n)))
					.withStyle(ChatFormatting.AQUA));
		}
		return InteractionResultHolder.consume(stack);
	}

	/** "7 blocks north-east, 4 down". */
	private static String describe(BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX(), dy = to.getY() - from.getY(), dz = to.getZ() - from.getZ();
		int flat = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
		String ns = dz < -1 ? "north" : dz > 1 ? "south" : "";
		String ew = dx < -1 ? "west" : dx > 1 ? "east" : "";
		String dir = ns.isEmpty() ? ew : ew.isEmpty() ? ns : ns + "-" + ew;
		String h = flat == 0 ? "right here" : flat + " block" + (flat == 1 ? "" : "s") + (dir.isEmpty() ? "" : " " + dir);
		String v = dy == 0 ? "" : ", " + Math.abs(dy) + (dy > 0 ? " up" : " down");
		return h + v;
	}

	@Override public boolean isBarVisible(ItemStack stack) { return true; }
	@Override public int getBarWidth(ItemStack stack) { return Math.round(13f * ItemEnergy.get(stack) / CAPACITY); }
	@Override public int getBarColor(ItemStack stack) { return 0x3FB8FF; }

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(String.format("%,d / %,d FE", ItemEnergy.get(stack), CAPACITY)).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Right-click: list ores within 16 blocks, with directions").withStyle(ChatFormatting.AQUA));
	}
}
