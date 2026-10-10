package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.BankControllerMenu;
import com.robvanblerk.tieredpower.multiblock.BankStructure;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Energy Bank controller. Holds the bank's energy as a long (so it can store far more than 2.1 billion FE).
 * Bank Ports move energy in and out through it. If the structure breaks, the stored energy is kept
 * (but nothing moves) until it's rebuilt; if it's rebuilt smaller, anything above the new capacity is lost.
 */
public class BankControllerBlockEntity extends BlockEntity implements MenuProvider {
	public static final int PORT_RATE = 1_000_000;
	public static final int CHECK_INTERVAL = 20;

	private boolean formed;
	private long capacity;
	private long stored;
	private int cells;
	private int checkTimer = CHECK_INTERVAL;

	private long inThisTick, outThisTick;
	private long rateIn, rateOut;
	private int lastSignal = -1;

	public int getComparatorSignal() {
		if (stored <= 0 || capacity <= 0) return 0;
		return 1 + (int) (stored * 14 / capacity);
	}

	public long getRateIn() {
		return rateIn;
	}

	public long getRateOut() {
		return rateOut;
	}

	public int getCells() {
		return cells;
	}

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			if (index < 4) return (int) ((stored >>> (16 * index)) & 0xFFFF);
			if (index < 8) return (int) ((capacity >>> (16 * (index - 4))) & 0xFFFF);
			if (index < 10) return (int) ((rateIn >>> (16 * (index - 8))) & 0xFFFF);
			if (index < 12) return (int) ((rateOut >>> (16 * (index - 10))) & 0xFFFF);
			if (index == 12) return formed ? 1 : 0;
			if (index == 13) return cells;
			return 0;
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return BankControllerMenu.DATA_COUNT;
		}
	};

	public BankControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BANK_CONTROLLER.get(), pos, state);
	}

	public boolean isFormed() {
		return formed;
	}

	public long getStored() {
		return stored;
	}

	public long getCapacity() {
		return capacity;
	}

	/** Called by Input ports. Returns how much was accepted. */
	public int insert(int amount, boolean simulate) {
		if (!formed || amount <= 0) return 0;
		int accepted = (int) Math.min(Math.min(amount, PORT_RATE), capacity - stored);
		if (accepted <= 0) return 0;
		if (!simulate) {
			stored += accepted;
			inThisTick += accepted;
			setChanged();
		}
		return accepted;
	}

	/** Called by Output ports. Returns how much was taken. */
	public int extract(int amount, boolean simulate) {
		if (!formed || amount <= 0) return 0;
		int taken = (int) Math.min(Math.min(amount, PORT_RATE), stored);
		if (taken <= 0) return 0;
		if (!simulate) {
			stored -= taken;
			outThisTick += taken;
			setChanged();
		}
		return taken;
	}

	public Component revalidate() {
		checkTimer = 0;
		if (level == null) return Component.empty();
		BankStructure.Result result = BankStructure.check(level, worldPosition, getBlockState().getValue(MachineBlock.FACING));
		formed = result.formed();
		if (formed) {
			capacity = result.capacity();
			cells = result.cells();
			stored = Math.min(stored, capacity);
			for (BlockPos portPos : result.ports()) {
				if (level.getBlockEntity(portPos) instanceof BankPortBlockEntity port) port.setController(worldPosition);
			}
		}
		setChanged();
		return result.message();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BankControllerBlockEntity be) {
		if (++be.checkTimer >= CHECK_INTERVAL) be.revalidate();
		be.rateIn = be.inThisTick;
		be.rateOut = be.outThisTick;
		be.inThisTick = 0;
		be.outThisTick = 0;
		int signal = be.getComparatorSignal();
		if (signal != be.lastSignal) {
			be.lastSignal = signal;
			level.updateNeighbourForOutputSignal(pos, state.getBlock());
		}
		if (state.getValue(MachineBlock.LIT) != be.formed) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, be.formed), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putLong("stored", stored);
		tag.putLong("capacity", capacity);
		tag.putInt("cells", cells);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		stored = tag.getLong("stored");
		capacity = tag.getLong("capacity");
		cells = tag.getInt("cells");
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.bank_controller");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new BankControllerMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
	}
}
