package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.item.CraftingPatternItem;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.CraftingPattern;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/** 27 patterns, inside an Assembly Matrix. Filled through the Matrix Controller's screen. */
public class PatternBankBlockEntity extends BlockEntity {
	public static final int SLOTS = 27;
	private final SimpleContainer patterns = new SimpleContainer(SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			PatternBankBlockEntity.this.setChanged();
			StorageNetwork.changed(); // terminals update their "can craft" lists
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return stack.getItem() instanceof CraftingPatternItem && CraftingPattern.fromStack(stack) != null;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	};

	public PatternBankBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PATTERN_BANK.get(), pos, state);
	}

	public SimpleContainer container() {
		return patterns;
	}

	public List<CraftingPattern> patterns() {
		List<CraftingPattern> out = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			CraftingPattern p = CraftingPattern.fromStack(patterns.getItem(i));
			if (p != null) out.add(p);
		}
		return out;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ContainerHelper.saveAllItems(tag, asList());
	}

	private net.minecraft.core.NonNullList<ItemStack> asList() {
		var list = net.minecraft.core.NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		for (int i = 0; i < SLOTS; i++) list.set(i, patterns.getItem(i));
		return list;
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		var list = net.minecraft.core.NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, list);
		for (int i = 0; i < SLOTS; i++) patterns.setItem(i, list.get(i));
	}
}
