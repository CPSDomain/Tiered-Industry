package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.ElectricPumpMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Pumps fluid source blocks from the lake or pool below it (it looks straight down through air until it finds fluid,
 * then searches the connected fluid within 32 blocks). Water is left in place (endless); other fluids are removed.
 * Pushes what it pumps into tanks/machines beside or above it.
 * Tier Installers make it pump several buckets per cycle (3/5/7/9/12), with a bigger tank, a wider reach and faster
 * output; Speed and Energy upgrades work as on any machine.
 */
public class ElectricPumpBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 50_000;
	public static final int MAX_INPUT = 2_000;
	public static final int ENERGY_PER_TICK = 25;
	public static final int TICKS_PER_BUCKET = 20;
	public static final int TANK_CAPACITY = 16_000;
	public static final int RANGE = 32;
	public static final int MAX_SCAN = 4_096;
	public static final int PUSH_PER_TICK = 1_000;

	private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};
	/** Outside view of the pump's tank: drain only. */
	private final IFluidHandler output = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public @NotNull FluidStack getFluidInTank(int t) {
			return tank.getFluid();
		}

		@Override
		public int getTankCapacity(int t) {
			return tankCapacity();
		}

		@Override
		public boolean isFluidValid(int t, @NotNull FluidStack stack) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			return tank.drain(resource, action);
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			return tank.drain(maxDrain, action);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> output);

	/** Per tier (Basic to Naquadah): how far out it searches the lake below. */
	public static final int[] TIER_RANGE = {32, 48, 64, 80, 96, 128};

	public int tankCapacity() { return TANK_CAPACITY * lanes(); }
	public int range() { return TIER_RANGE[Math.min(getTier(), TIER_RANGE.length - 1)]; }

	@Override
	public boolean supportsTiers() {
		return true;
	}

	@Override
	public String tierEffect(int tier) {
		int lanes = TIER_LANES[tier];
		return lanes + " buckets per cycle, " + String.format("%,d", TANK_CAPACITY * lanes) + " mB tank, " + TIER_RANGE[Math.min(tier, TIER_RANGE.length - 1)] + "-block reach";
	}

	@Override
	public void setTier(int tier) {
		super.setTier(tier);
		tank.setCapacity(tankCapacity());
	}

	private int progress;
	private boolean noFluid;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> tank.getFluidAmount() & 0xFFFF;
				case 3 -> tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
				case 4 -> noFluid ? 1 : 0;
				case 5 -> energyCost(ENERGY_PER_TICK) * lanes();
				case 6 -> (tank.getFluidAmount() >>> 16) & 0xFFFF;
				case 7 -> tankCapacity() / 1000;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return ElectricPumpMenu.DATA_COUNT;
		}
	};

	public ElectricPumpBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ELECTRIC_PUMP.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(0); // slots 0 and 1: Speed and Efficiency upgrades
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ElectricPumpBlockEntity be) {
		if (be.preTick(level, pos, state)) return;

		// Push fluid out to the sides and top.
		if (!be.tank.isEmpty()) {
			for (Direction dir : Direction.values()) {
				if (dir == Direction.DOWN || be.tank.isEmpty()) continue;
				BlockPos next = pos.relative(dir);
				if (!level.isLoaded(next)) continue;
				BlockEntity neighbour = level.getBlockEntity(next);
				if (neighbour == null) continue;
				neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite())
						.ifPresent(target -> FluidUtil.tryFluidTransfer(target, be.tank, PUSH_PER_TICK * be.lanes(), true));
			}
		}

		if (be.tank.getCapacity() != be.tankCapacity()) be.tank.setCapacity(be.tankCapacity());
		int cost = be.energyCost(ENERGY_PER_TICK) * be.lanes();
		boolean working = be.tank.getSpace() >= 1_000 && be.energy.getEnergyStored() >= cost && !be.noFluidCached(level, pos);
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS_PER_BUCKET * 100) {
				be.progress = 0;
				be.pumpOnce(level, pos);
			}
			be.setChanged();
		}
		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	/** Re-checks for fluid below once a second so an empty pump doesn't waste power. */
	private boolean noFluidCached(Level level, BlockPos pos) {
		if (level.getGameTime() % 20 == 0) noFluid = findStart(level, pos) == null;
		return noFluid;
	}

	/** First block with fluid straight below the pump (looking down through air). */
	private static @Nullable BlockPos findStart(Level level, BlockPos pos) {
		BlockPos p = pos.below();
		for (int i = 0; i < 64 && p.getY() >= level.getMinBuildHeight(); i++, p = p.below()) {
			if (!level.getFluidState(p).isEmpty()) return p;
			if (!level.getBlockState(p).isAir()) return null;
		}
		return null;
	}

	/** One pumping cycle: up to one bucket per lane. */
	private void pumpOnce(Level level, BlockPos pos) {
		BlockPos start = findStart(level, pos);
		if (start == null) {
			noFluid = true;
			return;
		}
		int want = lanes();
		if (level.getFluidState(start).is(FluidTags.WATER)) {
			// water is endless: one source is enough
			BlockPos source = findSource(level, start, range(), 1).stream().findFirst().orElse(null);
			if (source == null) return;
			FluidStack stack = new FluidStack(level.getFluidState(source).getType(), 1_000 * want);
			int room = tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) / 1_000 * 1_000;
			if (room > 0) tank.fill(new FluidStack(stack.getFluid(), room), IFluidHandler.FluidAction.EXECUTE);
			return;
		}
		for (BlockPos source : findSource(level, start, range(), want)) {
			FluidState fluidState = level.getFluidState(source);
			FluidStack stack = new FluidStack(fluidState.getType(), 1_000);
			if (tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) < 1_000) return;
			tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
			level.setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	/** Searches the connected body of fluid and returns up to 'count' source blocks, farthest first (any for water). */
	private static java.util.List<BlockPos> findSource(Level level, BlockPos start, int range, int count) {
		java.util.List<BlockPos> sources = new java.util.ArrayList<>();
		FluidState startFluid = level.getFluidState(start);
		Deque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(start);
		seen.add(start);
		int maxScan = MAX_SCAN * Math.max(1, range * range / (RANGE * RANGE));
		while (!queue.isEmpty() && seen.size() < maxScan) {
			BlockPos p = queue.poll();
			FluidState fs = level.getFluidState(p);
			if (fs.isSource()) {
				sources.add(p);
				if (fs.is(FluidTags.WATER)) return sources;
			}
			for (Direction dir : Direction.values()) {
				if (dir == Direction.UP && p.getY() >= start.getY()) continue;
				BlockPos next = p.relative(dir);
				if (seen.contains(next) || !level.isLoaded(next)) continue;
				if (Math.abs(next.getX() - start.getX()) > range || Math.abs(next.getZ() - start.getZ()) > range) continue;
				if (!level.getFluidState(next).getType().isSame(startFluid.getType())) continue;
				seen.add(next);
				queue.add(next);
			}
		}
		// farthest first, so lakes empty from the edges in
		java.util.Collections.reverse(sources);
		return sources.subList(0, Math.min(count, sources.size()));
	}

	public FluidTank getTank() {
		return tank;
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
		fluidCap = LazyOptional.of(() -> output);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("tank", tank.writeToNBT(new CompoundTag()));
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		tank.setCapacity(tankCapacity());
		tank.readFromNBT(tag.getCompound("tank"));
		progress = tag.getInt("progress");
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.electric_pump");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new ElectricPumpMenu(containerId, inventory, this, data);
	}

	/** Needs its bottom for its work area, so it takes power from any side. */
	@Override
	protected boolean powerFromBottomOnly() {
		return false;
	}
}
