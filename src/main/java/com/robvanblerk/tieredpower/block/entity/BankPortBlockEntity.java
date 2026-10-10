package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.block.BankPortBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.SideMode;
import com.robvanblerk.tieredpower.multiblock.BankStructure;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Energy Bank port. Input ports accept power into the bank; Output ports push the bank's power into neighbours. */
public class BankPortBlockEntity extends BlockEntity {
	@Nullable
	private BlockPos controllerPos;

	private final IEnergyStorage proxy = new IEnergyStorage() {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			BankControllerBlockEntity c = getController();
			return c != null && isInput() ? c.insert(maxReceive, simulate) : 0;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			BankControllerBlockEntity c = getController();
			return c != null && !isInput() ? c.extract(maxExtract, simulate) : 0;
		}

		@Override
		public int getEnergyStored() {
			BankControllerBlockEntity c = getController();
			return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, c.getStored());
		}

		@Override
		public int getMaxEnergyStored() {
			BankControllerBlockEntity c = getController();
			return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, c.getCapacity());
		}

		@Override
		public boolean canExtract() {
			return getController() != null && !isInput();
		}

		@Override
		public boolean canReceive() {
			return getController() != null && isInput();
		}
	};
	private LazyOptional<IEnergyStorage> cap = LazyOptional.of(() -> proxy);

	public BankPortBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BANK_PORT.get(), pos, state);
	}

	private boolean isInput() {
		return getBlockState().getValue(BankPortBlock.MODE) == SideMode.INPUT;
	}

	public void setController(BlockPos pos) {
		if (!pos.equals(controllerPos)) {
			controllerPos = pos;
			setChanged();
		}
	}

	public @Nullable BankControllerBlockEntity getController() {
		if (controllerPos == null || level == null) return null;
		return level.getBlockEntity(controllerPos) instanceof BankControllerBlockEntity c && c.isFormed() ? c : null;
	}

	/** Output ports push power into anything outside the bank that accepts it. */
	public static void tick(Level level, BlockPos pos, BlockState state, BankPortBlockEntity be) {
		if (state.getValue(BankPortBlock.MODE) != SideMode.OUTPUT) return;
		if (be.getController() == null) return;
		for (Direction dir : Direction.values()) {
			if (BankStructure.isBankPart(level.getBlockState(pos.relative(dir)).getBlock())) continue;
			EnergyUtil.move(be.proxy, EnergyUtil.neighbour(level, pos, dir), BankControllerBlockEntity.PORT_RATE);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
		if (capability == ForgeCapabilities.ENERGY) return cap.cast();
		return super.getCapability(capability, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		cap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		cap = LazyOptional.of(() -> proxy);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (controllerPos != null) tag.put("controller", NbtUtils.writeBlockPos(controllerPos));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		controllerPos = tag.contains("controller") ? NbtUtils.readBlockPos(tag.getCompound("controller")) : null;
	}
}
