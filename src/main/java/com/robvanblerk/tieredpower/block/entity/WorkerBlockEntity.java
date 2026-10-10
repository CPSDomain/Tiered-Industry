package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.WorkerMenu;
import com.robvanblerk.tieredpower.util.ItemUtil;

/**
 * Shared base for the automation machines (Block Breaker, Block Placer, Tree Farm, Animal Ranch): 3 input slots, a 3x3
 * output buffer pushed into neighbouring inventories, Speed/Energy upgrades, redstone control. Every few ticks (faster
 * with Speed Upgrades) the machine does one job and pays for it in FE.
 * Slots: 0-2 inputs, 3-11 outputs, 12-13 upgrades.
 */
public abstract class WorkerBlockEntity extends MachineBlockEntity {
	public static final int INPUTS = 3, OUTPUT_START = 3, OUTPUT_END = 12, UPGRADE_SLOT = 12, SIZE = 14;
	public static final int CAPACITY = 100_000, MAX_INPUT = 4_000;
	public static final int STATUS_IDLE = 0, STATUS_WORKING = 1, STATUS_NO_POWER = 2, STATUS_FULL = 3, STATUS_SPECIAL = 4;

	protected int status, actions, progress;
	/** A machine-specific message shown when status is STATUS_SPECIAL (e.g. "Needs saplings"). */
	protected int special;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> actions & 0xFFFF;
				case 3 -> energyCost(costPerAction());
				case 4 -> status;
				case 5 -> special;
				default -> 0;
			};
		}

		@Override public void set(int i, int v) {}
		@Override public int getCount() { return WorkerMenu.DATA_COUNT; }
	};

	protected WorkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		this(type, pos, state, CAPACITY, MAX_INPUT);
	}

	protected WorkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxInput) {
		super(type, pos, state, SIZE, capacity, maxInput, 0);
		enableUpgrades(UPGRADE_SLOT);
	}

	/** FE for one job. */
	protected abstract int costPerAction();

	/** Ticks between jobs at normal speed. */
	protected abstract int interval();

	/** Do one job. Return true if something was done (costs FE), false if there was nothing to do (free). */
	protected abstract boolean work(ServerLevel level, BlockPos pos, Direction facing);

	/** What the input slots take. */
	public abstract boolean isValidInput(ItemStack stack);

	protected FakePlayer fakePlayer(ServerLevel level) {
		FakePlayer p = FakePlayerFactory.getMinecraft(level);
		p.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
		return p;
	}

	/** Puts an item into the output buffer; returns what didn't fit. */
	protected ItemStack addOutput(ItemStack stack) {
		ItemStack left = stack.copy();
		for (int i = OUTPUT_START; i < OUTPUT_END && !left.isEmpty(); i++) {
			ItemStack slot = items.get(i);
			if (slot.isEmpty()) {
				items.set(i, left.copy());
				left = ItemStack.EMPTY;
			} else if (ItemStack.isSameItemSameTags(slot, left) && slot.getCount() < slot.getMaxStackSize()) {
				int n = Math.min(left.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(n);
				left.shrink(n);
			}
		}
		setChanged();
		return left;
	}

	/** Room for at least one more stack of anything? */
	protected boolean outputHasRoom() {
		for (int i = OUTPUT_START; i < OUTPUT_END; i++) if (items.get(i).isEmpty()) return true;
		return false;
	}

	/** The first input slot holding something that matches, or -1. */
	protected int findInput(java.util.function.Predicate<ItemStack> test) {
		for (int i = 0; i < INPUTS; i++) if (!items.get(i).isEmpty() && test.test(items.get(i))) return i;
		return -1;
	}

	/** Drops that don't fit go on the ground in front of the machine rather than vanish. */
	protected void outputOrDrop(ServerLevel level, BlockPos pos, ItemStack stack) {
		ItemStack left = addOutput(stack);
		if (!left.isEmpty()) Block.popResource(level, pos, left);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, WorkerBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		for (int i = OUTPUT_START; i < OUTPUT_END; i++) { // results go into chests, pipes and buses next to it
			ItemStack stack = be.items.get(i);
			if (!stack.isEmpty()) be.items.set(i, ItemUtil.pushToNeighbours(level, pos, stack));
		}
		if (!(level instanceof ServerLevel server)) return;
		be.progress += be.progressStep();
		if (be.progress < be.interval() * 100) return;
		be.progress = 0;
		int cost = be.energyCost(be.costPerAction());
		if (be.energy.getMaxEnergyStored() < cost) be.energy.setCapacity(cost); // Speed upgrades can make one job cost more than the buffer
		boolean working;
		if (be.energy.getEnergyStored() < cost) {
			be.status = STATUS_NO_POWER;
			working = false;
		} else if (!be.outputHasRoom()) {
			be.status = STATUS_FULL;
			working = false;
		} else {
			be.status = STATUS_IDLE;
			Direction facing = state.hasProperty(MachineBlock.FACING) ? state.getValue(MachineBlock.FACING) : Direction.NORTH;
			working = be.work(server, pos, facing);
			if (working) {
				be.energy.removeInternal(cost);
				be.actions++;
				be.status = STATUS_WORKING;
			}
		}
		be.setChanged();
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("actions", actions); }
	@Override public void load(CompoundTag tag) { super.load(tag); actions = tag.getInt("actions"); }

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11} : new int[]{0, 1, 2};
	}

	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= OUTPUT_START && slot < OUTPUT_END; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot < INPUTS && isValidInput(stack); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new WorkerMenu(menuType(), id, inv, this, data);
	}

	protected abstract net.minecraft.world.inventory.MenuType<WorkerMenu> menuType();
}
