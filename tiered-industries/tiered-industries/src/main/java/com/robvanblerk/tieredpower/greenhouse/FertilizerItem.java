package com.robvanblerk.tieredpower.greenhouse;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Strong bone meal: one use grows a plant three times over. The Crop Farmer uses it automatically from its seed slots. */
public class FertilizerItem extends Item {
	public FertilizerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		ItemStack probe = new ItemStack(Items.BONE_MEAL, 3);
		boolean grew = false;
		for (int i = 0; i < 3; i++) if (BoneMealItem.growCrop(probe, level, ctx.getClickedPos())) grew = true;
		if (!grew) return InteractionResult.PASS;
		if (!level.isClientSide()) {
			level.levelEvent(1505, ctx.getClickedPos(), 0); // the bone meal sparkle
			if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Like three bone meal in one. Crop Farmers use it automatically").withStyle(ChatFormatting.GRAY));
	}
}
