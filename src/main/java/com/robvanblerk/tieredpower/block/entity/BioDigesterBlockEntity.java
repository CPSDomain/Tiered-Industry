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
import com.robvanblerk.tieredpower.menu.BioDigesterMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;
import com.robvanblerk.tieredpower.registry.ModTags;

/**
 * Rots plant and animal matter into methane (biogas): crops, seeds, saplings, leaves, flowers, rotten flesh and more
 * each make 200 mB, one item every 2 seconds. Needs no power. Pushes methane into Gas Pipes or a Gas Burner Generator.
 */
public class BioDigesterBlockEntity extends MachineBlockEntity {
	public static final int TANK = 8_000, PER_ITEM = 200, SPORECAP_YIELD = 2_000, TICKS = 40, INPUT_SLOT = 0;

	private int methane, progress;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return methane > 0 ? new FluidStack(ModFluids.METHANE.get(), methane) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return false; }
		@Override public int fill(FluidStack r, FluidAction a) { return 0; }
		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return r.getFluid() == ModFluids.METHANE.get() ? drain(r.getAmount(), a) : FluidStack.EMPTY; }

		@Override
		public @NotNull FluidStack drain(int max, FluidAction a) {
			int n = Math.min(max, methane);
			if (n <= 0) return FluidStack.EMPTY;
			if (a.execute()) { methane -= n; setChanged(); }
			return new FluidStack(ModFluids.METHANE.get(), n);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 2 -> progress;
				case 3 -> TICKS;
				case 4 -> methane;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return BioDigesterMenu.DATA_COUNT; }
	};

	public BioDigesterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BIO_DIGESTER.get(), pos, state, 1, 1, 0, 0);
	}

	@Override
	protected boolean exposesEnergy() {
		return false; // bacteria do the work
	}

	public int getMethane() { return methane; }

	public static void tick(Level level, BlockPos pos, BlockState state, BioDigesterBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		ItemStack in = be.items.get(INPUT_SLOT);
		int yield = in.is(com.robvanblerk.tieredpower.registry.ModBlocks.SPORECAP.get().asItem()) ? SPORECAP_YIELD : PER_ITEM;
		boolean working = (in.is(ModTags.BIOMASS) || yield == SPORECAP_YIELD) && be.methane + yield <= TANK;
		if (working) {
			if (++be.progress >= TICKS) {
				be.progress = 0;
				in.shrink(1);
				be.methane += yield;
			}
		} else if (!in.is(ModTags.BIOMASS) && yield != SPORECAP_YIELD) {
			be.progress = 0;
		}
		if (be.methane > 0) be.methane = GasTanks.push(level, pos, ModFluids.METHANE.get(), be.methane);
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
		tag.putInt("methane", methane);
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		methane = tag.getInt("methane");
		progress = tag.getInt("progress");
	}

	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.is(ModTags.BIOMASS) || stack.is(com.robvanblerk.tieredpower.registry.ModBlocks.SPORECAP.get().asItem()); }
	@Override public int[] getSlotsForFace(Direction side) { return new int[]{INPUT_SLOT}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.bio_digester"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new BioDigesterMenu(id, inv, this, data);
	}
}
