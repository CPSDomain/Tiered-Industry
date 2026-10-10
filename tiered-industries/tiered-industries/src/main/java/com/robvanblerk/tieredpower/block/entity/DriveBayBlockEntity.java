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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.item.StorageDiskItem;
import com.robvanblerk.tieredpower.menu.DriveBayMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.DiskInventory;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/** Holds up to 8 Storage Disks for an item storage network. Needs no power itself - the Storage Controller pays for it. */
public class DriveBayBlockEntity extends MachineBlockEntity {
	public static final int SLOTS = 8;
	private final DiskInventory[] cache = new DiskInventory[SLOTS];
	private final com.robvanblerk.tieredpower.storage.FluidDiskInventory[] fluidCache = new com.robvanblerk.tieredpower.storage.FluidDiskInventory[SLOTS];
	private int ticks;

	public DriveBayBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DRIVE_BAY.get(), pos, state, SLOTS, 1, 0, 0);
	}

	@Override
	protected boolean exposesEnergy() {
		return false;
	}

	/** The disks in this bay, parsed (re-read only when a disk is swapped). */
	public List<DiskInventory> disks() {
		List<DiskInventory> out = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			ItemStack s = items.get(i);
			if (s.isEmpty() || !(s.getItem() instanceof StorageDiskItem)) {
				cache[i] = null;
				continue;
			}
			if (cache[i] == null || cache[i].disk() != s) cache[i] = new DiskInventory(s, this::setChanged);
			out.add(cache[i]);
		}
		return out;
	}

	/** The fluid disks in this bay. */
	public List<com.robvanblerk.tieredpower.storage.FluidDiskInventory> fluidDisks() {
		List<com.robvanblerk.tieredpower.storage.FluidDiskInventory> out = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			ItemStack s = items.get(i);
			if (s.isEmpty() || !(s.getItem() instanceof com.robvanblerk.tieredpower.item.FluidDiskItem)) {
				fluidCache[i] = null;
				continue;
			}
			if (fluidCache[i] == null || fluidCache[i].disk() != s) fluidCache[i] = new com.robvanblerk.tieredpower.storage.FluidDiskInventory(s, this::setChanged);
			out.add(fluidCache[i]);
		}
		return out;
	}

	public static boolean isDisk(ItemStack stack) {
		return stack.getItem() instanceof StorageDiskItem || stack.getItem() instanceof com.robvanblerk.tieredpower.item.FluidDiskItem;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DriveBayBlockEntity be) {
		if (++be.ticks < 40) return;
		be.ticks = 0;
		StorageControllerBlockEntity c = StorageNetwork.findController(level, pos);
		boolean lit = c != null && c.isOnline();
		if (state.getValue(MachineBlock.LIT) != lit) level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		super.setItem(slot, stack);
		StorageNetwork.changed();
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack out = super.removeItem(slot, count);
		StorageNetwork.changed();
		return out;
	}

	@Override public int getMaxStackSize() { return 1; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return isDisk(stack); }
	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.drive_bay"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new DriveBayMenu(id, inv, this, new SimpleContainerData(DriveBayMenu.DATA_COUNT));
	}
}
