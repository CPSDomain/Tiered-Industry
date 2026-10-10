package com.robvanblerk.tieredpower.conveyor;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Shared by every belt: lets hoppers, pipes and Export Buses drop items onto the belt, and holds the splitter's
 * turn counter and the Filter Belt's filter.
 */
public class ConveyorBlockEntity extends BlockEntity {
	public static final int FILTER_SIZE = 9;
	/** At most this many item entities on one belt before it stops accepting more from hoppers and pipes. */
	public static final int MAX_ON_BELT = 4;

	private int turn;
	private boolean sortRight;
	private final List<ItemStack> filter = new ArrayList<>();

	public ConveyorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CONVEYOR.get(), pos, state);
	}

	// ---- splitter ----

	/** Splitter: alternates between the outputs it is given. */
	public Direction nextOutput(List<Direction> outputs) {
		Direction d = outputs.get(Math.floorMod(turn, outputs.size()));
		turn++;
		setChanged();
		return d;
	}

	// ---- filter belt ----

	public boolean sortRight() {
		return sortRight;
	}

	public void flipSide() {
		sortRight = !sortRight;
		setChanged();
	}

	public List<ItemStack> filter() {
		return filter;
	}

	public boolean matches(ItemStack stack) {
		for (ItemStack f : filter) if (ItemStack.isSameItem(f, stack)) return true;
		return false;
	}

	/** Adds the item to the filter, or takes it out if it's already there. Returns true if it was added. */
	public boolean toggle(ItemStack stack) {
		for (int i = 0; i < filter.size(); i++) {
			if (ItemStack.isSameItem(filter.get(i), stack)) {
				filter.remove(i);
				setChanged();
				return false;
			}
		}
		if (filter.size() >= FILTER_SIZE) return false;
		filter.add(stack.copyWithCount(1));
		setChanged();
		return true;
	}

	/** Lines for Jade. */
	public List<String> info() {
		List<String> out = new ArrayList<>();
		if (getBlockState().getBlock() instanceof ConveyorBlock b)
			out.add(String.format("%s: %.2f blocks/s", ConveyorBlock.TIER_NAMES[b.tier()], b.speed() * 20));
		if (getBlockState().getBlock() instanceof FilterConveyorBlock) {
			if (filter.isEmpty()) out.add("!No filter - right-click with an item to add one");
			else {
				StringBuilder s = new StringBuilder("Sends " + (sortRight ? "right" : "left") + ": ");
				for (int i = 0; i < filter.size(); i++) s.append(i > 0 ? ", " : "").append(filter.get(i).getHoverName().getString());
				out.add(s.toString());
			}
		}
		return out;
	}

	// ---- items in from hoppers / pipes ----

	private final IItemHandler input = new IItemHandler() {
		@Override public int getSlots() { return 1; }
		@Override public @NotNull ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
		@Override public int getSlotLimit(int slot) { return 64; }
		@Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
		@Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }

		@Override
		public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
			if (stack.isEmpty() || level == null) return stack;
			AABB box = new AABB(worldPosition).inflate(0, 0.5, 0);
			if (level.getEntitiesOfClass(ItemEntity.class, box).size() >= MAX_ON_BELT) return stack;
			if (!simulate) {
				ItemEntity e = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5, stack.copy(), 0, 0, 0);
				e.setPickUpDelay(10);
				level.addFreshEntity(e);
			}
			return ItemStack.EMPTY;
		}
	};
	private LazyOptional<IItemHandler> inputCap = LazyOptional.of(() -> input);

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) return inputCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		inputCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		inputCap = LazyOptional.of(() -> input);
	}

	// ---- saving ----

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("turn", turn);
		tag.putBoolean("sortRight", sortRight);
		ListTag list = new ListTag();
		for (ItemStack f : filter) list.add(f.save(new CompoundTag()));
		tag.put("filter", list);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		turn = tag.getInt("turn");
		sortRight = tag.getBoolean("sortRight");
		filter.clear();
		ListTag list = tag.getList("filter", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			ItemStack s = ItemStack.of(list.getCompound(i));
			if (!s.isEmpty()) filter.add(s);
		}
	}
}
