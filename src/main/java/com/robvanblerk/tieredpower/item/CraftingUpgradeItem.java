package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.storage.CraftingTier;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/** Right-click a Crafting CPU, Crafting Accelerator or Machine Connector of the tier below to raise it to this tier (1 Advanced - 3 Quantum). */
public class CraftingUpgradeItem extends Item {
	private final int tier;

	public CraftingUpgradeItem(int tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public int tier() {
		return tier;
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		var pos = ctx.getClickedPos();
		var state = level.getBlockState(pos);
		boolean cpu = state.is(ModBlocks.CRAFTING_CPU.get()), accel = state.is(ModBlocks.CRAFTING_ACCELERATOR.get()),
				connector = state.is(ModBlocks.MACHINE_CONNECTOR.get());
		if ((!cpu && !accel && !connector) || ctx.getPlayer() == null) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		var p = ctx.getPlayer();
		if (!com.robvanblerk.tieredpower.storage.StorageSecurity.check(p, level, pos)) return InteractionResult.CONSUME;
		int now = CraftingTier.of(state);
		String what = cpu ? "Crafting CPU" : accel ? "Crafting Accelerator" : "Machine Connector";
		if (now >= tier) {
			p.displayClientMessage(Component.literal("This " + what + " is already " + CraftingTier.NAMES[now]).withStyle(ChatFormatting.YELLOW), true);
		} else if (now != tier - 1) {
			p.displayClientMessage(Component.literal("Upgrade it to " + CraftingTier.NAMES[now + 1] + " first").withStyle(ChatFormatting.RED), true);
		} else {
			level.setBlock(pos, state.setValue(CraftingTier.TIER, tier), 3);
			if (!p.getAbilities().instabuild) stack.shrink(1);
			level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 0.9f + 0.15f * tier);
			StorageNetwork.changed();
			String gain = cpu ? CraftingTier.CPU_JOBS[tier] + " jobs at once"
					: accel ? "+" + CraftingTier.ACCELERATOR_CRAFTS[tier] + " crafts per cycle"
					: CraftingTier.CONNECTOR_OPS[tier] + " operations per cycle";
			p.displayClientMessage(Component.literal(what + " is now " + CraftingTier.NAMES[tier] + ": " + gain)
					.withStyle(Style.EMPTY.withColor(CraftingTier.COLOUR[tier])), true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click a " + CraftingTier.NAMES[tier - 1] + " Crafting CPU, Crafting Accelerator or Machine Connector to make it " + CraftingTier.NAMES[tier])
				.withStyle(ChatFormatting.GRAY));
		Style st = Style.EMPTY.withColor(CraftingTier.COLOUR[tier]);
		tooltip.add(Component.literal("CPU: " + CraftingTier.CPU_JOBS[tier] + " jobs at once").withStyle(st));
		tooltip.add(Component.literal("Accelerator: +" + CraftingTier.ACCELERATOR_CRAFTS[tier] + " crafts per cycle").withStyle(st));
		tooltip.add(Component.literal("Machine Connector: " + CraftingTier.CONNECTOR_OPS[tier] + " operations per cycle").withStyle(st));
	}
}
