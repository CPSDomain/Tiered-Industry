package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.block.entity.FusionControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FusionReactorBlockEntity;
import com.robvanblerk.tieredpower.block.entity.PlasmaTiers;

/**
 * Raises a Fusion Reactor's plasma tier (fit Mk I, then II, then III): x2 / x3 / x4 output, with fuel burning x1.5 / x2 /
 * x2.5 as fast - more power from every fuel pair. Right-click a Fusion Reactor or Fusion Controller. Drops back out if
 * the reactor is broken.
 */
public class PlasmaCoilItem extends Item {
	private final int tier; // 1-3

	public PlasmaCoilItem(int tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		var be = level.getBlockEntity(ctx.getClickedPos());
		if (player == null || !(be instanceof FusionControllerBlockEntity || be instanceof FusionReactorBlockEntity)) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		int now = be instanceof FusionControllerBlockEntity c ? c.getPlasmaTier() : ((FusionReactorBlockEntity) be).getPlasmaTier();
		if (now >= tier) {
			player.displayClientMessage(Component.literal("Already plasma " + PlasmaTiers.NAMES[now]).withStyle(ChatFormatting.YELLOW), true);
		} else if (now != tier - 1) {
			player.displayClientMessage(Component.literal("Fit the Plasma Coil " + PlasmaTiers.NAMES[now + 1] + " first").withStyle(ChatFormatting.RED), true);
		} else {
			if (be instanceof FusionControllerBlockEntity c) c.setPlasmaTier(tier); else ((FusionReactorBlockEntity) be).setPlasmaTier(tier);
			if (!player.getAbilities().instabuild) stack.shrink(1);
			level.playSound(null, ctx.getClickedPos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8f, 1.2f);
			player.displayClientMessage(Component.literal("Plasma " + PlasmaTiers.NAMES[tier] + ": x" + PlasmaTiers.OUTPUT[tier] + " output").withStyle(ChatFormatting.GREEN), true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click a Fusion Reactor or Fusion Controller").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("x" + PlasmaTiers.OUTPUT[tier] + " output, fuel burns x" + (PlasmaTiers.BURN_HALVES[tier] / 2.0) + " as fast").withStyle(ChatFormatting.AQUA));
		if (tier > 1) tooltip.add(Component.literal("Needs Plasma Coil " + PlasmaTiers.NAMES[tier - 1] + " fitted first").withStyle(ChatFormatting.DARK_GRAY));
	}
}
