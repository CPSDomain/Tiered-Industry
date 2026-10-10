package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import com.robvanblerk.tieredpower.item.FluidDiskItem;
import com.robvanblerk.tieredpower.item.FluidDropItem;
import com.robvanblerk.tieredpower.item.StorageDiskItem;
import com.robvanblerk.tieredpower.menu.DiskWorkbenchMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.DiskConfig;
import com.robvanblerk.tieredpower.storage.DiskInventory;
import com.robvanblerk.tieredpower.storage.FluidDiskInventory;

/**
 * Sets up a Storage or Fluid Disk: its partition (only these items or fluids - empty = anything) and priority (higher
 * disks fill first). Everything is written straight onto the disk, so it travels with it.
 */
public class DiskWorkbenchBlockEntity extends BlockEntity implements MenuProvider {
	public static final int BUTTON_PRIORITY_DOWN = 0, BUTTON_PRIORITY_UP = 1, BUTTON_FILL = 2, BUTTON_CLEAR = 3;
	private boolean loading;

	private final SimpleContainer disk = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			loadPartition();
			DiskWorkbenchBlockEntity.this.setChanged();
		}
	};
	private final SimpleContainer partition = new SimpleContainer(DiskConfig.MAX_ITEMS) {
		@Override
		public void setChanged() {
			super.setChanged();
			if (!loading) savePartition();
		}
	};

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			ItemStack d = disk.getItem(0);
			return switch (i) {
				case 0 -> DiskConfig.priority(d) + 100;
				case 1 -> d.getItem() instanceof FluidDiskItem ? 2 : d.getItem() instanceof StorageDiskItem ? 1 : 0;
				default -> 0;
			};
		}

		@Override public void set(int i, int v) {}
		@Override public int getCount() { return 2; }
	};

	public DiskWorkbenchBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DISK_WORKBENCH.get(), pos, state);
	}

	public SimpleContainer disk() { return disk; }
	public SimpleContainer partition() { return partition; }
	public ContainerData data() { return data; }

	public boolean hasFluidDisk() { return disk.getItem(0).getItem() instanceof FluidDiskItem; }
	public boolean hasDisk() { return disk.getItem(0).getItem() instanceof StorageDiskItem || hasFluidDisk(); }

	/** Disk in: show its partition in the slots. */
	private void loadPartition() {
		loading = true;
		for (int i = 0; i < DiskConfig.MAX_ITEMS; i++) partition.setItem(i, ItemStack.EMPTY);
		ItemStack d = disk.getItem(0);
		if (d.getItem() instanceof StorageDiskItem) {
			List<ItemStack> items = DiskConfig.itemPartition(d);
			for (int i = 0; i < items.size() && i < DiskConfig.MAX_ITEMS; i++) partition.setItem(i, items.get(i));
		} else if (d.getItem() instanceof FluidDiskItem) {
			List<FluidStack> fluids = DiskConfig.fluidPartition(d);
			for (int i = 0; i < fluids.size() && i < DiskConfig.MAX_ITEMS; i++) partition.setItem(i, FluidDropItem.of(new FluidStack(fluids.get(i), 1000)));
		}
		loading = false;
	}

	/** Slots changed: write the partition onto the disk. */
	private void savePartition() {
		ItemStack d = disk.getItem(0);
		if (d.getItem() instanceof StorageDiskItem) {
			List<ItemStack> items = new ArrayList<>();
			for (int i = 0; i < DiskConfig.MAX_ITEMS; i++) if (!partition.getItem(i).isEmpty()) items.add(partition.getItem(i));
			DiskConfig.setItemPartition(d, items);
		} else if (d.getItem() instanceof FluidDiskItem) {
			List<FluidStack> fluids = new ArrayList<>();
			for (int i = 0; i < DiskConfig.MAX_ITEMS; i++) {
				FluidStack f = FluidDropItem.fluid(partition.getItem(i));
				if (!f.isEmpty()) fluids.add(f);
			}
			DiskConfig.setFluidPartition(d, fluids);
		}
		setChanged();
	}

	public void button(int id) {
		ItemStack d = disk.getItem(0);
		if (!hasDisk()) return;
		switch (id) {
			case BUTTON_PRIORITY_DOWN -> DiskConfig.setPriority(d, DiskConfig.priority(d) - 1);
			case BUTTON_PRIORITY_UP -> DiskConfig.setPriority(d, DiskConfig.priority(d) + 1);
			case BUTTON_CLEAR -> {
				DiskConfig.setItemPartition(d, List.of());
				DiskConfig.setFluidPartition(d, List.of());
				loadPartition();
			}
			case BUTTON_FILL -> { // partition the disk to exactly what's stored on it
				if (hasFluidDisk()) {
					List<FluidStack> fluids = new ArrayList<>();
					for (var k : new FluidDiskInventory(d, () -> {}).fluids().keySet()) fluids.add(k.proto());
					DiskConfig.setFluidPartition(d, fluids);
				} else {
					List<ItemStack> items = new ArrayList<>();
					for (var k : new DiskInventory(d, () -> {}).items().keySet()) items.add(k.proto());
					DiskConfig.setItemPartition(d, items);
				}
				loadPartition();
			}
			default -> {}
		}
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("Disk", disk.getItem(0).save(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		disk.setItem(0, ItemStack.of(tag.getCompound("Disk")));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.disk_workbench");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new DiskWorkbenchMenu(id, inv, this);
	}
}
