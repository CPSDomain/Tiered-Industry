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

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.menu.SteamEngineMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/** A small, cheap steam generator: up to 40 mB of steam per tick at 1 FE per mB, no spin-up. Idles when full. */
public class SteamEngineBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 50_000, MAX_PUSH = 1_000, TANK = 4_000, STEAM_PER_TICK = 40, FE_PER_MB = 1;

	private int steam, generating;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return steam > 0 ? new FluidStack(ModFluids.STEAM.get(), steam) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return s.getFluid() == ModFluids.STEAM.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (r.getFluid() != ModFluids.STEAM.get()) return 0;
			int n = Math.min(r.getAmount(), TANK - steam);
			if (a.execute() && n > 0) { steam += n; setChanged(); }
			return n;
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
		@Override public @NotNull FluidStack drain(int m, FluidAction a) { return FluidStack.EMPTY; }
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> steam;
				case 3 -> generating;
				default -> 0;
			};
		}

		@Override public void set(int i, int v) {}
		@Override public int getCount() { return SteamEngineMenu.DATA_COUNT; }
	};

	public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STEAM_ENGINE.get(), pos, state, 0, CAPACITY, 0, MAX_PUSH);
	}

	public int getGenerating() { return generating; }

	public static void tick(Level level, BlockPos pos, BlockState state, SteamEngineBlockEntity be) {
		boolean paused = be.preTick(level, pos, state);
		be.generating = 0;
		int use = Math.min(be.steam, STEAM_PER_TICK);
		int fe = Config.gen(use * FE_PER_MB);
		if (!paused && use > 0 && be.energy.getSpace() >= fe) {
			be.steam -= use;
			be.energy.addInternal(fe);
			be.generating = fe;
		}
		for (Direction dir : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), MAX_PUSH);
		}
		be.setChanged();
		boolean lit = be.generating > 0;
		BlockState now = level.getBlockState(pos);
		if (now.hasProperty(MachineBlock.LIT) && now.getValue(MachineBlock.LIT) != lit) level.setBlock(pos, now.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); fluidCap = LazyOptional.of(() -> fluids); }

	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("steam", steam); }
	@Override public void load(CompoundTag tag) { super.load(tag); steam = tag.getInt("steam"); }

	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.steam_engine"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new SteamEngineMenu(id, inv, data);
	}
}
