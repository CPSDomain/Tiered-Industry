package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.item.CraftingPatternItem;
import com.robvanblerk.tieredpower.menu.MolecularAssemblerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.CraftingPattern;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/** Holds up to 9 Crafting Patterns for the storage network and does one craft every 8 ticks for its jobs. */
public class MolecularAssemblerBlockEntity extends MachineBlockEntity {
	/** 9 pattern slots, then Speed/Energy upgrades. (1.28.1-1.30 had 18 more slots after the upgrades; see load().) */
	public static final int PATTERNS = 9, UPGRADE_SLOT = 9, SIZE = 11;

	public static boolean isPatternSlot(int slot) {
		return slot < PATTERNS;
	}

	public static int patternSlot(int n) {
		return n;
	}

	/** Patterns found in the old extra slots (11-28) when loading - dropped beside the assembler on its first tick. */
	private final java.util.List<ItemStack> overflow = new java.util.ArrayList<>();

	@Override
	public void load(net.minecraft.nbt.CompoundTag tag) {
		super.load(tag);
		overflow.clear();
		var list = tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			var t = list.getCompound(i);
			int slot = t.getByte("Slot") & 255;
			if (slot >= SIZE) {
				ItemStack s = ItemStack.of(t);
				if (!s.isEmpty()) overflow.add(s);
			}
		}
	}

	public MolecularAssemblerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MOLECULAR_ASSEMBLER.get(), pos, state, SIZE, 1, 0, 0);
		enableUpgrades(UPGRADE_SLOT);
	}

	/** Crafts (or machine operations) per cycle: 1, plus 1 per Speed Upgrade. */
	public int operationsPerCycle() {
		return 1 + speedUpgrades();
	}

	@Override
	protected boolean exposesEnergy() {
		return false; // the Storage Controller pays for it
	}

	public List<CraftingPattern> patterns() {
		List<CraftingPattern> out = new ArrayList<>();
		for (int n = 0; n < PATTERNS; n++) {
			ItemStack s = items.get(patternSlot(n));
			CraftingPattern p = CraftingPattern.fromStack(s);
			if (p != null) out.add(p);
		}
		return out;
	}

	/** Does this assembler hold this exact pattern? (Processing steps run only where the pattern - and its machine - is.) */
	public boolean holds(CraftingPattern pattern) {
		var key = pattern.save();
		for (CraftingPattern p : patterns()) if (p.save().equals(key)) return true;
		return false;
	}

	public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MolecularAssemblerBlockEntity be) {
		if (be.overflow.isEmpty() || level.isClientSide()) return;
		for (ItemStack s : be.overflow) net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, s);
		be.overflow.clear();
		be.setChanged();
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		super.setItem(slot, stack);
		StorageNetwork.changed(); // terminals update their "can craft" list
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack out = super.removeItem(slot, count);
		StorageNetwork.changed();
		return out;
	}

	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return isPatternSlot(slot) && stack.getItem() instanceof CraftingPatternItem && CraftingPattern.fromStack(stack) != null; }
	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.molecular_assembler"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new MolecularAssemblerMenu(id, inv, this, new SimpleContainerData(MolecularAssemblerMenu.DATA_COUNT));
	}
}
