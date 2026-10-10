package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import com.robvanblerk.tieredpower.block.ReactorPortBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.multiblock.FusionStructure;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Fuel Port or Power Port. Remembers which controller it belongs to and forwards items/energy to it
 * while the structure is formed. Power Ports also push the reactor's output into neighbouring cables.
 */
public class ReactorPortBlockEntity extends BlockEntity {
	@Nullable
	private BlockPos controllerPos;

	private final IEnergyStorage energyProxy = new IEnergyStorage() {
		private @Nullable IEnergyStorage target() {
			FusionControllerBlockEntity c = getController();
			return c == null ? null : c.getReactorEnergy();
		}

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			IEnergyStorage t = target();
			return t == null ? 0 : t.receiveEnergy(maxReceive, simulate);
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			IEnergyStorage t = target();
			return t == null ? 0 : t.extractEnergy(maxExtract, simulate);
		}

		@Override
		public int getEnergyStored() {
			IEnergyStorage t = target();
			return t == null ? 0 : t.getEnergyStored();
		}

		@Override
		public int getMaxEnergyStored() {
			IEnergyStorage t = target();
			return t == null ? 0 : t.getMaxEnergyStored();
		}

		@Override
		public boolean canExtract() {
			IEnergyStorage t = target();
			return t != null && t.canExtract();
		}

		@Override
		public boolean canReceive() {
			IEnergyStorage t = target();
			return t != null && t.canReceive();
		}
	};

	private final IItemHandler itemProxy = new IItemHandler() {
		private @Nullable IItemHandler target() {
			FusionControllerBlockEntity c = getController();
			return c == null ? null : new SidedInvWrapper(c, Direction.UP);
		}

		@Override
		public int getSlots() {
			IItemHandler t = target();
			return t == null ? 0 : t.getSlots();
		}

		@Override
		public @NotNull ItemStack getStackInSlot(int slot) {
			IItemHandler t = target();
			return t == null ? ItemStack.EMPTY : t.getStackInSlot(slot);
		}

		@Override
		public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
			IItemHandler t = target();
			return t == null ? stack : t.insertItem(slot, stack, simulate);
		}

		@Override
		public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
			IItemHandler t = target();
			return t == null ? ItemStack.EMPTY : t.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			IItemHandler t = target();
			return t == null ? 0 : t.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			IItemHandler t = target();
			return t != null && t.isItemValid(slot, stack);
		}
	};

	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyProxy);
	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> itemProxy);

	public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.REACTOR_PORT.get(), pos, state);
	}

	public void setController(BlockPos pos) {
		if (!pos.equals(controllerPos)) {
			controllerPos = pos;
			setChanged();
		}
	}

	/** The controller this port belongs to, if it exists and the structure is currently formed. */
	public @Nullable FusionControllerBlockEntity getController() {
		if (controllerPos == null || level == null) return null;
		return level.getBlockEntity(controllerPos) instanceof FusionControllerBlockEntity c && c.isFormed() ? c : null;
	}

	private ReactorPortBlock.Kind kind() {
		return getBlockState().getBlock() instanceof ReactorPortBlock port ? port.getKind() : ReactorPortBlock.Kind.FUEL;
	}

	/** Power Ports push the reactor's output into anything outside the reactor that accepts FE. */
	public static void tick(Level level, BlockPos pos, BlockState state, ReactorPortBlockEntity be) {
		FusionControllerBlockEntity controller = be.getController();
		if (controller == null) return;
		for (Direction dir : Direction.values()) {
			if (controller.energy.getEnergyStored() <= 0) break;
			if (FusionStructure.isReactorPart(level.getBlockState(pos.relative(dir)).getBlock())) continue;
			EnergyUtil.move(controller.energy, EnergyUtil.neighbour(level, pos, dir), FusionControllerBlockEntity.MAX_PUSH);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY && kind() == ReactorPortBlock.Kind.POWER) return energyCap.cast();
		if (cap == ForgeCapabilities.ITEM_HANDLER && kind() == ReactorPortBlock.Kind.FUEL) return itemCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energyCap.invalidate();
		itemCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		energyCap = LazyOptional.of(() -> energyProxy);
		itemCap = LazyOptional.of(() -> itemProxy);
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
