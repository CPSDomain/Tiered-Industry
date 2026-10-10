package com.robvanblerk.tieredpower.item.powered;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import com.robvanblerk.tieredpower.network.ModNetwork;

/**
 * Jetpack, worn in the chest slot. Hold jump to fly up; it uses energy while thrusting and stops fall damage.
 * Movement is handled on the client (like all player movement); the server charges the energy.
 */
public class JetpackItem extends ArmorItem {
	protected final int capacity;
	protected final int costPerTick;
	private final double thrust;
	private final double maxRise;

	public JetpackItem(net.minecraft.world.item.ArmorMaterial material, int capacity, int costPerTick, double thrust, double maxRise, Properties properties) {
		super(material, ArmorItem.Type.CHESTPLATE, properties.stacksTo(1));
		this.capacity = capacity;
		this.costPerTick = costPerTick;
		this.thrust = thrust;
		this.maxRise = maxRise;
	}

	public int getCostPerTick() { return costPerTick; }
	public int getCapacity() { return capacity; }

	/** How much fuel is left (FE for the normal jetpacks). */
	public int fuel(ItemStack stack) {
		return ItemEnergy.get(stack);
	}

	/** Uses one tick of fuel; false if there isn't enough. */
	protected boolean consume(ItemStack stack) {
		return ItemEnergy.use(stack, costPerTick);
	}
	public double getThrust() { return thrust; }
	public double getMaxRise() { return maxRise; }

	public static boolean isWorn(Entity entity, ItemStack stack) {
		return entity instanceof Player p && p.getItemBySlot(EquipmentSlot.CHEST) == stack;
	}

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return ItemEnergy.provider(stack, capacity, capacity / 50, false);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (!(entity instanceof Player player) || !isWorn(entity, stack)) return;
		if (level.isClientSide()) {
			com.robvanblerk.tieredpower.client.JetpackClient.tick(player, stack, this);
		} else if (ModNetwork.isFlying(player.getUUID())) {
			if (consume(stack)) player.fallDistance = 0;
			else ModNetwork.stopFlying(player.getUUID());
		}
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
		tooltip.add(Component.literal("Wear it, then hold jump to fly. " + String.format("%,d", costPerTick) + " FE/t while thrusting.").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("No fall damage while it has energy.").withStyle(ChatFormatting.GRAY));
	}
}
