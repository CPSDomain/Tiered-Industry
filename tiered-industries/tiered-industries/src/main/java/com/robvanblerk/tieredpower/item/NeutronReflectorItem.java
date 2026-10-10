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

import com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FissionReactorBlockEntity;

/** Bounces stray neutrons back into the core: fuel rods last 50% longer. Right-click a fission reactor to fit. */
public class NeutronReflectorItem extends Item {
	public NeutronReflectorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		var be = level.getBlockEntity(ctx.getClickedPos());
		if (player == null || !(be instanceof FissionReactorBlockEntity || be instanceof FissionControllerBlockEntity)) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		boolean has = be instanceof FissionReactorBlockEntity r ? r.hasReflector() : ((FissionControllerBlockEntity) be).hasReflector();
		if (has) {
			player.displayClientMessage(Component.literal("This reactor already has a Neutron Reflector").withStyle(ChatFormatting.YELLOW), true);
		} else {
			if (be instanceof FissionReactorBlockEntity r) r.setReflector(true); else ((FissionControllerBlockEntity) be).setReflector(true);
			if (!player.getAbilities().instabuild) stack.shrink(1);
			level.playSound(null, ctx.getClickedPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
			player.displayClientMessage(Component.literal("Neutron Reflector fitted: fuel rods last 50% longer").withStyle(ChatFormatting.GREEN), true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click a Fission Reactor or Fission Controller").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Fuel rods last 50% longer. Drops back out if the reactor is broken.").withStyle(ChatFormatting.AQUA));
	}
}
