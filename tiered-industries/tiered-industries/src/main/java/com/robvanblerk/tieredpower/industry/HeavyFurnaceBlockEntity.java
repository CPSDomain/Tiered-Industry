package com.robvanblerk.tieredpower.industry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Early-game heavy industry - no power needed, the materials heat themselves.
 * Coke Oven (coke = true): coal -> Coal Coke + 250 mB Creosote Oil (30 s); logs -> charcoal + 125 mB Creosote (15 s).
 * Industrial Blast Furnace: iron ingot + Coal Coke -> steel ingot (20 s).
 * Slots: 0 input, 1 second input (blast furnace coke), 2 output.
 */
public class HeavyFurnaceBlockEntity extends MachineBlockEntity {
	public static final int SIZE = 3, OUTPUT = 2, TANK = 8_000;
	private static final TagKey<Item> IRON = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ingots/iron"));
	private final boolean coke;
	private int progress, maxProgress;
	public final FluidTank creosote = new FluidTank(TANK) {
		@Override protected void onContentsChanged() { setChanged(); }
		@Override public int fill(net.minecraftforge.fluids.FluidStack r, FluidAction a) { return 0; } // only the oven fills it
	};
	private LazyOptional<IFluidHandler> tankCap = LazyOptional.of(() -> creosote);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> progress;
				case 1 -> maxProgress;
				case 2 -> creosote.getFluidAmount();
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return HeavyFurnaceMenu.DATA_COUNT; }
	};

	public HeavyFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.HEAVY_FURNACE.get(), pos, state, SIZE, 1, 0, 0);
		this.coke = state.getBlock() == ModBlocks.COKE_OVEN.get();
	}

	public boolean isCokeOven() { return coke; }

	/** What the current inputs make: result, creosote mB, time in ticks - or null. */
	private record Job(ItemStack result, int creosoteMb, int time) {}

	private @Nullable Job job() {
		ItemStack in = items.get(0);
		if (in.isEmpty()) return null;
		if (coke) {
			if (in.is(Items.COAL)) return new Job(new ItemStack(ModBlocks.COAL_COKE.get()), 250, 600);
			if (in.is(ItemTags.LOGS_THAT_BURN)) return new Job(new ItemStack(Items.CHARCOAL), 125, 300);
			return null;
		}
		if (in.is(IRON) && items.get(1).is(ModBlocks.COAL_COKE.get())) return new Job(new ItemStack(ModBlocks.STEEL_INGOT.get()), 0, 400);
		return null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, HeavyFurnaceBlockEntity be) {
		if (level.isClientSide()) return;
		Job job = be.job();
		ItemStack out = be.items.get(OUTPUT);
		boolean room = job != null && (out.isEmpty() || ItemStack.isSameItemSameTags(out, job.result()) && out.getCount() < out.getMaxStackSize())
				&& be.creosote.getFluidAmount() + job.creosoteMb() <= TANK;
		boolean working = false;
		if (room) {
			working = true;
			be.maxProgress = job.time();
			if (++be.progress >= job.time()) {
				be.progress = 0;
				be.items.get(0).shrink(1);
				if (!be.coke) be.items.get(1).shrink(1);
				if (out.isEmpty()) be.items.set(OUTPUT, job.result().copy()); else out.grow(1);
				if (job.creosoteMb() > 0) {
					var cur = be.creosote.getFluid();
					be.creosote.setFluid(new net.minecraftforge.fluids.FluidStack(ModFluids.CREOSOTE.get(), cur.getAmount() + job.creosoteMb()));
				}
			}
		} else {
			be.progress = 0;
		}
		be.setChanged();
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == 0) return coke ? stack.is(Items.COAL) || stack.is(ItemTags.LOGS_THAT_BURN) : stack.is(IRON);
		if (slot == 1) return !coke && stack.is(ModBlocks.COAL_COKE.get());
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{OUTPUT} : coke ? new int[]{0} : new int[]{0, 1};
	}

	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT; }

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (coke && cap == ForgeCapabilities.FLUID_HANDLER) return tankCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); tankCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); tankCap = LazyOptional.of(() -> creosote); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("progress", progress);
		tag.put("creosote", creosote.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		progress = tag.getInt("progress");
		creosote.readFromNBT(tag.getCompound("creosote"));
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(coke ? "block.tieredpower.coke_oven" : "block.tieredpower.industrial_blast_furnace");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new HeavyFurnaceMenu(coke ? com.robvanblerk.tieredpower.registry.ModMenus.COKE_OVEN.get() : com.robvanblerk.tieredpower.registry.ModMenus.INDUSTRIAL_BLAST_FURNACE.get(),
				id, inv, this, data, coke);
	}
}
