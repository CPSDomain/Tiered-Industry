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

import com.robvanblerk.tieredpower.item.powered.SuitModules;

/** A Quantum Suit module: wear the right piece and right-click the module to fit it. Manage modules with the Suit Modules key. */
public class SuitModuleItem extends Item {
	private final SuitModules.Module module;

	public SuitModuleItem(SuitModules.Module module, Properties properties) {
		super(properties);
		this.module = module;
	}

	public SuitModules.Module module() {
		return module;
	}

	private static String pieceName(SuitModules.Module m) {
		return switch (m.slot) {
			case HEAD -> "Quantum Helmet";
			case CHEST -> "Quantum Chestplate";
			case LEGS -> "Quantum Leggings";
			default -> "Quantum Boots";
		};
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) return InteractionResultHolder.success(stack);
		ItemStack armor = player.getItemBySlot(module.slot);
		if (!SuitModules.isQuantum(armor)) {
			player.displayClientMessage(Component.literal("Wear the " + pieceName(module) + " to fit this").withStyle(ChatFormatting.RED), true);
			return InteractionResultHolder.fail(stack);
		}
		if (SuitModules.installed(armor, module)) {
			player.displayClientMessage(Component.literal(module.title + " is already fitted").withStyle(ChatFormatting.YELLOW), true);
			return InteractionResultHolder.fail(stack);
		}
		SuitModules.install(armor, module);
		if (!player.getAbilities().instabuild) stack.shrink(1);
		player.displayClientMessage(Component.literal(module.title + " fitted to your " + pieceName(module)).withStyle(ChatFormatting.GREEN), true);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(module.description).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.literal("Fits the " + pieceName(module) + ": wear it and right-click this").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Switch on/off or remove with the Suit Modules key").withStyle(ChatFormatting.DARK_GRAY));
	}
}
