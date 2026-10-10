package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.util.ItemUtil;

/**
 * Item Buffer (27 slots) and Fluid Buffer (32,000 mB): take things in from any side except the front, and steadily push
 * them out of the front - 16 items every 4 ticks, or 1,000 mB a tick - so a fast producer can queue up for a slower
 * machine without stalling.
 */
public class BufferBlockEntity extends BlockEntity {
	public static final int SLOTS = 27, TANK = 32_000;
	private final boolean fluid;
	public final ItemStackHandler items = new ItemStackHandler(SLOTS) {
		@Override protected void onContentsChanged(int slot) { setChanged(); }
	};
	public final FluidTank tank = new FluidTank(TANK) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
	private int ticks;

	public BufferBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BUFFER.get(), pos, state);
		this.fluid = state.getBlock() == com.robvanblerk.tieredpower.registry.ModBlocks.FLUID_BUFFER.get();
	}

	public boolean isFluid() { return fluid; }

	private Direction front() {
		return getBlockState().hasProperty(DirectionalBlock.FACING) ? getBlockState().getValue(DirectionalBlock.FACING) : Direction.NORTH;
	}

	public int itemCount() {
		int n = 0;
		for (int i = 0; i < SLOTS; i++) n += items.getStackInSlot(i).getCount();
		return n;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BufferBlockEntity be) {
		Direction front = be.front();
		if (be.fluid) {
			if (be.tank.isEmpty()) return;
			BlockEntity n = level.getBlockEntity(pos.relative(front));
			IFluidHandler target = n == null ? null : n.getCapability(ForgeCapabilities.FLUID_HANDLER, front.getOpposite()).orElse(null);
			if (target == null) return;
			FluidStack offer = be.tank.drain(1_000, IFluidHandler.FluidAction.SIMULATE);
			int filled = target.fill(offer, IFluidHandler.FluidAction.EXECUTE);
			if (filled > 0) be.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
		} else {
			if (++be.ticks % 4 != 0) return;
			IItemHandler target = ItemUtil.neighbour(level, pos, front);
			if (target == null) return;
			int budget = 16;
			for (int i = 0; i < SLOTS && budget > 0; i++) {
				ItemStack peek = be.items.extractItem(i, budget, true);
				if (peek.isEmpty()) continue;
				ItemStack left = ItemHandlerHelper.insertItemStacked(target, peek, false);
				int moved = peek.getCount() - left.getCount();
				if (moved > 0) {
					be.items.extractItem(i, moved, false);
					budget -= moved;
				}
			}
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (side != null && side == front()) return super.getCapability(cap, side); // the front only gives out (pushed by the buffer)
		if (!fluid && cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
		if (fluid && cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); itemCap.invalidate(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); itemCap = LazyOptional.of(() -> items); fluidCap = LazyOptional.of(() -> tank); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("items", items.serializeNBT());
		tag.put("tank", tank.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		items.deserializeNBT(tag.getCompound("items"));
		tank.readFromNBT(tag.getCompound("tank"));
	}
}
