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
import net.minecraft.world.item.Items;
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
import com.robvanblerk.tieredpower.menu.FreezerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Freezes fluids and compresses ice:
 *  - 9 Ice -> Packed Ice, 9 Packed Ice -> Blue Ice (from the input slot, takes priority)
 *  - 1,000 mB water -> Ice, 1,000 mB lava -> Obsidian (from its tank)
 * Pulls water or lava from neighbouring tanks, sinks and pumps; buckets work too.
 */
public class FreezerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 40_000;
	public static final int MAX_INPUT = 2_000;
	public static final int ENERGY_PER_TICK = 40;
	public static final int TICKS = 100;
	public static final int TANK_CAPACITY = 8_000;
	public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1;

	private final FluidTank tank = new FluidTank(TANK_CAPACITY,
			stack -> stack.getFluid().is(FluidTags.WATER) || stack.getFluid().is(FluidTags.LAVA)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
	private int progress;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> TICKS;
				case 4 -> tank.getFluidAmount();
				case 5 -> tank.isEmpty() ? 0 : tank.getFluid().getFluid().is(FluidTags.LAVA) ? 2 : 1;
				case 6 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return FreezerMenu.DATA_COUNT;
		}
	};

	public FreezerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FREEZER.get(), pos, state, 4, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(2); // slots 2 and 3: Speed and Efficiency upgrades
	}

	/** What the freezer would make right now, and whether it uses the input slot (true) or the tank (false). */
	private ItemStack currentResult(boolean[] fromItems) {
		ItemStack input = items.get(INPUT_SLOT);
		if (input.is(Items.ICE) && input.getCount() >= 9) { fromItems[0] = true; return new ItemStack(Items.PACKED_ICE); }
		if (input.is(Items.PACKED_ICE) && input.getCount() >= 9) { fromItems[0] = true; return new ItemStack(Items.BLUE_ICE); }
		fromItems[0] = false;
		if (tank.getFluidAmount() >= 1_000) {
			return tank.getFluid().getFluid().is(FluidTags.LAVA) ? new ItemStack(Items.OBSIDIAN) : new ItemStack(Items.ICE);
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean supportsTiers() {
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FreezerBlockEntity be) {
		if (be.preTick(level, pos, state)) return;

		// Pull water or lava from neighbours.
		if (be.tank.getSpace() > 0) {
			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if (!level.isLoaded(next)) continue;
				BlockEntity neighbour = level.getBlockEntity(next);
				if (neighbour == null) continue;
				IFluidHandler source = neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
				if (source == null) continue;
				FluidUtil.tryFluidTransfer(be.tank, source, new FluidStack(Fluids.WATER, 1_000), true);
				FluidUtil.tryFluidTransfer(be.tank, source, new FluidStack(Fluids.LAVA, 1_000), true);
			}
		}

		boolean[] fromItems = new boolean[1];
		ItemStack result = be.currentResult(fromItems);
		int batch = 0;
		if (!result.isEmpty()) {
			int have = fromItems[0] ? be.items.get(INPUT_SLOT).getCount() / 9 : be.tank.getFluidAmount() / 1_000;
			batch = Math.min(Math.min(be.lanes(), have), roomFor(be.items.get(OUTPUT_SLOT), result));
		}
		int cost = be.energyCost(ENERGY_PER_TICK) * Math.max(1, batch);
		boolean working = batch > 0 && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS * 100) {
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, result.copyWithCount(result.getCount() * batch));
				if (fromItems[0]) be.items.get(INPUT_SLOT).shrink(9 * batch);
				else be.tank.drain(1_000 * batch, IFluidHandler.FluidAction.EXECUTE);
				be.progress = 0;
			}
			be.setChanged();
		} else if (result.isEmpty() && be.progress != 0) {
			be.progress = 0;
		}
		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
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
		fluidCap = LazyOptional.of(() -> tank);
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
		tank.readFromNBT(tag.getCompound("tank"));
		progress = tag.getInt("progress");
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{INPUT_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == INPUT_SLOT && (stack.is(Items.ICE) || stack.is(Items.PACKED_ICE));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.freezer");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FreezerMenu(containerId, inventory, this, data);
	}
}
