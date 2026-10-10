package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.robvanblerk.tieredpower.block.entity.CableBlockEntity;
import com.robvanblerk.tieredpower.block.entity.EnergyMeterBlockEntity;
import com.robvanblerk.tieredpower.block.entity.PowerMonitorBlockEntity;

/**
 * Right-click a cable for the whole network (in, to machines, into storage, stored), or any block for its energy,
 * what it's generating and its tanks. Reads before the block reacts, so machines don't open their screens.
 */
public class MultimeterItem extends Item {
	public MultimeterItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
		if (player == null || be == null) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (be instanceof CableBlockEntity cable) {
			Readouts.send(player, Readouts.network(cable.getNetwork() == null ? null : cable.getNetwork().getStats()));
		} else if (be instanceof PowerMonitorBlockEntity monitor) {
			Readouts.send(player, Readouts.network(monitor.isConnected() ? monitor.getStats() : null));
		} else if (be instanceof EnergyMeterBlockEntity meter) {
			Readouts.send(player, Readouts.meter(meter));
		} else if (be instanceof com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity pipe) {
			Readouts.send(player, Readouts.itemPipe(pipe));
		} else {
			Readouts.send(player, Readouts.block(be, ctx.getClickedFace()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click a cable: the whole network's power").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Right-click anything else: energy, output and tanks").withStyle(ChatFormatting.GRAY));
	}
}
