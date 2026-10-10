package com.robvanblerk.tieredpower.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Turns a list of item stacks into a full vanilla Container, so block entities only supply getItems(). */
public interface ImplementedContainer extends Container {
	NonNullList<ItemStack> getItems();

	@Override
	default int getContainerSize() {
		return getItems().size();
	}

	@Override
	default boolean isEmpty() {
		for (ItemStack stack : getItems()) if (!stack.isEmpty()) return false;
		return true;
	}

	@Override
	default ItemStack getItem(int slot) {
		return getItems().get(slot);
	}

	@Override
	default ItemStack removeItem(int slot, int count) {
		ItemStack result = ContainerHelper.removeItem(getItems(), slot, count);
		if (!result.isEmpty()) setChanged();
		return result;
	}

	@Override
	default ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(getItems(), slot);
	}

	@Override
	default void setItem(int slot, ItemStack stack) {
		getItems().set(slot, stack);
		if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
		setChanged();
	}

	@Override
	default void clearContent() {
		getItems().clear();
	}

	@Override
	default boolean stillValid(Player player) {
		return true;
	}
}
