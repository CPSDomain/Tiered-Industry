package com.robvanblerk.tieredpower.item.powered;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.IEnergyStorage;

/** A battery you carry. Right-click to switch it on: it then charges every other powered item in your inventory and armour. */
public class PortableBatteryItem extends Item {
	private final int capacity;
	private final int rate;

	public int getCapacity() {
		return capacity;
	}

	public PortableBatteryItem(int capacity, int rate, Properties properties) {
		super(properties.stacksTo(1));
		this.capacity = capacity;
		this.rate = rate;
	}

	public static boolean isActive(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean("Active");
	}

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return ItemEnergy.provider(stack, capacity, rate, true);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			boolean on = !isActive(stack);
			stack.getOrCreateTag().putBoolean("Active", on);
			player.displayClientMessage(Component.literal("Portable Battery: " + (on ? "charging your items" : "off")), true);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide() || !isActive(stack) || !(entity instanceof Player player) || level.getGameTime() % 5 != 0) return;
		int budget = Math.min(ItemEnergy.get(stack), rate * 5);
		for (ItemStack other : player.getInventory().items) budget = chargeOne(stack, other, budget);
		for (ItemStack other : player.getInventory().armor) budget = chargeOne(stack, other, budget);
		for (ItemStack other : player.getInventory().offhand) budget = chargeOne(stack, other, budget);
	}

	private int chargeOne(ItemStack battery, ItemStack other, int budget) {
		if (budget <= 0 || other.isEmpty() || other == battery || other.getItem() instanceof PortableBatteryItem) return budget;
		IEnergyStorage target = other.getCapability(ForgeCapabilities.ENERGY).orElse(null);
		if (target == null || !target.canReceive()) return budget;
		int given = target.receiveEnergy(budget, false);
		ItemEnergy.set(battery, ItemEnergy.get(battery) - given);
		return budget - given;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isActive(stack);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return ItemEnergy.barWidth(stack, capacity);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return ItemEnergy.BAR_COLOUR;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		ItemEnergy.tooltip(stack, capacity, tooltip);
		tooltip.add(Component.literal((isActive(stack) ? "ON" : "OFF") + " - right-click to switch").withStyle(isActive(stack) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		tooltip.add(Component.literal(String.format("Charges your other items at %,d FE/t", rate)).withStyle(ChatFormatting.GRAY));
	}
}
