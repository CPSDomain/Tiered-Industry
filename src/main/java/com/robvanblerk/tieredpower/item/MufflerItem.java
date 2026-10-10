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

import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;

/** Silences a machine: right-click it to fit. Take it off again from the machine's Upgrades window. */
public class MufflerItem extends Item {
	public MufflerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof MachineBlockEntity m) || player == null) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (m.isMuffled()) {
			player.displayClientMessage(Component.literal("Already muffled").withStyle(ChatFormatting.YELLOW), true);
		} else {
			m.setMuffled(true);
			if (!player.getAbilities().instabuild) stack.shrink(1);
			level.playSound(null, ctx.getClickedPos(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 1f);
			player.displayClientMessage(Component.literal("Muffler fitted - this machine is now silent").withStyle(ChatFormatting.GREEN), true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click a machine to silence it").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Remove it from the machine's Upgrades window").withStyle(ChatFormatting.DARK_GRAY));
	}
}
