package com.robvanblerk.tieredpower.core;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.energy.PowerInfo;

/** Right-click an Energy Core of the tier below to raise it to this tier (II-V). */
public class CoreUpgradeItem extends Item {
	private final int tier;

	public CoreUpgradeItem(int tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof EnergyCoreBlockEntity core) || ctx.getPlayer() == null) return InteractionResult.PASS;
		if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
		var p = ctx.getPlayer();
		if (core.getTier() >= tier) {
			p.displayClientMessage(Component.literal("This core is already tier " + core.getTier()).withStyle(ChatFormatting.YELLOW), true);
		} else if (core.getTier() != tier - 1) {
			p.displayClientMessage(Component.literal("Upgrade it to tier " + (core.getTier() + 1) + " first").withStyle(ChatFormatting.RED), true);
		} else {
			core.upgrade();
			if (!p.getAbilities().instabuild) stack.shrink(1);
			ctx.getLevel().playSound(null, ctx.getClickedPos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 0.8f + 0.1f * tier);
			p.displayClientMessage(Component.literal("Energy Core is now tier " + tier + ": " + PowerInfo.shortFe(core.getCapacity()) + " FE").withStyle(ChatFormatting.GREEN), true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(String.format("Right-click a tier %d Energy Core: tier %d, %s FE, %s FE/t per pylon", tier - 1, tier,
				PowerInfo.shortFe(EnergyCoreBlockEntity.CAPACITY[tier - 1]), PowerInfo.shortFe(EnergyCoreBlockEntity.PYLON_RATE[tier - 1]))).withStyle(ChatFormatting.AQUA));
	}
}
