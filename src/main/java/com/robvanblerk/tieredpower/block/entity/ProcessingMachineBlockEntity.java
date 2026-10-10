package com.robvanblerk.tieredpower.block.entity;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import com.robvanblerk.tieredpower.menu.ProcessingMachineMenu;
import com.robvanblerk.tieredpower.recipe.MachineRecipe;

/**
 * Shared logic for the Ore Purifier, Compressor and Electric Sawmill: one input slot, a main output,
 * a secondary output (e.g. sawdust), two upgrade slots, and (for the purifier) a water tank.
 * Slots: 0 input, 1 output, 2 secondary output, 3-4 upgrades.
 */
public abstract class ProcessingMachineBlockEntity extends MachineBlockEntity {
	public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1, SECONDARY_SLOT = 2;
	public static final int CAPACITY = 40_000;
	public static final int MAX_INPUT = 2_000;
	public static final int TANK_CAPACITY = 8_000;

	private final MachineRecipe.Kind kind;
	private final int energyPerTick;
	private final int waterPerOperation;

	private final FluidTank water = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid().is(FluidTags.WATER)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};
	private LazyOptional<IFluidHandler> waterCap = LazyOptional.of(() -> water);

	private int progress;
	private int maxProgress = 100;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> maxProgress;
				case 4 -> energyCost(energyPerTick);
				case 5 -> water.getFluidAmount();
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return ProcessingMachineMenu.DATA_COUNT;
		}
	};

	protected ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
			MachineRecipe.Kind kind, int energyPerTick, int waterPerOperation) {
		super(type, pos, state, 5, CAPACITY, MAX_INPUT, 0);
		this.kind = kind;
		this.energyPerTick = energyPerTick;
		this.waterPerOperation = waterPerOperation;
		enableUpgrades(3); // slots 3 and 4: Speed and Efficiency upgrades
	}

	@Override
	public boolean supportsTiers() {
		return true;
	}

	public boolean usesWater() {
		return waterPerOperation > 0;
	}

	public int getWaterPerOperation() {
		return waterPerOperation;
	}

	public MachineRecipe.Kind getKind() {
		return kind;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ProcessingMachineBlockEntity be) {
		if (be.preTick(level, pos, state)) return;

		if (be.usesWater() && be.water.getSpace() > 0) {
			for (Direction dir : Direction.values()) {
				BlockPos next = pos.relative(dir);
				if (!level.isLoaded(next)) continue;
				BlockEntity neighbour = level.getBlockEntity(next);
				if (neighbour == null) continue;
				neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(source ->
						FluidUtil.tryFluidTransfer(be.water, source, new FluidStack(Fluids.WATER, 1_000), true));
			}
		}

		ItemStack input = be.items.get(INPUT_SLOT);
		Optional<MachineRecipe> recipe = input.isEmpty() ? Optional.empty()
				: level.getRecipeManager().getRecipeFor(be.kind.type(), new SimpleContainer(input), level);

		ItemStack result = recipe.map(MachineRecipe::getResult).orElse(ItemStack.EMPTY);
		ItemStack secondary = recipe.map(MachineRecipe::getSecondary).orElse(ItemStack.EMPTY);
		// How many run side by side: the tier's lanes, limited by input, output room and water.
		int batch = 0;
		if (!result.isEmpty()) {
			batch = Math.min(be.lanes(), input.getCount() / Math.max(1, recipe.get().getCount()));
			batch = Math.min(batch, roomFor(be.items.get(OUTPUT_SLOT), result));
			if (!secondary.isEmpty()) batch = Math.min(batch, roomFor(be.items.get(SECONDARY_SLOT), secondary));
			if (be.usesWater()) batch = Math.min(batch, be.water.getFluidAmount() / Math.max(1, be.waterPerOperation));
		}
		int cost = be.energyCost(be.energyPerTick) * Math.max(1, batch);
		boolean working = batch > 0 && be.energy.getEnergyStored() >= cost;

		if (working) {
			be.maxProgress = recipe.get().getTime();
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= be.maxProgress * 100) {
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, result.copyWithCount(result.getCount() * batch));
				if (!secondary.isEmpty()) MachineBlockEntity.merge(be.items, SECONDARY_SLOT, secondary.copyWithCount(secondary.getCount() * batch));
				input.shrink(recipe.get().getCount() * batch);
				if (be.usesWater()) be.water.drain(be.waterPerOperation * batch, IFluidHandler.FluidAction.EXECUTE);
				be.progress = 0;
			}
			be.setChanged();
		} else if (result.isEmpty() && be.progress != 0) {
			be.progress = 0;
			be.setChanged();
		}

		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER && usesWater()) return waterCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		waterCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		waterCap = LazyOptional.of(() -> water);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("progress", progress);
		if (usesWater()) tag.put("water", water.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		progress = tag.getInt("progress");
		if (usesWater()) water.readFromNBT(tag.getCompound("water"));
	}

	// Hoppers/pipes: input from the top and sides, both outputs from the bottom.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{OUTPUT_SLOT, SECONDARY_SLOT} : new int[]{INPUT_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == INPUT_SLOT;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT || slot == SECONDARY_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == INPUT_SLOT;
	}

	protected abstract String translationKey();

	@Override
	public Component getDisplayName() {
		return Component.translatable(translationKey());
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new ProcessingMachineMenu(menuType(), containerId, inventory, this, data, usesWater());
	}

	protected abstract net.minecraft.world.inventory.MenuType<?> menuType();
}
