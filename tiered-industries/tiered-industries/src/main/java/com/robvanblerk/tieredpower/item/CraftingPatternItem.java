package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.storage.CraftingPattern;

/** An encoded crafting recipe. Goes in a Molecular Assembler. Sneak + right-click in the air to wipe it back to blank. */
public class CraftingPatternItem extends Item {
	public CraftingPatternItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
		if (!level.isClientSide()) player.setItemInHand(hand, new ItemStack(ModBlocks.BLANK_PATTERN.get(), stack.getCount()));
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public Component getName(ItemStack stack) {
		CraftingPattern p = CraftingPattern.fromStack(stack);
		if (p == null) return super.getName(stack);
		Component what = p.hasItemOutput() ? p.output().getHoverName() : p.fluidOutputs().get(0).getDisplayName();
		return Component.literal(p.processing() ? "Processing Pattern: " : "Crafting Pattern: ").append(what);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return CraftingPattern.fromStack(stack) != null;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		CraftingPattern p = CraftingPattern.fromStack(stack);
		if (p == null) {
			tooltip.add(Component.literal("Empty - encode it in a Pattern Encoder").withStyle(ChatFormatting.GRAY));
			return;
		}
		if (p.processing()) {
			tooltip.add(Component.literal("Machine recipe - done by the machine next to the Molecular Assembler holding it").withStyle(ChatFormatting.GOLD));
			tooltip.add(Component.literal("In:").withStyle(ChatFormatting.GRAY));
			for (ItemStack in : p.inputs()) if (!in.isEmpty()) tooltip.add(Component.literal("  " + in.getCount() + " x " + in.getHoverName().getString()).withStyle(ChatFormatting.GRAY));
			for (var f : p.fluidInputs()) tooltip.add(Component.literal("  " + String.format("%,d", f.getAmount()) + " mB " + f.getDisplayName().getString()).withStyle(ChatFormatting.BLUE));
			if (p.hasMachine()) {
				var b = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.tryParse(p.machine()));
				tooltip.add(Component.literal("Machine: " + (b == null ? p.machine() : b.getName().getString())).withStyle(ChatFormatting.GOLD));
			}
			tooltip.add(Component.literal("Out:").withStyle(ChatFormatting.GRAY));
			for (ItemStack out : p.outputs()) tooltip.add(Component.literal("  " + out.getCount() + " x " + out.getHoverName().getString()).withStyle(ChatFormatting.AQUA));
			for (var f : p.fluidOutputs()) tooltip.add(Component.literal("  " + String.format("%,d", f.getAmount()) + " mB " + f.getDisplayName().getString()).withStyle(ChatFormatting.BLUE));
		} else {
			tooltip.add(Component.literal("Makes " + p.output().getCount() + " " + p.output().getHoverName().getString()).withStyle(ChatFormatting.AQUA));
			for (int i = 0; i < 9; i++) {
				ItemStack in = p.inputs().get(i);
				if (in.isEmpty()) continue;
				int alts = p.options(i).size() - 1;
				tooltip.add(Component.literal("  " + in.getHoverName().getString() + (alts > 0 ? " (or " + alts + " others)" : "")).withStyle(ChatFormatting.GRAY));
			}
		}
		tooltip.add(Component.literal("Put it in a Molecular Assembler. Sneak + right-click to wipe.").withStyle(ChatFormatting.DARK_GRAY));
	}
}
