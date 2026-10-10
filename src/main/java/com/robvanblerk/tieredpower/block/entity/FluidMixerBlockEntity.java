package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
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
import com.robvanblerk.tieredpower.menu.FluidMixerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Mixes fluids with items:
 *  - any concrete powder + 250 mB water -> concrete (same colour)
 *  - dirt + 250 mB water -> mud
 *  - nothing in the slot: 1,000 mB lava + 1,000 mB water -> obsidian
 * Pulls water and lava from neighbours (Sink, Pump, pipes); buckets work too.
 * Slots: 0 input, 1 output, 2-3 upgrades.
 */
public class FluidMixerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 40_000, MAX_INPUT = 2_000, ENERGY_PER_TICK = 20, TICKS = 20, TANK = 8_000;
	public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1;

	private final FluidTank water = tank(true);
	private final FluidTank lava = tank(false);
	private int progress;

	private FluidTank tank(boolean isWater) {
		return new FluidTank(TANK, s -> s.getFluid().is(isWater ? FluidTags.WATER : FluidTags.LAVA)) {
			@Override
			protected void onContentsChanged() {
				setChanged();
			}
		};
	}

	private final IFluidHandler inputs = new IFluidHandler() {
		@Override public int getTanks() { return 2; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return t == 0 ? water.getFluid() : lava.getFluid(); }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return t == 0 ? water.isFluidValid(s) : lava.isFluidValid(s); }
		@Override public int fill(FluidStack r, FluidAction a) { return water.isFluidValid(r) ? water.fill(r, a) : lava.isFluidValid(r) ? lava.fill(r, a) : 0; }
		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
		@Override public @NotNull FluidStack drain(int m, FluidAction a) { return FluidStack.EMPTY; }
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> inputs);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> TICKS;
				case 4 -> water.getFluidAmount();
				case 5 -> lava.getFluidAmount();
				case 6 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return FluidMixerMenu.DATA_COUNT;
		}
	};

	public FluidMixerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FLUID_MIXER.get(), pos, state, 4, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(2);
	}

	/** Concrete powder -> the matching concrete, found by name (white_concrete_powder -> white_concrete). */
	public static @Nullable Item concreteFor(ItemStack powder) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(powder.getItem());
		if (!id.getPath().endsWith("_concrete_powder")) return null;
		Item concrete = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().replace("_concrete_powder", "_concrete")));
		return concrete == Items.AIR ? null : concrete;
	}

	public static boolean isMixable(ItemStack stack) {
		return stack.is(Items.DIRT) || concreteFor(stack) != null;
	}

	@Override
	public boolean supportsTiers() {
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FluidMixerBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		for (Direction dir : Direction.values()) {
			BlockPos next = pos.relative(dir);
			if (!level.isLoaded(next)) continue;
			BlockEntity n = level.getBlockEntity(next);
			if (n == null) continue;
			IFluidHandler src = n.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
			if (src == null) continue;
			if (be.water.getSpace() > 0) FluidUtil.tryFluidTransfer(be.water, src, new FluidStack(Fluids.WATER, 1_000), true);
			if (be.lava.getSpace() > 0) FluidUtil.tryFluidTransfer(be.lava, src, new FluidStack(Fluids.LAVA, 1_000), true);
		}

		ItemStack input = be.items.get(INPUT_SLOT);
		ItemStack result = ItemStack.EMPTY;
		int waterUse = 0, lavaUse = 0;
		Item concrete = concreteFor(input);
		if (concrete != null) { result = new ItemStack(concrete); waterUse = 250; }
		else if (input.is(Items.DIRT)) { result = new ItemStack(Items.MUD); waterUse = 250; }
		else if (input.isEmpty()) { result = new ItemStack(Items.OBSIDIAN); waterUse = 1_000; lavaUse = 1_000; }

		int batch = 0;
		if (!result.isEmpty()) {
			batch = Math.min(be.lanes(), roomFor(be.items.get(OUTPUT_SLOT), result));
			if (!input.isEmpty()) batch = Math.min(batch, input.getCount());
			if (waterUse > 0) batch = Math.min(batch, be.water.getFluidAmount() / waterUse);
			if (lavaUse > 0) batch = Math.min(batch, be.lava.getFluidAmount() / lavaUse);
		}
		int cost = be.energyCost(ENERGY_PER_TICK) * Math.max(1, batch);
		boolean working = batch > 0 && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS * 100) {
				be.progress = 0;
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, result.copyWithCount(result.getCount() * batch));
				if (!input.isEmpty()) input.shrink(batch);
				be.water.drain(waterUse * batch, IFluidHandler.FluidAction.EXECUTE);
				be.lava.drain(lavaUse * batch, IFluidHandler.FluidAction.EXECUTE);
			}
			be.setChanged();
		}
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
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
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		water.readFromNBT(tag.getCompound("water"));
		lava.readFromNBT(tag.getCompound("lava"));
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
		return slot == INPUT_SLOT && isMixable(stack);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.fluid_mixer");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FluidMixerMenu(containerId, inventory, this, data);
	}
}
