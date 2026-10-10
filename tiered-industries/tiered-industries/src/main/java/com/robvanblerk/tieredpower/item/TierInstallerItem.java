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

/**
 * Upgrades a processing machine one tier, in place (contents and settings kept): Advanced runs 3 operations at once,
 * Elite 5, Ultimate 7, Quantum 9. Tiers go in order. The installer drops back out if the machine is broken.
 */
public class TierInstallerItem extends Item {
	private final int tier; // 1 = Advanced ... 4 = Quantum

	public TierInstallerItem(int tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof MachineBlockEntity m) || player == null) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		String name = MachineBlockEntity.TIER_NAMES[tier];
		if (!m.supportsTiers()) {
			player.displayClientMessage(Component.literal("This machine can't be upgraded with installers").withStyle(ChatFormatting.RED), true);
		} else if (m.getTier() >= tier) {
			player.displayClientMessage(Component.literal("Already " + MachineBlockEntity.TIER_NAMES[m.getTier()]).withStyle(ChatFormatting.YELLOW), true);
		} else if (m.getTier() != tier - 1) {
			player.displayClientMessage(Component.literal("Needs the " + MachineBlockEntity.TIER_NAMES[m.getTier() + 1] + " Installer first").withStyle(ChatFormatting.RED), true);
		} else {
			m.setTier(tier);
			if (!player.getAbilities().instabuild) stack.shrink(1);
			level.playSound(null, ctx.getClickedPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1f, 1.1f);
			player.displayClientMessage(Component.literal("Upgraded to " + name + ": " + m.tierEffect(tier)).withStyle(ChatFormatting.GREEN), true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click a " + MachineBlockEntity.TIER_NAMES[tier - 1] + " processing machine:").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("it becomes " + MachineBlockEntity.TIER_NAMES[tier] + " and runs " + MachineBlockEntity.TIER_LANES[tier] + " operations at once").withStyle(ChatFormatting.AQUA));
		int size = com.robvanblerk.tieredpower.block.entity.ChunkLoaderBlockEntity.MAX_RADIUS[tier] * 2 + 1;
		tooltip.add(Component.literal("Chunk Loader: up to " + size + "x" + size + " chunks").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.literal("Furnace, Pulverizer, Alloy Smelter, Compressor, Sawmill, Ore Purifier, Rock Crusher, Fluid Mixer, Chemical Washer").withStyle(ChatFormatting.DARK_GRAY));
	}
}
