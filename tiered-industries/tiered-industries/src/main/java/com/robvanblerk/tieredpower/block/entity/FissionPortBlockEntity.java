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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import com.robvanblerk.tieredpower.block.FissionPortBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.multiblock.FissionStructure;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Fuel, Waste, Coolant or Power Port of the multiblock Fission Reactor. Forwards to the controller while it's formed. */
public class FissionPortBlockEntity extends BlockEntity {
	@Nullable
	private BlockPos controllerPos;

	public FissionPortBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FISSION_PORT.get(), pos, state);
	}

	public void setController(BlockPos pos) {
		if (!pos.equals(controllerPos)) {
			controllerPos = pos;
			setChanged();
		}
	}

	public @Nullable FissionControllerBlockEntity getController() {
		if (controllerPos == null || level == null) return null;
		return level.getBlockEntity(controllerPos) instanceof FissionControllerBlockEntity c && c.isFormed() ? c : null;
	}

	private FissionPortBlock.Kind kind() {
		return getBlockState().getBlock() instanceof FissionPortBlock b ? b.getKind() : FissionPortBlock.Kind.FUEL;
	}

	/** Fuel Port: rods go straight into the reactor's fuel store. Nothing comes out. */
	private final IItemHandler fuelIn = new IItemHandler() {
		@Override public int getSlots() { return 1; }
		@Override public @NotNull ItemStack getStackInSlot(int s) { return ItemStack.EMPTY; }
		@Override public int getSlotLimit(int s) { return 64; }
		@Override public boolean isItemValid(int s, @NotNull ItemStack st) { return com.robvanblerk.tieredpower.energy.FuelRods.isFuel(st); }
		@Override public @NotNull ItemStack extractItem(int s, int a, boolean sim) { return ItemStack.EMPTY; }

		@Override
		public @NotNull ItemStack insertItem(int s, @NotNull ItemStack st, boolean sim) {
			FissionControllerBlockEntity c = getController();
			if (c == null || !com.robvanblerk.tieredpower.energy.FuelRods.isFuel(st)) return st;
			int fit = Math.min(st.getCount(), FissionControllerBlockEntity.MAX_RODS - c.getStoredRods());
			if (fit <= 0) return st;
			if (!sim) c.loadRods(fit, com.robvanblerk.tieredpower.energy.FuelRods.isMox(st));
			return st.copyWithCount(st.getCount() - fit);
		}
	};

	/** Waste Port: depleted rods can be taken out (and it pushes them out by itself). Nothing goes in. */
	private final IItemHandler wasteOut = new IItemHandler() {
		@Override public int getSlots() { return 1; }
		@Override public int getSlotLimit(int s) { return 64; }
		@Override public boolean isItemValid(int s, @NotNull ItemStack st) { return false; }
		@Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack st, boolean sim) { return st; }

		@Override
		public @NotNull ItemStack getStackInSlot(int s) {
			FissionControllerBlockEntity c = getController();
			return c == null ? ItemStack.EMPTY : c.takeDepleted(64, true);
		}

		@Override
		public @NotNull ItemStack extractItem(int s, int a, boolean sim) {
			FissionControllerBlockEntity c = getController();
			return c == null ? ItemStack.EMPTY : c.takeDepleted(a, sim);
		}
	};

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 2;
		}

		@Override
		public @NotNull FluidStack getFluidInTank(int tank) {
			FissionControllerBlockEntity c = getController();
			if (c == null) return FluidStack.EMPTY;
			return tank == 0 ? c.getWater().getFluid() : c.getSteam() > 0 ? new FluidStack(ModFluids.STEAM.get(), c.getSteam()) : FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank == 0 ? FissionControllerBlockEntity.WATER_CAPACITY : FissionControllerBlockEntity.STEAM_CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
			FissionControllerBlockEntity c = getController();
			return tank == 0 && c != null && c.getWater().isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			FissionControllerBlockEntity c = getController();
			return c == null ? 0 : c.insertWater(resource, action);
		}

		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			return resource.getFluid() == ModFluids.STEAM.get() ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			FissionControllerBlockEntity c = getController();
			return c == null ? FluidStack.EMPTY : c.takeSteam(maxDrain, action);
		}
	};

	private final IEnergyStorage energyOut = new IEnergyStorage() {
		@Override public int receiveEnergy(int max, boolean sim) { return 0; }
		@Override public int extractEnergy(int max, boolean sim) { FissionControllerBlockEntity c = getController(); return c == null ? 0 : c.energy.extractEnergy(max, sim); }
		@Override public int getEnergyStored() { FissionControllerBlockEntity c = getController(); return c == null ? 0 : c.energy.getEnergyStored(); }
		@Override public int getMaxEnergyStored() { return FissionControllerBlockEntity.CAPACITY; }
		@Override public boolean canExtract() { return getController() != null; }
		@Override public boolean canReceive() { return false; }
	};

	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> fuelIn);
	private LazyOptional<IItemHandler> wasteCap = LazyOptional.of(() -> wasteOut);
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyOut);

	/** Power Ports push power out; Coolant Ports push steam out - into anything outside the reactor. */
	public static void tick(Level level, BlockPos pos, BlockState state, FissionPortBlockEntity be) {
		FissionControllerBlockEntity c = be.getController();
		if (c == null) return;
		FissionPortBlock.Kind kind = be.kind();
		if (kind == FissionPortBlock.Kind.FUEL) return;
		if (kind == FissionPortBlock.Kind.WASTE) {
			if (level.getGameTime() % 10 != 0) return;
			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if (!level.isLoaded(next) || FissionStructure.isPart(level.getBlockState(next).getBlock())) continue;
				net.minecraftforge.items.IItemHandler target = com.robvanblerk.tieredpower.util.ItemUtil.neighbour(level, pos, dir);
				if (target == null) continue;
				ItemStack waiting = c.takeDepleted(64, true);
				if (waiting.isEmpty()) return;
				ItemStack rest = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(target, waiting, false);
				c.takeDepleted(waiting.getCount() - rest.getCount(), false);
			}
			return;
		}
		for (Direction dir : Direction.values()) {
			BlockPos next = pos.relative(dir);
			if (!level.isLoaded(next) || FissionStructure.isPart(level.getBlockState(next).getBlock())) continue;
			if (kind == FissionPortBlock.Kind.POWER) {
				if (c.energy.getEnergyStored() > 0) EnergyUtil.move(c.energy, EnergyUtil.neighbour(level, pos, dir), FissionControllerBlockEntity.PORT_RATE);
			} else if (c.getSteam() > 0) {
				BlockEntity n = level.getBlockEntity(next);
				if (n == null) continue;
				IFluidHandler target = n.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
				if (target == null) continue;
				int accepted = target.fill(new FluidStack(ModFluids.STEAM.get(), c.getSteam()), IFluidHandler.FluidAction.SIMULATE);
				if (accepted > 0) target.fill(c.takeSteam(accepted, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
			}
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		FissionPortBlock.Kind kind = kind();
		if (cap == ForgeCapabilities.ITEM_HANDLER && kind == FissionPortBlock.Kind.FUEL) return itemCap.cast();
		if (cap == ForgeCapabilities.ITEM_HANDLER && kind == FissionPortBlock.Kind.WASTE) return wasteCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER && kind == FissionPortBlock.Kind.COOLANT) return fluidCap.cast();
		if (cap == ForgeCapabilities.ENERGY && kind == FissionPortBlock.Kind.POWER) return energyCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		itemCap.invalidate();
		wasteCap.invalidate();
		fluidCap.invalidate();
		energyCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		itemCap = LazyOptional.of(() -> fuelIn);
		wasteCap = LazyOptional.of(() -> wasteOut);
		fluidCap = LazyOptional.of(() -> fluids);
		energyCap = LazyOptional.of(() -> energyOut);
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
