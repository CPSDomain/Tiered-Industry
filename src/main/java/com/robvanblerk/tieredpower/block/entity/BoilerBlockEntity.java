package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.BoilerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Heats water into steam and sends the steam up into a Steam Turbine placed directly on top.
 *
 * Heat sources:
 *  - Burning fuel in its slot (any furnace fuel): 100% heat.
 *  - Lava, magma, fire or a lit campfire directly underneath: 50% heat, no fuel needed.
 *
 *  - Lava in its lava tank (piped in, pulled from a neighbour, or from a lava bucket): 100% heat.
 *
 * Water sources:
 *  - Each water source block touching its sides adds 25 mB per tick for free.
 *  - Pulls water from any adjacent block that offers it (sinks, tanks, infinite water blocks).
 *  - Right-click with a water bucket, or pipe water in from any mod.
 */
public class BoilerBlockEntity extends MachineBlockEntity {
	public static final int WATER_CAPACITY = 8_000;       // mB (1,000 mB = 1 bucket)
	public static final int STEAM_CAPACITY = 4_000;       // mB
	public static final int MAX_STEAM_PER_TICK = 100;     // at 100% heat
	public static final int STEAM_PER_WATER = 10;         // 1 mB of water makes 10 mB of steam
	public static final int WATER_FROM_SOURCE = 25;       // mB per tick per adjacent water source
	public static final int LAVA_CAPACITY = 4_000;        // mB
	public static final int LAVA_PER_BURN = 50;           // mB of lava consumed per burn cycle
	public static final int TICKS_PER_LAVA_BURN = 1_000;  // same heat per mB as a vanilla lava bucket (20,000 ticks / 1,000 mB)
	public static final int PULL_WATER_PER_TICK = 100;    // max mB pulled from each neighbour per tick
	public static final int PULL_LAVA_PER_TICK = 50;

	private final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> stack.getFluid().is(FluidTags.WATER)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};
	private final FluidTank lava = new FluidTank(LAVA_CAPACITY, stack -> stack.getFluid().is(FluidTags.LAVA)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};

	/** What other mods see: two input tanks (water + lava). Nothing can be drained back out. */
	private final IFluidHandler inputs = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 3;
		}

		@Override
		public @NotNull FluidStack getFluidInTank(int tank) {
			if (tank == 2) return steam > 0 ? new FluidStack(com.robvanblerk.tieredpower.registry.ModFluids.STEAM.get(), steam) : FluidStack.EMPTY;
			return tank == 0 ? water.getFluid() : lava.getFluid();
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank == 0 ? WATER_CAPACITY : tank == 1 ? LAVA_CAPACITY : STEAM_CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
			return tank == 0 ? water.isFluidValid(stack) : tank == 1 && lava.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (water.isFluidValid(resource)) return water.fill(resource, action);
			if (lava.isFluidValid(resource)) return lava.fill(resource, action);
			return 0;
		}

		/** Steam can be taken out (by Gas Pipes); water and lava can't. */
		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			return resource.getFluid() == com.robvanblerk.tieredpower.registry.ModFluids.STEAM.get() ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			int amount = Math.min(maxDrain, steam);
			if (amount <= 0) return FluidStack.EMPTY;
			if (action.execute()) {
				steam -= amount;
				setChanged();
			}
			return new FluidStack(com.robvanblerk.tieredpower.registry.ModFluids.STEAM.get(), amount);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> inputs);

	private int steam;
	private int burnTime;
	private int burnTimeTotal;
	private int heat;           // 0, 50 or 100 (%)
	private int steamSent;      // last tick, for the GUI

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 2 -> water.getFluidAmount();
				case 3 -> steam;
				case 4 -> burnTime;
				case 5 -> burnTimeTotal;
				case 6 -> heat;
				case 7 -> steamSent;
				case 8 -> lava.getFluidAmount();
				default -> 0; // 0 and 1 are the (unused) energy slots
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return BoilerMenu.DATA_COUNT;
		}
	};

	public BoilerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BOILER.get(), pos, state, 1, 0, 0, 0);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BoilerBlockEntity be) {
		if (be.preTick(level, pos, state)) return; // paused by redstone control
		// 1. Top up water from adjacent source blocks.
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			FluidState fluid = level.getFluidState(pos.relative(dir));
			if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
				be.water.fill(new FluidStack(Fluids.WATER, WATER_FROM_SOURCE), IFluidHandler.FluidAction.EXECUTE);
			}
		}

		// 1b. Pull water and lava from neighbouring tanks, sinks, pipes, infinite source blocks, etc.
		for (Direction dir : Direction.values()) {
			if (dir == Direction.UP) continue; // that's the turbine
			BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
			if (neighbour == null || neighbour instanceof BoilerBlockEntity) continue;
			IFluidHandler source = neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
			if (source == null) continue;
			if (be.water.getSpace() > 0) {
				FluidUtil.tryFluidTransfer(be.water, source, new FluidStack(Fluids.WATER, PULL_WATER_PER_TICK), true);
			}
			if (be.lava.getSpace() > 0) {
				FluidUtil.tryFluidTransfer(be.lava, source, new FluidStack(Fluids.LAVA, PULL_LAVA_PER_TICK), true);
			}
		}

		// 2. Work out heat. Only light new fuel if there's water to boil and room for steam.
		boolean canBoil = be.water.getFluidAmount() > 0 && be.steam < STEAM_CAPACITY;
		if (be.burnTime <= 0 && canBoil && !hasPassiveHeat(level, pos)) {
			ItemStack fuel = be.items.get(0);
			int time = CoalGeneratorBlockEntity.burnTimeOf(fuel);
			if (time > 0) {
				be.burnTime = be.burnTimeTotal = time;
				if (fuel.hasCraftingRemainingItem() && fuel.getCount() == 1) {
					be.items.set(0, fuel.getCraftingRemainingItem());
				} else {
					fuel.shrink(1);
				}
			} else if (be.lava.getFluidAmount() >= LAVA_PER_BURN) {
				// No solid fuel: burn lava from the lava tank instead.
				be.lava.drain(LAVA_PER_BURN, IFluidHandler.FluidAction.EXECUTE);
				be.burnTime = be.burnTimeTotal = TICKS_PER_LAVA_BURN;
			}
		}

		if (be.burnTime > 0 && canBoil) {
			be.burnTime--;
			be.heat = 100;
		} else if (be.burnTime > 0) {
			be.heat = 0; // paused: steam is full (nothing is using it) or there's no water - fuel isn't wasted
		} else {
			be.heat = hasPassiveHeat(level, pos) ? 50 : 0;
		}

		// 3. Boil.
		if (be.heat > 0 && canBoil) {
			int wanted = Math.min(MAX_STEAM_PER_TICK * be.heat / 100, STEAM_CAPACITY - be.steam);
			int waterNeeded = Math.max(1, wanted / STEAM_PER_WATER);
			int waterUsed = be.water.drain(waterNeeded, IFluidHandler.FluidAction.EXECUTE).getAmount();
			be.steam += Math.min(wanted, waterUsed * STEAM_PER_WATER);
		}

		// 4. Send steam into a turbine on top, and into Gas Pipes / turbines on any other side.
		be.steamSent = 0;
		if (be.steam > 0 && level.getBlockEntity(pos.above()) instanceof SteamTurbineBlockEntity turbine) {
			int accepted = turbine.acceptSteam(be.steam);
			be.steam -= accepted;
			be.steamSent += accepted;
		}
		for (Direction dir : Direction.values()) {
			if (be.steam <= 0) break;
			BlockPos next = pos.relative(dir);
			if (!level.isLoaded(next)) continue;
			BlockEntity neighbour = level.getBlockEntity(next);
			if (neighbour == null || neighbour instanceof BoilerBlockEntity) continue;
			if (dir == Direction.UP && neighbour instanceof SteamTurbineBlockEntity) continue; // already fed above
			IFluidHandler target = neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
			if (target == null) continue;
			int accepted = target.fill(new FluidStack(com.robvanblerk.tieredpower.registry.ModFluids.STEAM.get(), be.steam), IFluidHandler.FluidAction.EXECUTE);
			be.steam -= accepted;
			be.steamSent += accepted;
		}

		boolean lit = be.heat > 0;
		if (state.getValue(MachineBlock.LIT) != lit) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
		}
		be.setChanged();
	}

	private static boolean hasPassiveHeat(Level level, BlockPos pos) {
		BlockPos below = pos.below();
		BlockState state = level.getBlockState(below);
		if (level.getFluidState(below).is(FluidTags.LAVA)) return true;
		if (state.is(Blocks.MAGMA_BLOCK) || state.is(BlockTags.FIRE)) return true;
		return state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT);
	}

	@Override
	protected boolean exposesEnergy() {
		return false;
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
		fluidCap = LazyOptional.of(() -> inputs);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("water", water.writeToNBT(new CompoundTag()));
		tag.put("lava", lava.writeToNBT(new CompoundTag()));
		tag.putInt("steam", steam);
		tag.putInt("burn_time", burnTime);
		tag.putInt("burn_time_total", burnTimeTotal);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		water.readFromNBT(tag.getCompound("water"));
		lava.readFromNBT(tag.getCompound("lava"));
		steam = tag.getInt("steam");
		burnTime = tag.getInt("burn_time");
		burnTimeTotal = tag.getInt("burn_time_total");
	}

	// Hoppers/pipes: fuel in from any side, empty buckets out of the bottom.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{0};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return CoalGeneratorBlockEntity.burnTimeOf(stack) > 0;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN && CoalGeneratorBlockEntity.burnTimeOf(stack) <= 0;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return CoalGeneratorBlockEntity.burnTimeOf(stack) > 0;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.boiler");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new BoilerMenu(containerId, inventory, this, data);
	}
}
