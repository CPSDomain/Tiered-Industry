package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.block.entity.QuarryBlockEntity;

/**
 * Marks out exactly where a Quarry digs: right-click one corner, then the opposite corner, then the Quarry. It digs that
 * rectangle (up to 65 x 65) from the higher corner's level down. Sneak + right-click in the air to clear the corners.
 */
public class QuarryPlannerItem extends Item {
	public QuarryPlannerItem(Properties properties) {
		super(properties);
	}

	private static @Nullable BlockPos corner(ItemStack stack, String key) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(key) ? NbtUtils.readBlockPos(tag.getCompound(key)) : null;
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		if (player == null) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		BlockPos pos = ctx.getClickedPos();
		BlockPos a = corner(stack, "CornerA"), b = corner(stack, "CornerB");
		if (level.getBlockEntity(pos) instanceof QuarryBlockEntity quarry) {
			if (a == null || b == null) {
				player.displayClientMessage(Component.literal("Mark two corners first (right-click blocks)").withStyle(ChatFormatting.RED), true);
			} else if (!quarry.setCustomArea(a, b)) {
				player.displayClientMessage(Component.literal("Too big - the most is " + QuarryBlockEntity.MAX_CUSTOM_SIZE + " x " + QuarryBlockEntity.MAX_CUSTOM_SIZE).withStyle(ChatFormatting.RED), true);
			} else {
				int w = Math.abs(a.getX() - b.getX()) + 1, l = Math.abs(a.getZ() - b.getZ()) + 1;
				player.displayClientMessage(Component.literal("Quarry will dig " + w + " x " + l + " from Y " + Math.max(a.getY(), b.getY()) + " down").withStyle(ChatFormatting.GREEN), true);
			}
			return InteractionResult.SUCCESS;
		}
		CompoundTag tag = stack.getOrCreateTag();
		if (a == null || b != null) { // start a new pair
			tag.put("CornerA", NbtUtils.writeBlockPos(pos));
			tag.remove("CornerB");
			player.displayClientMessage(Component.literal("Corner 1 set at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " - now the opposite corner").withStyle(ChatFormatting.AQUA), true);
		} else {
			tag.put("CornerB", NbtUtils.writeBlockPos(pos));
			int w = Math.abs(a.getX() - pos.getX()) + 1, l = Math.abs(a.getZ() - pos.getZ()) + 1;
			player.displayClientMessage(Component.literal("Corner 2 set: " + w + " x " + l + " - now right-click your Quarry").withStyle(ChatFormatting.AQUA), true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown() && stack.getTag() != null) {
			if (!level.isClientSide()) {
				stack.getTag().remove("CornerA");
				stack.getTag().remove("CornerB");
				player.displayClientMessage(Component.literal("Corners cleared"), true);
			}
			return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		return net.minecraft.world.InteractionResultHolder.pass(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		BlockPos a = corner(stack, "CornerA"), b = corner(stack, "CornerB");
		tooltip.add(Component.literal("1) Right-click a corner  2) the opposite corner  3) your Quarry").withStyle(ChatFormatting.GRAY));
		if (a != null) tooltip.add(Component.literal("Corner 1: " + a.getX() + ", " + a.getY() + ", " + a.getZ()).withStyle(ChatFormatting.AQUA));
		if (b != null) tooltip.add(Component.literal("Corner 2: " + b.getX() + ", " + b.getY() + ", " + b.getZ()).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.literal("Sneak + right-click the air to clear").withStyle(ChatFormatting.DARK_GRAY));
	}
}
