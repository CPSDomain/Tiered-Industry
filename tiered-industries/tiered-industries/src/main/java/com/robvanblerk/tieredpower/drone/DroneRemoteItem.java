package com.robvanblerk.tieredpower.drone;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/**
 * Drone Remote: right-click an inventory to remember it, then right-click a Drone Station to link them (for Fetch
 * and Deliver). Sneak + right-click a station to clear its link.
 */
public class DroneRemoteItem extends Item {
	public DroneRemoteItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		var player = ctx.getPlayer();
		if (player == null) return InteractionResult.PASS;
		BlockPos pos = ctx.getClickedPos();
		var be = level.getBlockEntity(pos);
		if (level.isClientSide()) return be != null ? InteractionResult.SUCCESS : InteractionResult.PASS;
		if (be instanceof DroneStationBlockEntity station) {
			if (player.isShiftKeyDown()) {
				station.setLink(null);
				player.displayClientMessage(Component.literal("Link cleared").withStyle(ChatFormatting.YELLOW), true);
			} else if (!stack.hasTag() || !stack.getTag().contains("pos")) {
				player.displayClientMessage(Component.literal("Right-click an inventory first to remember it").withStyle(ChatFormatting.RED), true);
			} else {
				BlockPos target = BlockPos.of(stack.getTag().getLong("pos"));
				if (!target.closerThan(pos, DroneStationBlockEntity.LINK_RANGE)) {
					player.displayClientMessage(Component.literal("Too far - links reach " + DroneStationBlockEntity.LINK_RANGE + " blocks").withStyle(ChatFormatting.RED), true);
				} else {
					station.setLink(target);
					player.displayClientMessage(Component.literal("Linked to " + target.getX() + ", " + target.getY() + ", " + target.getZ()).withStyle(ChatFormatting.GREEN), true);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, ctx.getClickedFace()).isPresent()) {
			stack.getOrCreateTag().putLong("pos", pos.asLong());
			player.displayClientMessage(Component.literal("Remembered " + level.getBlockState(pos).getBlock().getName().getString()
					+ " - now right-click a Drone Station").withStyle(ChatFormatting.AQUA), true);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (stack.hasTag() && stack.getTag().contains("pos")) {
			BlockPos p = BlockPos.of(stack.getTag().getLong("pos"));
			tooltip.add(Component.literal("Remembers " + p.getX() + ", " + p.getY() + ", " + p.getZ()).withStyle(ChatFormatting.AQUA));
		}
		tooltip.add(Component.literal("Right-click an inventory, then a Drone Station").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains("pos");
	}
}
