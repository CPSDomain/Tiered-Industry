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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.RocketFuelMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Liquefies gas for rocket fuel: methane becomes Liquid Methane and oxygen becomes Liquid Oxygen, cooled with nitrogen.
 * Each operation: 40 mB gas + 4 mB nitrogen + 200 FE = 20 mB liquid (one a tick, more with Speed Upgrades). Feed it one
 * gas at a time (a Fluid Filter helps); liquid comes out into Fluid Pipes. Slots: 0-1 upgrades.
 */
public class CryogenicCondenserBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 400_000, MAX_INPUT = 16_000, ENERGY_PER_OP = 200, TANK = 8_000;
	public static final int GAS_PER_OP = 40, NITROGEN_PER_OP = 4, LIQUID_PER_OP = 20;
	private FluidStack gas = FluidStack.EMPTY, liquid = FluidStack.EMPTY;
	private int nitrogen;

	private static @Nullable Fluid liquidFor(Fluid gas) {
		if (gas == ModFluids.METHANE.get()) return ModFluids.LIQUID_METHANE.get();
		if (gas == ModFluids.OXYGEN.get()) return ModFluids.LIQUID_OXYGEN.get();
		return null;
	}

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 3; }

		@Override
		public @NotNull FluidStack getFluidInTank(int t) {
			return switch (t) {
				case 0 -> gas;
				case 1 -> nitrogen > 0 ? new FluidStack(ModFluids.NITROGEN.get(), nitrogen) : FluidStack.EMPTY;
				default -> liquid;
			};
		}

		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return t == 0 ? liquidFor(s.getFluid()) != null : t == 1 && s.getFluid() == ModFluids.NITROGEN.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (r.getFluid() == ModFluids.NITROGEN.get()) {
				int n = Math.min(r.getAmount(), TANK - nitrogen);
				if (a.execute() && n > 0) { nitrogen += n; setChanged(); }
				return Math.max(0, n);
			}
			if (liquidFor(r.getFluid()) == null || (!gas.isEmpty() && !gas.isFluidEqual(r))) return 0;
			int n = Math.min(r.getAmount(), TANK - gas.getAmount());
			if (a.execute() && n > 0) {
				if (gas.isEmpty()) gas = new FluidStack(r, n); else gas.grow(n);
				setChanged();
			}
			return Math.max(0, n);
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return liquid.isFluidEqual(r) ? drain(r.getAmount(), a) : FluidStack.EMPTY; }

		@Override
		public @NotNull FluidStack drain(int max, FluidAction a) {
			int n = Math.min(max, liquid.getAmount());
			if (n <= 0) return FluidStack.EMPTY;
			FluidStack out = new FluidStack(liquid, n);
			if (a.execute()) { liquid.shrink(n); if (liquid.isEmpty()) liquid = FluidStack.EMPTY; setChanged(); }
			return out;
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> gas.getAmount();
				case 3 -> nitrogen;
				case 4 -> liquid.getAmount();
				case 5 -> gas.isEmpty() ? 0 : gas.getFluid() == ModFluids.METHANE.get() ? 1 : 2;
				case 6 -> liquid.isEmpty() ? 0 : liquid.getFluid() == ModFluids.LIQUID_METHANE.get() ? 1 : 2;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return RocketFuelMenu.DATA_COUNT; }
	};

	public CryogenicCondenserBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CRYOGENIC_CONDENSER.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(0);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CryogenicCondenserBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		Fluid out = be.gas.isEmpty() ? null : liquidFor(be.gas.getFluid());
		int ops = 0;
		if (out != null && (be.liquid.isEmpty() || be.liquid.getFluid() == out)) {
			int cost = be.energyCost(ENERGY_PER_OP);
			ops = Math.max(1, be.progressStep() / 100);
			ops = Math.min(ops, be.gas.getAmount() / GAS_PER_OP);
			ops = Math.min(ops, be.nitrogen / NITROGEN_PER_OP);
			ops = Math.min(ops, (TANK - be.liquid.getAmount()) / LIQUID_PER_OP);
			ops = Math.min(ops, be.energy.getEnergyStored() / Math.max(1, cost));
			if (ops > 0) {
				be.energy.removeInternal(cost * ops);
				be.gas.shrink(GAS_PER_OP * ops);
				if (be.gas.isEmpty()) be.gas = FluidStack.EMPTY;
				be.nitrogen -= NITROGEN_PER_OP * ops;
				if (be.liquid.isEmpty()) be.liquid = new FluidStack(out, LIQUID_PER_OP * ops); else be.liquid.grow(LIQUID_PER_OP * ops);
			}
		}
		if (!be.liquid.isEmpty()) {
			int left = GasTanks.push(level, pos, be.liquid.getFluid(), be.liquid.getAmount());
			if (left <= 0) be.liquid = FluidStack.EMPTY; else be.liquid.setAmount(left);
		}
		be.setChanged();
		boolean working = ops > 0;
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
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
		tag.put("gas", gas.writeToNBT(new CompoundTag()));
		tag.put("liquid", liquid.writeToNBT(new CompoundTag()));
		tag.putInt("nitrogen", nitrogen);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		gas = FluidStack.loadFluidStackFromNBT(tag.getCompound("gas"));
		liquid = FluidStack.loadFluidStackFromNBT(tag.getCompound("liquid"));
		nitrogen = tag.getInt("nitrogen");
	}

	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.cryogenic_condenser"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new RocketFuelMenu(com.robvanblerk.tieredpower.registry.ModMenus.CRYOGENIC_CONDENSER.get(), id, inv, this, data, false);
	}
}
