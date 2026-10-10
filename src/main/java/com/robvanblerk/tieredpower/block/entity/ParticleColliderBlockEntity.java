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
import com.robvanblerk.tieredpower.menu.ParticleColliderMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Makes antimatter by smashing fusion fuel together: each operation turns 10 mB deuterium + 10 mB tritium + 20,000 FE
 * into 1 mB of antimatter (one per tick, more with Speed Upgrades). Pipe the gases in; antimatter is pushed out into Gas
 * Pipes, or straight into an Antimatter Reactor next to it. Slots: 0-1 upgrades.
 */
public class ParticleColliderBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 10_000_000, MAX_INPUT = 1_000_000, ENERGY_PER_OP = 20_000, FUEL_PER_OP = 10, TANK = 16_000;
	private int deuterium, tritium, antimatter;
	private boolean working;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 3; }

		@Override
		public @NotNull FluidStack getFluidInTank(int t) {
			return switch (t) {
				case 0 -> deuterium > 0 ? new FluidStack(ModFluids.DEUTERIUM.get(), deuterium) : FluidStack.EMPTY;
				case 1 -> tritium > 0 ? new FluidStack(ModFluids.TRITIUM.get(), tritium) : FluidStack.EMPTY;
				default -> antimatter > 0 ? new FluidStack(ModFluids.ANTIMATTER.get(), antimatter) : FluidStack.EMPTY;
			};
		}

		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return t == 0 ? s.getFluid() == ModFluids.DEUTERIUM.get() : t == 1 && s.getFluid() == ModFluids.TRITIUM.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			boolean d = r.getFluid() == ModFluids.DEUTERIUM.get();
			if (!d && r.getFluid() != ModFluids.TRITIUM.get()) return 0;
			int n = Math.min(r.getAmount(), TANK - (d ? deuterium : tritium));
			if (a.execute() && n > 0) {
				if (d) deuterium += n; else tritium += n;
				setChanged();
			}
			return Math.max(0, n);
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return r.getFluid() == ModFluids.ANTIMATTER.get() ? drain(r.getAmount(), a) : FluidStack.EMPTY; }

		@Override
		public @NotNull FluidStack drain(int max, FluidAction a) {
			int n = Math.min(max, antimatter);
			if (n <= 0) return FluidStack.EMPTY;
			if (a.execute()) { antimatter -= n; setChanged(); }
			return new FluidStack(ModFluids.ANTIMATTER.get(), n);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> deuterium;
				case 3 -> tritium;
				case 4 -> antimatter;
				case 5 -> energyCost(ENERGY_PER_OP);
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return ParticleColliderMenu.DATA_COUNT; }
	};

	public ParticleColliderBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PARTICLE_COLLIDER.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(0);
	}

	private int carry;

	public int getAntimatter() { return antimatter; }

	public static void tick(Level level, BlockPos pos, BlockState state, ParticleColliderBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		// Speed upgrades: progress carries over between ticks (1 Speed card = 1.5 ops/tick, not rounded down to 1).
		int progress = be.carry + be.progressStep();
		int ops = progress / 100;
		be.carry = progress % 100;
		// Energy per op like every other machine: base x (1 + 0.75 x Speed) x 0.85^Energy, spread over the faster ops.
		int cost = (int) Math.max(1, (long) be.energyCost(com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.COLLIDER_FE_PER_MB)) * 100 / be.progressStep());
		ops = Math.min(ops, be.energy.getEnergyStored() / Math.max(1, cost));
		ops = Math.min(ops, Math.min(be.deuterium, be.tritium) / FUEL_PER_OP);
		ops = Math.min(ops, TANK - be.antimatter);
		be.working = ops > 0;
		if (be.working) {
			be.energy.removeInternal(cost * ops);
			be.deuterium -= FUEL_PER_OP * ops;
			be.tritium -= FUEL_PER_OP * ops;
			be.antimatter += ops;
		}
		if (be.antimatter > 0) be.antimatter = GasTanks.push(level, pos, ModFluids.ANTIMATTER.get(), be.antimatter);
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
		tag.putInt("deuterium", deuterium);
		tag.putInt("tritium", tritium);
		tag.putInt("antimatter", antimatter);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		deuterium = tag.getInt("deuterium");
		tritium = tag.getInt("tritium");
		antimatter = tag.getInt("antimatter");
	}

	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.particle_collider"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new ParticleColliderMenu(id, inv, this, data);
	}
}
