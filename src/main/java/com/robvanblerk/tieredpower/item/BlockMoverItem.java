package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Picks up a block that holds things - a chest, a machine, a tank - with everything inside it, and puts it down again
 * somewhere else, contents intact. Right-click a block to pick it up; right-click a surface to put it down.
 */
public class BlockMoverItem extends Item {
	public BlockMoverItem(Properties properties) {
		super(properties);
	}

	public static boolean isFull(ItemStack stack) {
		return stack.getTag() != null && stack.getTag().contains("Block");
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		ItemStack stack = ctx.getItemInHand();
		if (level.isClientSide() || player == null) return InteractionResult.SUCCESS;
		BlockPos pos = ctx.getClickedPos();
		if (!isFull(stack)) {
			BlockState state = level.getBlockState(pos);
			var be = level.getBlockEntity(pos);
			if (be == null) {
				player.displayClientMessage(Component.literal("The Block Mover is for blocks that hold things (chests, machines, tanks)").withStyle(ChatFormatting.RED), true);
				return InteractionResult.FAIL;
			}
			if (state.getDestroySpeed(level, pos) < 0) {
				player.displayClientMessage(Component.literal("That block can't be moved").withStyle(ChatFormatting.RED), true);
				return InteractionResult.FAIL;
			}
			if (!com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, level, pos)) return InteractionResult.FAIL;
			CompoundTag tag = stack.getOrCreateTag();
			tag.put("Block", NbtUtils.writeBlockState(state));
			tag.put("Data", be.saveWithoutMetadata());
			level.removeBlockEntity(pos); // so the block doesn't spill its contents
			level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			player.displayClientMessage(Component.literal("Picked up " + state.getBlock().getName().getString() + " with its contents").withStyle(ChatFormatting.GREEN), true);
			return InteractionResult.CONSUME;
		}
		CompoundTag tag = stack.getTag();
		BlockState state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), tag.getCompound("Block"));
		BlockPlaceContext place = new BlockPlaceContext(ctx);
		BlockPos target = place.getClickedPos();
		if (!place.canPlace() || !level.getBlockState(target).canBeReplaced()) {
			player.displayClientMessage(Component.literal("No room to put it down there").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		level.setBlock(target, state, Block.UPDATE_ALL);
		var be = level.getBlockEntity(target);
		if (be != null) {
			CompoundTag data = tag.getCompound("Data").copy();
			data.putInt("x", target.getX());
			data.putInt("y", target.getY());
			data.putInt("z", target.getZ());
			be.load(data);
			be.setChanged();
		}
		tag.remove("Block");
		tag.remove("Data");
		player.displayClientMessage(Component.literal("Put down " + state.getBlock().getName().getString()).withStyle(ChatFormatting.GREEN), true);
		return InteractionResult.CONSUME;
	}

	@Override public boolean isFoil(ItemStack stack) { return isFull(stack); }
	@Override public int getMaxStackSize(ItemStack stack) { return 1; }

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (isFull(stack)) {
			String name = stack.getTag().getCompound("Block").getString("Name");
			tooltip.add(Component.literal("Holding: " + name).withStyle(ChatFormatting.GOLD));
			tooltip.add(Component.literal("Right-click a surface to put it down").withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(Component.literal("Right-click a chest, machine or tank to pick it up with its contents").withStyle(ChatFormatting.GRAY));
		}
	}
}
