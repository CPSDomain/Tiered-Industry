package com.robvanblerk.tieredpower.factory;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
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

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;
import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModRecipes;

/**
 * Digital Factory controller. Build Factory Cells onto it (touching it or each other - any shape, up to 16 cells);
 * every cell runs 4 operations at once, so a full factory does 64 per cycle. Modes: Smelt (furnace recipes), Crush
 * (Pulverizer: an ore makes 2 dust), Wash (Ore Purifier: 3 dust, uses water) and Ore to Ingots (washes, then smelts the
 * dust: 3 ingots per ore). Factory Ports anywhere on the structure take items, water and power in and let items out.
 * Slots: 0-8 input, 9-17 output, 18-19 Speed and Energy upgrades.
 */
public class DigitalFactoryBlockEntity extends MachineBlockEntity {
	public static final int INPUTS = 9, OUTPUT_START = 9, OUTPUTS = 9;
	public static final int CAPACITY = 2_000_000, MAX_INPUT = 200_000;
	public static final int OPS_PER_CELL = 4, MAX_CELLS = 16, CYCLE_TICKS = 100;
	public static final int WATER_PER_OP = 250, TANK = 64_000;

	public enum Mode {
		// FE/t per operation: the same as the machine it replaces (Electric Furnace 20, Pulverizer 40, Ore Purifier 60),
		// and Ore to Ingots pays for both washing and smelting.
		SMELT("Smelt", 20), CRUSH("Crush", 40), WASH("Wash", 60), ORE_LINE("Ore to Ingots", 80);
		public final String label;
		public final int energyPerOp;
		Mode(String label, int energyPerOp) { this.label = label; this.energyPerOp = energyPerOp; }
		public boolean usesWater() { return this == WASH || this == ORE_LINE; }
	}

	private Mode mode = Mode.ORE_LINE;
	private int cells;
	private int progress, active, scanTicks;
	private final Set<BlockPos> structure = new HashSet<>();

	private final FluidTank water = new FluidTank(TANK, s -> s.getFluid().is(FluidTags.WATER)) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private LazyOptional<IFluidHandler> waterCap = LazyOptional.of(() -> water);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> CYCLE_TICKS;
				case 4 -> energyCost(mode.energyPerOp) * Math.max(1, active);
				case 5 -> mode.ordinal();
				case 6 -> cells;
				case 7 -> water.getFluidAmount();
				case 8 -> active;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return DigitalFactoryMenu.DATA_COUNT; }
	};

	public DigitalFactoryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DIGITAL_FACTORY.get(), pos, state, INPUTS + OUTPUTS + 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(INPUTS + OUTPUTS);
	}

	public int parallel() { return cells * OPS_PER_CELL; }
	public Mode getMode() { return mode; }
	public int getCells() { return cells; }
	public FluidTank water() { return water; }

	public void cycleMode() {
		mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
		progress = 0;
		setChanged();
	}

	// ---- structure ----

	/** Flood-fills the cells and ports touching the controller. */
	private void scan(Level level) {
		structure.clear();
		int found = 0;
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(worldPosition);
		seen.add(worldPosition);
		while (!queue.isEmpty() && seen.size() < 200) {
			BlockPos p = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (!seen.add(n) || !level.isLoaded(n)) continue;
				BlockState s = level.getBlockState(n);
				boolean cell = s.is(ModBlocks.FACTORY_CELL.get()), port = s.is(ModBlocks.FACTORY_PORT.get());
				if (!cell && !port) continue;
				if (cell && found >= MAX_CELLS) continue;
				if (cell) found++;
				structure.add(n.immutable());
				queue.add(n);
			}
		}
		cells = found;
		for (BlockPos p : structure)
			if (level.getBlockEntity(p) instanceof FactoryPortBlockEntity port) port.link(worldPosition);
	}

	public boolean owns(BlockPos part) {
		return structure.contains(part);
	}

	// ---- recipes ----

	/** What one input item becomes in the current mode (empty if nothing). */
	private ItemStack resultFor(Level level, ItemStack input) {
		ItemStack one = input.copyWithCount(1);
		return switch (mode) {
			case SMELT -> smelt(level, one);
			case CRUSH -> level.getRecipeManager().getRecipeFor(ModRecipes.PULVERIZING.get(), new SimpleContainer(one), level)
					.map(r -> r.getResult().copy()).orElse(ItemStack.EMPTY);
			case WASH -> wash(level, one);
			case ORE_LINE -> {
				ItemStack dust = wash(level, one);
				if (dust.isEmpty()) yield smelt(level, one);
				ItemStack ingot = smelt(level, dust.copyWithCount(1));
				yield ingot.isEmpty() ? dust : ingot.copyWithCount(ingot.getCount() * dust.getCount());
			}
		};
	}

	private static ItemStack smelt(Level level, ItemStack one) {
		return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(one), level)
				.map(r -> r.getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
	}

	private static ItemStack wash(Level level, ItemStack one) {
		Optional<MachineRecipe> r = level.getRecipeManager().getRecipeFor(MachineRecipe.Kind.PURIFYING.type(), new SimpleContainer(one), level);
		return r.filter(x -> x.getCount() <= 1).map(x -> x.getResult().copy()).orElse(ItemStack.EMPTY);
	}

	/** Puts 'stack' into the output slots of 'out' (a copy of the items list); false if it didn't all fit. */
	private static boolean insert(NonNullList<ItemStack> out, ItemStack stack) {
		ItemStack left = stack.copy();
		for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUTS && !left.isEmpty(); i++) {
			ItemStack o = out.get(i);
			if (!o.isEmpty() && ItemStack.isSameItemSameTags(o, left)) {
				int n = Math.min(left.getCount(), o.getMaxStackSize() - o.getCount());
				o.grow(n);
				left.shrink(n);
			}
		}
		for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUTS && !left.isEmpty(); i++) {
			if (out.get(i).isEmpty()) {
				int n = Math.min(left.getCount(), left.getMaxStackSize());
				out.set(i, left.copyWithCount(n));
				left.shrink(n);
			}
		}
		return left.isEmpty();
	}

	/** How many operations can run now: input items with a result, limited by cells and water. */
	private int countWork(Level level) {
		int n = 0;
		for (int i = 0; i < INPUTS && n < parallel(); i++) {
			ItemStack s = items.get(i);
			if (s.isEmpty() || resultFor(level, s).isEmpty()) continue;
			n += s.getCount();
		}
		n = Math.min(n, parallel());
		if (mode.usesWater()) n = Math.min(n, water.getFluidAmount() / WATER_PER_OP);
		if (n > 0) {
			// outputs full? then there's nothing to do
			NonNullList<ItemStack> sim = NonNullList.withSize(items.size(), ItemStack.EMPTY);
			for (int i = 0; i < items.size(); i++) sim.set(i, items.get(i).copy());
			boolean room = false;
			for (int i = 0; i < INPUTS && !room; i++) {
				ItemStack s = items.get(i);
				if (s.isEmpty()) continue;
				ItemStack res = resultFor(level, s);
				if (!res.isEmpty() && insert(sim, res)) room = true;
			}
			if (!room) n = 0;
		}
		return n;
	}

	/** End of a cycle: process up to 'active' items, as far as the outputs have room. */
	private void finishCycle(Level level) {
		int budget = active;
		NonNullList<ItemStack> sim = NonNullList.withSize(items.size(), ItemStack.EMPTY);
		for (int i = 0; i < items.size(); i++) sim.set(i, items.get(i).copy());
		int done = 0;
		for (int i = 0; i < INPUTS && budget > 0; i++) {
			ItemStack in = items.get(i);
			if (in.isEmpty()) continue;
			ItemStack res = resultFor(level, in);
			if (res.isEmpty()) continue;
			while (budget > 0 && !in.isEmpty()) {
				if (!insert(sim, res)) { budget = 0; break; }
				in.shrink(1);
				budget--;
				done++;
			}
		}
		for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUTS; i++) items.set(i, sim.get(i));
		if (mode.usesWater() && done > 0) water.drain(done * WATER_PER_OP, IFluidHandler.FluidAction.EXECUTE);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DigitalFactoryBlockEntity be) {
		if (be.scanTicks++ % 40 == 0) be.scan(level);
		if (be.preTick(level, pos, state)) return;
		if (be.mode.usesWater() && be.water.getSpace() >= 1_000) {
			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if (!level.isLoaded(next)) continue;
				BlockEntity n = level.getBlockEntity(next);
				if (n == null) continue;
				n.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(src ->
						FluidUtil.tryFluidTransfer(be.water, src, new FluidStack(Fluids.WATER, 4_000), true));
			}
		}
		if (be.progress == 0 || be.scanTicks % 10 == 0) {
			int work = be.countWork(level);
			if (be.progress == 0) be.active = work;
			else if (work == 0) { be.active = 0; be.progress = 0; }
		}
		int cost = be.energyCost(be.mode.energyPerOp) * be.active;
		boolean working = be.active > 0 && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= CYCLE_TICKS * 100) {
				be.finishCycle(level);
				be.progress = 0;
			}
			be.setChanged();
		}
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	// ---- capabilities and saving ----

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return waterCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); waterCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); waterCap = LazyOptional.of(() -> water); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("progress", progress);
		tag.putInt("active", active);
		tag.putInt("mode", mode.ordinal());
		tag.put("water", water.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		progress = tag.getInt("progress");
		active = tag.getInt("active");
		mode = Mode.values()[Math.max(0, Math.min(Mode.values().length - 1, tag.getInt("mode")))];
		water.readFromNBT(tag.getCompound("water"));
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		int[] all = new int[INPUTS + OUTPUTS];
		for (int i = 0; i < all.length; i++) all[i] = i;
		return all;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot < INPUTS;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUTS;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < INPUTS;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.digital_factory");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new DigitalFactoryMenu(id, inventory, this, data);
	}

	/** Takes power from any side, so it fits anywhere in a structure. */
	@Override
	protected boolean powerFromBottomOnly() {
		return false;
	}
}
