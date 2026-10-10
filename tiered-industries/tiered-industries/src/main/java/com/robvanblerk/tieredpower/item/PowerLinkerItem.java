package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.block.entity.PowerReceiverBlockEntity;
import com.robvanblerk.tieredpower.block.entity.PowerTransmitterBlockEntity;

/** Links Power Receivers to a Power Transmitter: right-click the transmitter, then each receiver (any distance, any dimension). */
public class PowerLinkerItem extends Item {
	public PowerLinkerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		var be = level.getBlockEntity(ctx.getClickedPos());
		if (player == null) return InteractionResult.PASS;
		if (be instanceof com.robvanblerk.tieredpower.block.entity.WirelessSenderBlockEntity || be instanceof com.robvanblerk.tieredpower.block.entity.WirelessReceiverBlockEntity)
			return linkTransport(stack, ctx, level, player, be);
		if (!(be instanceof PowerTransmitterBlockEntity || be instanceof PowerReceiverBlockEntity)) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		BlockPos pos = ctx.getClickedPos();
		if (be instanceof PowerTransmitterBlockEntity) {
			CompoundTag tag = stack.getOrCreateTag();
			tag.putLong("Transmitter", pos.asLong());
			tag.putString("Dim", level.dimension().location().toString());
			player.displayClientMessage(Component.literal("Transmitter remembered - now right-click Power Receivers to link them").withStyle(ChatFormatting.AQUA), true);
		} else if (be instanceof PowerReceiverBlockEntity receiver) {
			CompoundTag tag = stack.getTag();
			ResourceLocation dim = tag == null || !tag.contains("Transmitter") ? null : ResourceLocation.tryParse(tag.getString("Dim"));
			if (dim == null) {
				player.displayClientMessage(Component.literal("Right-click a Power Transmitter first").withStyle(ChatFormatting.RED), true);
			} else {
				BlockPos t = BlockPos.of(tag.getLong("Transmitter"));
				receiver.link(ResourceKey.create(Registries.DIMENSION, dim), t);
				boolean other = !dim.equals(level.dimension().location());
				player.displayClientMessage(Component.literal("Linked to the transmitter at " + t.toShortString() + (other ? " in " + dim + " (10% loss across dimensions)" : ""))
						.withStyle(ChatFormatting.GREEN), true);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Wireless item/fluid transport: right-click a Wireless Sender, then Wireless Receivers. */
	private InteractionResult linkTransport(ItemStack stack, UseOnContext ctx, Level level, Player player, net.minecraft.world.level.block.entity.BlockEntity be) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		BlockPos pos = ctx.getClickedPos();
		if (be instanceof com.robvanblerk.tieredpower.block.entity.WirelessSenderBlockEntity) {
			CompoundTag tag = stack.getOrCreateTag();
			tag.putLong("Sender", pos.asLong());
			tag.putString("SenderDim", level.dimension().location().toString());
			player.displayClientMessage(Component.literal("Sender remembered - now right-click Wireless Receivers to link them").withStyle(ChatFormatting.AQUA), true);
		} else if (be instanceof com.robvanblerk.tieredpower.block.entity.WirelessReceiverBlockEntity receiver) {
			CompoundTag tag = stack.getTag();
			ResourceLocation dim = tag == null || !tag.contains("Sender") ? null : ResourceLocation.tryParse(tag.getString("SenderDim"));
			if (dim == null) {
				player.displayClientMessage(Component.literal("Right-click a Wireless Sender first").withStyle(ChatFormatting.RED), true);
			} else {
				BlockPos s = BlockPos.of(tag.getLong("Sender"));
				receiver.link(ResourceKey.create(Registries.DIMENSION, dim), s);
				player.displayClientMessage(Component.literal("Linked to the Wireless Sender at " + s.toShortString()).withStyle(ChatFormatting.GREEN), true);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Power: right-click a Power Transmitter, then Power Receivers").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Items/fluids: right-click a Wireless Sender, then Wireless Receivers").withStyle(ChatFormatting.GRAY));
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains("Transmitter"))
			tooltip.add(Component.literal("Transmitter: " + BlockPos.of(tag.getLong("Transmitter")).toShortString() + " (" + tag.getString("Dim") + ")").withStyle(ChatFormatting.AQUA));
		if (tag != null && tag.contains("Sender"))
			tooltip.add(Component.literal("Sender: " + BlockPos.of(tag.getLong("Sender")).toShortString() + " (" + tag.getString("SenderDim") + ")").withStyle(ChatFormatting.AQUA));
	}
}
