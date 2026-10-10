package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.menu.FissionReactorMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Fission Reactor. Up to 4 Uranium Fuel Rods; each burns for 5 minutes, then becomes a Depleted Fuel Rod.
 * Every active rod adds heat. Output scales with heat and is full at 50% heat. Above 50% the reactor cools itself with
 * water (5 mB per heat point), which flashes to steam (pushed into Gas Pipes / turbines for bonus power).
 * At 100% heat it SCRAMs (shuts down until it's cooled) - or, if enabled in the config, it explodes.
 * Slots: 0-3 fuel rods.
 */
public class FissionReactorBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 2_000_000;
	public static final int OUTPUT_PER_ROD = 800;
	public static final int ROD_TICKS = 6_000;
	public static final int MAX_HEAT = 1_000, TARGET_HEAT = 500, MAX_COOLING = 20, WATER_PER_HEAT = 5, STEAM_PER_HEAT = 50;
	public static final int WATER_CAPACITY = 16_000, STEAM_CAPACITY = 16_000;
	public static final int RODS = 4;

	private final int[] burn = new int[RODS];
	private int heat;
	private int activeRods;
	private int generating;
	private boolean scram;
	/** MOX rods burning this tick (each adds half a rod's worth of power and heat). */
	private int moxActive;
	private int steam;

	private final FluidTank water = new FluidTank(WATER_CAPACITY, s -> s.getFluid().is(FluidTags.WATER)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};

	/** Water goes in; steam comes out. */
	private final IFluidHandler fluids = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 2;
		}

		@Override
		public @NotNull FluidStack getFluidInTank(int tank) {
			if (tank == 0) return water.getFluid();
			return steam > 0 ? new FluidStack(ModFluids.STEAM.get(), steam) : FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank == 0 ? WATER_CAPACITY : STEAM_CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
			return tank == 0 && water.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return water.fill(resource, action);
		}

		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			return resource.getFluid() == ModFluids.STEAM.get() ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			int amount = Math.min(maxDrain, steam);
			if (amount <= 0) return FluidStack.EMPTY;
			if (action.execute()) {
				steam -= amount;
				setChanged();
			}
			return new FluidStack(ModFluids.STEAM.get(), amount);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> heat;
				case 3 -> activeRods;
				case 4 -> water.getFluidAmount();
				case 5 -> steam;
				case 6 -> generating;
				case 7 -> scram ? 1 : 0;
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return FissionReactorMenu.DATA_COUNT;
		}
	};

	public FissionReactorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FISSION_REACTOR.get(), pos, state, RODS, CAPACITY, 0, 16_000);
	}

	public int getHeat() { return heat; }
	public int getGenerating() { return generating; }
	public boolean isScrammed() { return scram; }

	// ---- Neutron Reflector ----
	private boolean reflector;

	public boolean hasReflector() {
		return reflector;
	}

	public void setReflector(boolean fitted) {
		reflector = fitted;
		setChanged();
	}

	@Override
	public java.util.List<ItemStack> installedDrops() {
		return reflector ? java.util.List.of(new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.NEUTRON_REFLECTOR.get())) : java.util.List.of();
	}

	// ---- nitrogen cooling (Cryo Injector) ----
	private int cryoTicks;

	/** Is the reactor making power right now? (Tritium Breeders and Cryo Injectors need it running.) */
	public boolean isRunning() {
		return generating > 0;
	}

	/** Called each tick by a supplied Cryo Injector: nitrogen cooling lets it run harder, +50% power. */
	public boolean applyCryoBoost() {
		if (generating <= 0 && cryoTicks <= 0) return false;
		cryoTicks = 2;
		return true;
	}

	public boolean isCryoBoosted() {
		return cryoTicks > 0;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FissionReactorBlockEntity be) {
		boolean paused = be.preTick(level, pos, state);

		// Pull water from neighbours (Sink, Pump, pipes, tanks).
		if (be.water.getSpace() > 0) {
			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if (!level.isLoaded(next)) continue;
				BlockEntity n = level.getBlockEntity(next);
				if (n == null) continue;
				n.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(src ->
						FluidUtil.tryFluidTransfer(be.water, src, new FluidStack(Fluids.WATER, 1_000), true));
			}
		}

		// Burn fuel.
		be.activeRods = 0;
		int moxRods = 0;
		if (!paused && !be.scram && be.energy.getSpace() > 0) { // idles (no fuel used) while its power buffer is full
			for (int i = 0; i < RODS; i++) {
				ItemStack rod = be.items.get(i);
				if (!com.robvanblerk.tieredpower.energy.FuelRods.isFuel(rod)) {
					be.burn[i] = 0;
					continue;
				}
				boolean mox = com.robvanblerk.tieredpower.energy.FuelRods.isMox(rod);
				if (be.burn[i] <= 0) be.burn[i] = mox ? ROD_TICKS * 2 : ROD_TICKS; // MOX lasts twice as long
				if (!be.reflector || level.getGameTime() % 3 != 0) be.burn[i]--; // a Neutron Reflector makes rods last 50% longer
				be.activeRods++;
				if (mox) moxRods++;
				if (be.burn[i] <= 0) be.items.set(i, new ItemStack(ModBlocks.DEPLETED_FUEL_ROD.get()));
			}
		}

		be.moxActive = moxRods;
		// Heat and cooling. A MOX rod counts as 1.5 rods (an extra point of heat every other tick).
		be.heat += be.activeRods + (level.getGameTime() % 2 == 0 ? moxRods : 0);
		if (be.heat > TARGET_HEAT) {
			int cool = Math.min(Math.min(be.heat - TARGET_HEAT, MAX_COOLING), be.water.getFluidAmount() / WATER_PER_HEAT);
			if (cool > 0) {
				be.heat -= cool;
				be.water.drain(cool * WATER_PER_HEAT, IFluidHandler.FluidAction.EXECUTE);
				be.steam = Math.min(STEAM_CAPACITY, be.steam + cool * STEAM_PER_HEAT);
			}
		}
		if (be.activeRods == 0 && be.heat > 0) be.heat = Math.max(0, be.heat - 2); // cools down when idle

		if (be.heat >= MAX_HEAT) {
			if (Config.get(Config.FISSION_MELTDOWN)) {
				level.removeBlock(pos, false);
				level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0f, Level.ExplosionInteraction.BLOCK);
				return;
			}
			be.scram = true;
		}
		if (be.scram && be.heat <= 100) be.scram = false;

		// Power: full output from 50% heat up.
		be.generating = (int) ((be.activeRods + be.moxActive * 0.5) * OUTPUT_PER_ROD * Math.min(1.0, be.heat / (double) TARGET_HEAT));
		if (be.cryoTicks > 0) {
			be.cryoTicks--;
			be.generating = be.generating * 3 / 2; // nitrogen-cooled: +50%
		}
		if (be.generating > 0) be.energy.addInternal(com.robvanblerk.tieredpower.Config.gen(be.generating));

		for (Direction dir : Direction.values()) {
			if (be.energy.getEnergyStored() > 0) EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), 16_000);
			if (be.steam > 0) {
				BlockPos next = pos.relative(dir);
				if (!level.isLoaded(next)) continue;
				BlockEntity n = level.getBlockEntity(next);
				if (n == null) continue;
				IFluidHandler target = n.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
				if (target != null) be.steam -= target.fill(new FluidStack(ModFluids.STEAM.get(), be.steam), IFluidHandler.FluidAction.EXECUTE);
			}
		}

		be.setChanged();
		boolean running = be.activeRods > 0;
		BlockState now = level.getBlockState(pos);
		if (now.hasProperty(MachineBlock.LIT) && now.getValue(MachineBlock.LIT) != running) {
			level.setBlock(pos, now.setValue(MachineBlock.LIT, running), Block.UPDATE_ALL);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		fluidCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		fluidCap = LazyOptional.of(() -> fluids);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("reflector", reflector);
		tag.putIntArray("burn", burn);
		tag.putInt("heat", heat);
		tag.putBoolean("scram", scram);
		tag.putInt("steam", steam);
		tag.put("water", water.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		reflector = tag.getBoolean("reflector");
		int[] saved = tag.getIntArray("burn");
		for (int i = 0; i < RODS && i < saved.length; i++) burn[i] = saved[i];
		heat = tag.getInt("heat");
		scram = tag.getBoolean("scram");
		steam = tag.getInt("steam");
		water.readFromNBT(tag.getCompound("water"));
	}

	// Hoppers/pipes: fuel rods in from any side; depleted rods out from any side.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{0, 1, 2, 3};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return stack.is(ModBlocks.DEPLETED_FUEL_ROD.get());
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return com.robvanblerk.tieredpower.energy.FuelRods.isFuel(stack) && items.get(slot).isEmpty();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.fission_reactor");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FissionReactorMenu(containerId, inventory, this, data);
	}
}
