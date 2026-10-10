package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
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
import com.robvanblerk.tieredpower.menu.OxygenFurnaceMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Oxygen-blown steelmaking: blows oxygen through iron to burn off the carbon - 1 iron ingot (or iron dust) + 100 mB of
 * oxygen = 1 steel ingot in 3 seconds, using 80 FE/t and no coal. Speed Upgrades make it faster.
 * Slots: 0 iron, 1 steel, 2-3 upgrades.
 */
public class OxygenFurnaceBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 50_000, MAX_INPUT = 2_000, ENERGY_PER_TICK = 80, TICKS = 60, OXYGEN_PER_INGOT = 100, TANK = 8_000;
	public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1;
	private static final TagKey<Item> IRON_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ingots/iron"));
	private static final TagKey<Item> IRON_DUSTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "dusts/iron"));

	private int oxygen, progress;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return oxygen > 0 ? new FluidStack(ModFluids.OXYGEN.get(), oxygen) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return s.getFluid() == ModFluids.OXYGEN.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (r.getFluid() != ModFluids.OXYGEN.get()) return 0;
			int n = Math.min(r.getAmount(), TANK - oxygen);
			if (a.execute() && n > 0) { oxygen += n; setChanged(); }
			return Math.max(0, n);
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
				case 2 -> progress / 100;
				case 3 -> TICKS;
				case 4 -> oxygen;
				case 5 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return OxygenFurnaceMenu.DATA_COUNT; }
	};

	public OxygenFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.OXYGEN_FURNACE.get(), pos, state, 4, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(2);
	}

	public static boolean isIron(ItemStack stack) {
		return stack.is(IRON_INGOTS) || stack.is(IRON_DUSTS);
	}

	@Override
	public boolean supportsTiers() {
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, OxygenFurnaceBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		ItemStack in = be.items.get(INPUT_SLOT);
		ItemStack steel = new ItemStack(ModBlocks.STEEL_INGOT.get());
		int batch = !isIron(in) ? 0 : Math.min(Math.min(be.lanes(), in.getCount()), Math.min(be.oxygen / OXYGEN_PER_INGOT, roomFor(be.items.get(OUTPUT_SLOT), steel)));
		int cost = be.energyCost(ENERGY_PER_TICK) * Math.max(1, batch);
		boolean working = batch > 0 && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS * 100) {
				be.progress = 0;
				in.shrink(batch);
				be.oxygen -= OXYGEN_PER_INGOT * batch;
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, steel.copyWithCount(batch));
			}
			be.setChanged();
		} else if (!isIron(in)) {
			be.progress = 0;
		}
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
		tag.putInt("oxygen", oxygen);
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		oxygen = tag.getInt("oxygen");
		progress = tag.getInt("progress");
	}

	@Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{INPUT_SLOT}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT_SLOT; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == INPUT_SLOT && isIron(stack); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.oxygen_furnace"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new OxygenFurnaceMenu(id, inv, this, data);
	}
}
