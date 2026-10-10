package com.robvanblerk.tieredpower.quantum;

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

/**
 * Quantum Linker: sneak + right-click your main Quantum Chest to record it, then right-click any number of other Quantum
 * Chests (any world) to link them to it. Sneak + right-click a linked chest with an empty linker to make it a main chest
 * again. The linker keeps the main chest, so you can link chests on several planets in one trip.
 */
public class QuantumLinkerItem extends Item {
	public QuantumLinkerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof QuantumChestBlockEntity chest) || player == null) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		ItemStack stack = ctx.getItemInHand();
		var tag = stack.getOrCreateTag();
		if (player.isShiftKeyDown()) {
			if (chest.isMain()) {
				tag.putUUID("main", chest.getId());
				tag.putString("mainAt", ctx.getClickedPos().getX() + " " + ctx.getClickedPos().getY() + " " + ctx.getClickedPos().getZ()
						+ " (" + level.dimension().location().getPath() + ")");
				player.displayClientMessage(Component.literal("Main chest recorded - right-click other Quantum Chests to link them to it").withStyle(ChatFormatting.LIGHT_PURPLE), true);
			} else {
				chest.linkTo(null);
				player.displayClientMessage(Component.literal("Unlinked - this is a main chest again").withStyle(ChatFormatting.YELLOW), true);
			}
			level.playSound(null, ctx.getClickedPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.4f);
			return InteractionResult.CONSUME;
		}
		if (!tag.hasUUID("main")) {
			player.displayClientMessage(Component.literal("Sneak + right-click your main Quantum Chest first").withStyle(ChatFormatting.RED), true);
			return InteractionResult.CONSUME;
		}
		if (chest.getId().equals(tag.getUUID("main"))) {
			player.displayClientMessage(Component.literal("That's the main chest itself").withStyle(ChatFormatting.YELLOW), true);
			return InteractionResult.CONSUME;
		}
		chest.linkTo(tag.getUUID("main"));
		level.playSound(null, ctx.getClickedPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1.2f);
		player.displayClientMessage(Component.literal("Linked: everything put in this chest goes to the main chest at " + tag.getString("mainAt")).withStyle(ChatFormatting.LIGHT_PURPLE), true);
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.hasTag() && stack.getTag().hasUUID("main");
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (stack.hasTag() && stack.getTag().hasUUID("main"))
			tooltip.add(Component.literal("Main chest: " + stack.getTag().getString("mainAt")).withStyle(ChatFormatting.LIGHT_PURPLE));
		else tooltip.add(Component.literal("Sneak + right-click a main Quantum Chest to start").withStyle(ChatFormatting.GRAY));
	}
}
