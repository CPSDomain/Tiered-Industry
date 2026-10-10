package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.AirSeparatorMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Splits air into nitrogen and oxygen - no inputs, just power: 16 mB nitrogen + 4 mB oxygen per tick (faster with Speed
 * Upgrades), using 120 FE/t. Pushes both gases into Gas Pipes. It needs at least one side open to the air.
 * Slots: 0-1 upgrades.
 */
public class AirSeparatorBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 100_000, MAX_INPUT = 4_000, ENERGY_PER_TICK = 120;
	public static final int TANK = 8_000, NITROGEN_PER_TICK = 16, OXYGEN_PER_TICK = 4;

	private int nitrogen, oxygen;
	private boolean working, airOk = true;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 2; }
		@Override public @NotNull FluidStack getFluidInTank(int t) {
			if (t == 0) return nitrogen > 0 ? new FluidStack(ModFluids.NITROGEN.get(), nitrogen) : FluidStack.EMPTY;
			return oxygen > 0 ? new FluidStack(ModFluids.OXYGEN.get(), oxygen) : FluidStack.EMPTY;
		}
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return false; }
		@Override public int fill(FluidStack r, FluidAction a) { return 0; }

		@Override
		public @NotNull FluidStack drain(FluidStack r, FluidAction a) {
			if (r.getFluid() == ModFluids.NITROGEN.get()) return take(true, r.getAmount(), a);
			if (r.getFluid() == ModFluids.OXYGEN.get()) return take(false, r.getAmount(), a);
			return FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int max, FluidAction a) {
			return nitrogen > 0 ? take(true, max, a) : take(false, max, a);
		}

		private FluidStack take(boolean n2, int max, FluidAction a) {
			int n = Math.min(max, n2 ? nitrogen : oxygen);
			if (n <= 0) return FluidStack.EMPTY;
			if (a.execute()) {
				if (n2) nitrogen -= n; else oxygen -= n;
				setChanged();
			}
			return new FluidStack(n2 ? ModFluids.NITROGEN.get() : ModFluids.OXYGEN.get(), n);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> nitrogen;
				case 3 -> oxygen;
				case 4 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return AirSeparatorMenu.DATA_COUNT; }
	};

	public AirSeparatorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.AIR_SEPARATOR.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(0);
	}

	/** Needs at least one side open to the air. */
	private static boolean hasAir(Level level, BlockPos pos) {
		for (Direction d : Direction.values()) if (level.getBlockState(pos.relative(d)).isAir()) return true;
		return false;
	}

	public boolean hasAirAccess() {
		return airOk;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, AirSeparatorBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		if (level.getGameTime() % 20 == 0) be.airOk = hasAir(level, pos); // checked once a second
		int mult = Math.max(1, be.progressStep() / 100);
		int cost = be.energyCost(ENERGY_PER_TICK);
		be.working = be.airOk && be.nitrogen + NITROGEN_PER_TICK * mult <= TANK && be.oxygen + OXYGEN_PER_TICK * mult <= TANK
				&& be.energy.getEnergyStored() >= cost;
		if (be.working) {
			be.energy.removeInternal(cost);
			be.nitrogen += NITROGEN_PER_TICK * mult;
			be.oxygen += OXYGEN_PER_TICK * mult;
		}
		if (be.nitrogen > 0) be.nitrogen = GasTanks.push(level, pos, ModFluids.NITROGEN.get(), be.nitrogen);
		if (be.oxygen > 0) be.oxygen = GasTanks.push(level, pos, ModFluids.OXYGEN.get(), be.oxygen);
		be.setChanged();
		if (state.getValue(MachineBlock.LIT) != be.working) level.setBlock(pos, state.setValue(MachineBlock.LIT, be.working), Block.UPDATE_ALL);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); fluidCap = LazyOptional.of(() -> fluids); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("nitrogen", nitrogen);
		tag.putInt("oxygen", oxygen);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		nitrogen = tag.getInt("nitrogen");
		oxygen = tag.getInt("oxygen");
	}

	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.air_separator"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new AirSeparatorMenu(id, inv, this, data);
	}
}
