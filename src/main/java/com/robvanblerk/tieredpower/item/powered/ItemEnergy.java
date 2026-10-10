package com.robvanblerk.tieredpower.item.powered;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Forge Energy stored on an item (in its "Energy" tag). Any FE charger from any mod can fill these items,
 * including our Charger, Wireless Charger and Portable Battery.
 */
public final class ItemEnergy {
	public static final int BAR_COLOUR = 0xD63A2E;

	public static int get(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getInt("Energy");
	}

	public static void set(ItemStack stack, int energy) {
		stack.getOrCreateTag().putInt("Energy", Math.max(0, energy));
	}

	/** Uses energy if there's enough; returns false (and uses nothing) if not. */
	public static boolean use(ItemStack stack, int amount) {
		int e = get(stack);
		if (e < amount) return false;
		set(stack, e - amount);
		return true;
	}

	public static int barWidth(ItemStack stack, int capacity) {
		return Math.round(13f * Math.min(1f, (float) get(stack) / Math.max(1, capacity)));
	}

	public static void tooltip(ItemStack stack, int capacity, List<Component> tooltip) {
		tooltip.add(Component.literal(String.format("%,d / %,d FE", get(stack), capacity)).withStyle(ChatFormatting.RED));
	}

	/** Capability provider: canExtract = whether other things may pull energy back out (only batteries allow it). */
	/** The energy store of one of our powered items, as an IEnergyStorage working on its "Energy" tag. */
	public static IEnergyStorage storage(ItemStack stack, int capacity, int maxTransfer, boolean canExtract) {
		return new IEnergyStorage() {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int e = get(stack);
			int accepted = Math.min(Math.min(maxReceive, maxTransfer), capacity - e);
			if (accepted <= 0) return 0;
			if (!simulate) set(stack, e + accepted);
			return accepted;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			if (!canExtract) return 0;
			int e = get(stack);
			int taken = Math.min(Math.min(maxExtract, maxTransfer), e);
			if (taken <= 0) return 0;
			if (!simulate) set(stack, e - taken);
			return taken;
		}

		@Override
		public int getEnergyStored() {
			return get(stack);
		}

		@Override
		public int getMaxEnergyStored() {
			return capacity;
		}

		@Override
		public boolean canExtract() {
			return canExtract;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
		};
	}

	/** Capacity of this mod's FE items (jetpacks, Quantum Suit, drills and chainsaws, Portable Battery); 0 for anything else. */
	public static int capacityOf(ItemStack stack) {
		var item = stack.getItem();
		if (item instanceof HydrogenJetpackItem) return 0; // runs on hydrogen, not FE
		if (item instanceof JetpackItem j) return j.getCapacity();
		if (item instanceof QuantumArmorItem) return QuantumArmorItem.CAPACITY;
		if (item instanceof ElectricDrillItem d) return d.getCapacity();
		if (item instanceof PortableBatteryItem b) return b.getCapacity();
		if (item instanceof OreScannerItem) return OreScannerItem.CAPACITY;
		if (item instanceof com.robvanblerk.tieredpower.item.MagnetItem m && m.isAdvanced()) return com.robvanblerk.tieredpower.item.MagnetItem.ADVANCED_CAPACITY;
		return 0;
	}

	public static ICapabilityProvider provider(ItemStack stack, int capacity, int maxTransfer, boolean canExtract) {
		return new ICapabilityProvider() {
			private final LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> storage(stack, capacity, maxTransfer, canExtract));

			@Override
			public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
				return capability == ForgeCapabilities.ENERGY ? cap.cast() : LazyOptional.empty();
			}
		};
	}

	private ItemEnergy() {}
}
