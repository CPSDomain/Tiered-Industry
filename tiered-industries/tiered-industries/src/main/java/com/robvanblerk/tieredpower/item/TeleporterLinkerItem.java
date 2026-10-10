package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.block.entity.TeleporterBlockEntity;

/** Right-click one Teleporter pad, then another: they're linked both ways. Works across dimensions. */
public class TeleporterLinkerItem extends Item {
	public TeleporterLinkerItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Player player = ctx.getPlayer();
		if (!(level.getBlockEntity(pos) instanceof TeleporterBlockEntity pad)) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;

		ItemStack stack = ctx.getItemInHand();
		CompoundTag tag = stack.getOrCreateTag();
		ResourceLocation here = level.dimension().location();
		if (!tag.contains("pad")) {
			tag.put("pad", NbtUtils.writeBlockPos(pos));
			tag.putString("dim", here.toString());
			message(player, "First pad stored - now right-click the other pad");
			return InteractionResult.CONSUME;
		}
		BlockPos first = NbtUtils.readBlockPos(tag.getCompound("pad"));
		ResourceLocation firstDim = ResourceLocation.tryParse(tag.getString("dim"));
		tag.remove("pad");
		tag.remove("dim");
		if (first.equals(pos) && here.equals(firstDim)) {
			message(player, "That's the same pad - link cleared, start again");
			return InteractionResult.CONSUME;
		}
		ServerLevel firstLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, firstDim));
		if (firstLevel == null || !(firstLevel.getBlockEntity(first) instanceof TeleporterBlockEntity other)) {
			message(player, "The first pad is gone - start again");
			return InteractionResult.CONSUME;
		}
		pad.link(firstDim, first);
		other.link(here, pos);
		message(player, "Pads linked! Crouch on one to travel to the other.");
		return InteractionResult.CONSUME;
	}

	private static void message(@Nullable Player player, String text) {
		if (player != null) player.displayClientMessage(Component.literal(text), true);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains("pad");
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (stack.hasTag() && stack.getTag().contains("pad")) {
			tooltip.add(Component.literal("First pad: " + NbtUtils.readBlockPos(stack.getTag().getCompound("pad")).toShortString()).withStyle(ChatFormatting.AQUA));
		}
		tooltip.add(Component.literal("Right-click a Teleporter pad, then another, to link them").withStyle(ChatFormatting.GRAY));
	}
}
