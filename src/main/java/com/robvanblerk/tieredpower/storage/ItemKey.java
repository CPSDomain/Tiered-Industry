package com.robvanblerk.tieredpower.storage;

import java.util.Objects;

import net.minecraft.world.item.ItemStack;

/** An item type (item + NBT), usable as a map key. Always holds a copy with count 1. */
public final class ItemKey {
	private final ItemStack proto;
	private final int hash;

	public ItemKey(ItemStack stack) {
		this.proto = stack.copyWithCount(1);
		this.hash = Objects.hash(proto.getItem(), proto.getTag());
	}

	public ItemStack proto() {
		return proto;
	}

	public ItemStack toStack(int count) {
		return proto.copyWithCount(count);
	}

	/** True if this is the plain item, with no NBT (no stored energy, enchantments, contents...). */
	public boolean isPlain() {
		return proto.getTag() == null || proto.getTag().isEmpty();
	}

	/**
	 * A stored copy that may stand in for the plain item in a crafting recipe: an energy-storing item, or a block that
	 * kept its charge when broken - never enchanted or renamed gear.
	 */
	public boolean isChargedVariant() {
		if (isPlain() || proto.isEnchanted() || proto.hasCustomHoverName()) return false;
		if (proto.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent()) return true;
		// A block that kept its charge when broken (Battery Boxes, Energy Cells): its only data is BlockEntityTag.energy.
		var tag = proto.getTag();
		if (tag.getAllKeys().size() != 1 || !tag.contains("BlockEntityTag", net.minecraft.nbt.Tag.TAG_COMPOUND)) return false;
		var be = tag.getCompound("BlockEntityTag");
		return be.getAllKeys().size() == 1 && be.contains("energy");
	}

	public boolean sameItem(ItemKey other) {
		return proto.getItem() == other.proto.getItem();
	}

	public boolean matches(ItemStack stack) {
		return ItemStack.isSameItemSameTags(proto, stack);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ItemKey k && ItemStack.isSameItemSameTags(proto, k.proto);
	}

	@Override
	public int hashCode() {
		return hash;
	}
}
