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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.TritiumBreederMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;
import com.robvanblerk.tieredpower.registry.ModTags;

/**
 * Breeds tritium for fusion from lithium, using the neutrons of a running Fission Reactor next to it (the single-block
 * reactor, or a multiblock's Fission Controller). One lithium ingot becomes 250 mB of tritium gas - one fusion fuel
 * pair's worth - in 20 seconds. Needs no power of its own. Pushes tritium into Gas Pipes or a Fusion Reactor.
 */
public class TritiumBreederBlockEntity extends MachineBlockEntity {
	public static final int TANK = 8_000, PER_INGOT = 250, TICKS = 400, INPUT_SLOT = 0;

	private int tritium, progress;
	private boolean reactorRunning;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return tritium > 0 ? new FluidStack(ModFluids.TRITIUM.get(), tritium) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return false; }
		@Override public int fill(FluidStack r, FluidAction a) { return 0; }
		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return r.getFluid() == ModFluids.TRITIUM.get() ? drain(r.getAmount(), a) : FluidStack.EMPTY; }

		@Override
		public @NotNull FluidStack drain(int max, FluidAction a) {
			int n = Math.min(max, tritium);
			if (n <= 0) return FluidStack.EMPTY;
			if (a.execute()) { tritium -= n; setChanged(); }
			return new FluidStack(ModFluids.TRITIUM.get(), n);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 2 -> progress;
				case 3 -> TICKS;
				case 4 -> tritium;
				case 5 -> reactorRunning ? 1 : 0;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return TritiumBreederMenu.DATA_COUNT; }
	};

	public TritiumBreederBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TRITIUM_BREEDER.get(), pos, state, 1, 1, 0, 0);
	}

	@Override
	protected boolean exposesEnergy() {
		return false; // runs on the reactor's neutrons, not power
	}

	public boolean isReactorRunning() { return reactorRunning; }
	public int getTritium() { return tritium; }

	static boolean runningReactorNextTo(Level level, BlockPos pos) {
		for (Direction d : Direction.values()) {
			BlockEntity n = level.getBlockEntity(pos.relative(d));
			if (n instanceof FissionReactorBlockEntity r && r.isRunning()) return true;
			if (n instanceof FissionControllerBlockEntity c && c.isRunning()) return true;
		}
		return false;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TritiumBreederBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		if (level.getGameTime() % 20 == 0) be.reactorRunning = runningReactorNextTo(level, pos);
		ItemStack in = be.items.get(INPUT_SLOT);
		boolean working = be.reactorRunning && in.is(ModTags.LITHIUM_INGOTS) && be.tritium + PER_INGOT <= TANK;
		if (working) {
			if (++be.progress >= TICKS) {
				be.progress = 0;
				in.shrink(1);
				be.tritium += PER_INGOT;
			}
		} else if (!in.is(ModTags.LITHIUM_INGOTS)) {
			be.progress = 0;
		}
		if (be.tritium > 0) be.tritium = GasTanks.push(level, pos, ModFluids.TRITIUM.get(), be.tritium);
		be.setChanged();
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
		tag.putInt("tritium", tritium);
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		tritium = tag.getInt("tritium");
		progress = tag.getInt("progress");
	}

	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.is(ModTags.LITHIUM_INGOTS); }
	@Override public int[] getSlotsForFace(Direction side) { return new int[]{INPUT_SLOT}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.tritium_breeder"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new TritiumBreederMenu(id, inv, this, data);
	}
}
