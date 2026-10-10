package com.robvanblerk.tieredpower.menu;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;

import com.robvanblerk.tieredpower.block.entity.DiskWorkbenchBlockEntity;
import com.robvanblerk.tieredpower.block.entity.DriveBayBlockEntity;
import com.robvanblerk.tieredpower.item.FluidDropItem;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Slots: 0 the disk, 1-18 its partition (copies), 19+ player. */
public class DiskWorkbenchMenu extends AbstractContainerMenu {
	public static final int DISK = 0, PART_START = 1, PART_END = 19, INV_Y = 102;
	private final @Nullable DiskWorkbenchBlockEntity bench;
	private final Container disk, partition;
	private final ContainerData data;

	public DiskWorkbenchMenu(int id, Inventory inv) {
		this(id, inv, null, new SimpleContainer(1), new SimpleContainer(18), new SimpleContainerData(2));
	}

	public DiskWorkbenchMenu(int id, Inventory inv, DiskWorkbenchBlockEntity be) {
		this(id, inv, be, be.disk(), be.partition(), be.data());
	}

	private DiskWorkbenchMenu(int id, Inventory inv, @Nullable DiskWorkbenchBlockEntity be, Container disk, Container partition, ContainerData data) {
		super(ModMenus.DISK_WORKBENCH.get(), id);
		this.bench = be;
		this.disk = disk;
		this.partition = partition;
		this.data = data;
		addSlot(new Slot(disk, 0, 17, 35) {
			@Override public boolean mayPlace(ItemStack s) { return DriveBayBlockEntity.isDisk(s); }
			@Override public int getMaxStackSize() { return 1; }
		});
		for (int i = 0; i < 18; i++) {
			addSlot(new Slot(partition, i, 44 + (i % 6) * 18, 17 + (i / 6) * 18) {
				@Override public boolean mayPlace(ItemStack s) { return false; }
				@Override public boolean mayPickup(Player p) { return false; }
			});
		}
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
		addDataSlots(data);
	}

	public int getPriority() { return data.get(0) - 100; }
	/** 0 = no disk, 1 = item disk, 2 = fluid disk. */
	public int getDiskKind() { return data.get(1); }

	/** Partition slots: click with an item (or a bucket/tank on a fluid disk) to add it, empty hand to remove. */
	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		if (slotId >= PART_START && slotId < PART_END) {
			int i = slotId - PART_START;
			ItemStack carried = getCarried();
			if (getDiskKind() == 0) return;
			if (carried.isEmpty()) partition.setItem(i, ItemStack.EMPTY);
			else if (getDiskKind() == 2) {
				var fluid = FluidUtil.getFluidContained(carried);
				if (fluid.isEmpty()) return;
				partition.setItem(i, FluidDropItem.of(new net.minecraftforge.fluids.FluidStack(fluid.get(), 1000)));
			} else {
				partition.setItem(i, carried.copyWithCount(1));
			}
			partition.setChanged();
			return;
		}
		super.clicked(slotId, button, type, player);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (bench == null) return false;
		bench.button(id);
		return true;
	}

	/** Shift-click: a disk goes in or out; other items are added to the partition. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index >= PART_START && index < PART_END) return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index == DISK) {
			if (!moveItemStackTo(stack, PART_END, slots.size(), true)) return ItemStack.EMPTY;
		} else if (DriveBayBlockEntity.isDisk(stack)) {
			if (!moveItemStackTo(stack, DISK, DISK + 1, false)) return ItemStack.EMPTY;
		} else {
			if (getDiskKind() != 1) return ItemStack.EMPTY;
			for (int i = 0; i < 18; i++) if (ItemStack.isSameItemSameTags(partition.getItem(i), stack)) return ItemStack.EMPTY;
			for (int i = 0; i < 18; i++) {
				if (partition.getItem(i).isEmpty()) {
					partition.setItem(i, stack.copyWithCount(1));
					partition.setChanged();
					break;
				}
			}
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
		return copy;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.index < PART_START || slot.index >= PART_END;
	}

	@Override
	public boolean stillValid(Player player) {
		return disk.stillValid(player);
	}
}
