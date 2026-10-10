package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.menu.WorkerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

/**
 * Plants saplings all around itself at its own height - on every other block out to 4 blocks away (24 trees), so each
 * tree has room to grow - and fells grown trees (logs and leaves), keeping the wood, sticks and apples. Saplings that
 * drop go back into its sapling slots. Fertilizer (or bone meal) in its own slot makes saplings grow even in the dark:
 * each Fertilizer is 32 growth steps, bone meal 8, and a sapling needs about 2. 200 FE per job.
 * Slots: 0-2 saplings, 3-11 output, 12-13 upgrades, 14 fertilizer.
 */
public class TreeFarmBlockEntity extends WorkerBlockEntity {
	public static final int RADIUS = 4, SPACING = 2, MAX_TREE = 256, SPECIAL_NO_SAPLINGS = 1;
	public static final int FERTILIZER_SLOT = 14, SLOTS = 15;
	public static final int PULSES_PER_FERTILIZER = 32, PULSES_PER_BONE_MEAL = 8;
	private int cursor, growthCharge;
	private long lastRestock;

	public TreeFarmBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.TREE_FARM.get(), pos, state, CAPACITY, MAX_INPUT, SLOTS); }
	@Override protected int costPerAction() { return 200; }
	@Override protected int interval() { return 5; }
	@Override public boolean isValidInput(ItemStack stack) { return stack.is(ItemTags.SAPLINGS) || stack.is(Items.BONE_MEAL) || stack.is(ModBlocks.FERTILIZER.get()); }
	@Override protected MenuType<WorkerMenu> menuType() { return ModMenus.TREE_FARM.get(); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.tree_farm"); }
	@Override protected int extraData() { return growthCharge; }

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, Direction facing) {
		if (level.getGameTime() - lastRestock >= 40) {
			lastRestock = level.getGameTime();
			restock(level, pos);
		}
		int perSide = RADIUS / SPACING * 2 + 1; // 5 x 5 grid of tree spots, centred on the machine
		int spots = perSide * perSide;
		boolean noSaplings = false;
		for (int tries = 0; tries < spots; tries++) { // look at the next spot until something needs doing
			cursor = (cursor + 1) % spots;
			int dx = (cursor % perSide) * SPACING - RADIUS, dz = (cursor / perSide) * SPACING - RADIUS;
			if (dx == 0 && dz == 0) continue; // that's the machine
			BlockPos spot = pos.offset(dx, 0, dz);
			if (!level.isLoaded(spot)) continue;
			BlockState s = level.getBlockState(spot);
			if (s.is(BlockTags.LOGS)) { fell(level, spot); return true; }
			if (s.is(BlockTags.SAPLINGS)) {
				if (grow(level, spot, s)) return true;
				continue;
			}
			if (s.isAir()) {
				int sap = findInput(st -> st.is(ItemTags.SAPLINGS) && st.getItem() instanceof BlockItem);
				if (sap < 0) { noSaplings = true; continue; }
				BlockState sapling = ((BlockItem) items.get(sap).getItem()).getBlock().defaultBlockState();
				if (sapling.canSurvive(level, spot)) {
					level.setBlock(spot, sapling, Block.UPDATE_ALL);
					items.get(sap).shrink(1);
					setChanged();
					return true;
				}
			}
		}
		if (noSaplings) { status = STATUS_SPECIAL; special = SPECIAL_NO_SAPLINGS; }
		return false;
	}

	/** One growth step for a sapling, from the growth charge (Fertilizer or bone meal). Works in the dark too. */
	private boolean grow(ServerLevel level, BlockPos spot, BlockState state) {
		if (!level.isEmptyBlock(spot.above()) || !level.isEmptyBlock(spot.above(2))) return false; // no room to grow: don't waste fertilizer
		if (growthCharge <= 0 && !refuel()) return false;
		if (state.getBlock() instanceof SaplingBlock sapling) {
			sapling.advanceTree(level, spot, state, level.random);
		} else if (state.getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(level, spot, state, false)) {
			b.performBonemeal(level, level.random, spot, state); // propagules, azaleas and other modded saplings
		} else {
			return false;
		}
		growthCharge--;
		level.levelEvent(1505, spot, 0); // the green sparkles
		setChanged();
		return true;
	}

	/** Uses one Fertilizer or bone meal (fertilizer slot first, then the sapling slots). */
	private boolean refuel() {
		int slot = CropFarmerBlockEntity.isFertilizer(items.get(FERTILIZER_SLOT)) ? FERTILIZER_SLOT : findInput(CropFarmerBlockEntity::isFertilizer);
		if (slot < 0) return false;
		ItemStack s = items.get(slot);
		growthCharge += s.is(ModBlocks.FERTILIZER.get()) ? PULSES_PER_FERTILIZER : PULSES_PER_BONE_MEAL;
		s.shrink(1);
		return true;
	}

	/**
	 * Next to a storage cable (or anything else on a storage network), it keeps itself stocked: Fertilizer (or bone meal)
	 * in the fertilizer slot, and more of the saplings already in its sapling slots.
	 */
	private void restock(Level level, BlockPos pos) {
		boolean nearNetwork = false;
		for (Direction d : Direction.values())
			if (level.getBlockState(pos.relative(d)).getBlock() instanceof com.robvanblerk.tieredpower.storage.StorageNetworkBlock) { nearNetwork = true; break; }
		if (!nearNetwork) return;
		var controller = com.robvanblerk.tieredpower.storage.StorageNetwork.findController(level, pos);
		var network = controller == null ? null : controller.getNetwork();
		if (network == null) return;
		ItemStack fert = items.get(FERTILIZER_SLOT);
		if (fert.getCount() < 32) {
			for (ItemStack want : new ItemStack[] {new ItemStack(ModBlocks.FERTILIZER.get()), new ItemStack(Items.BONE_MEAL)}) {
				if (!fert.isEmpty() && !ItemStack.isSameItemSameTags(fert, want)) continue;
				ItemStack got = network.extract(new com.robvanblerk.tieredpower.storage.ItemKey(want), want.getMaxStackSize() - fert.getCount(), false);
				if (got.isEmpty()) continue;
				if (fert.isEmpty()) items.set(FERTILIZER_SLOT, got); else fert.grow(got.getCount());
				setChanged();
				break;
			}
		}
		for (int i = 0; i < INPUTS; i++) {
			ItemStack s = items.get(i);
			if (s.isEmpty() || s.getCount() >= 32 || !s.is(ItemTags.SAPLINGS)) continue;
			ItemStack got = network.extract(new com.robvanblerk.tieredpower.storage.ItemKey(s), s.getMaxStackSize() - s.getCount(), false);
			if (!got.isEmpty()) { s.grow(got.getCount()); setChanged(); }
		}
	}

	/** Breaks the whole tree: connected logs and leaves, up to 256 blocks. */
	private void fell(ServerLevel level, BlockPos start) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		var player = fakePlayer(level);
		while (!queue.isEmpty() && seen.size() < MAX_TREE) {
			BlockPos p = queue.poll();
			if (!seen.add(p)) continue;
			BlockState s = level.getBlockState(p);
			if (!s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES)) continue;
			for (ItemStack d : Block.getDrops(s, level, p, null, player, ItemStack.EMPTY)) keep(level, p, d);
			level.destroyBlock(p, false);
			for (int dx = -1; dx <= 1; dx++)
				for (int dy = 0; dy <= 1; dy++)
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos n = p.offset(dx, dy, dz);
						if (!seen.contains(n) && Math.abs(n.getX() - start.getX()) <= 12 && Math.abs(n.getZ() - start.getZ()) <= 12) queue.add(n);
					}
		}
	}

	/** Saplings top up the sapling slots first (so it never runs out); everything else goes to the output. */
	private void keep(ServerLevel level, BlockPos p, ItemStack drop) {
		if (drop.is(ItemTags.SAPLINGS)) {
			for (int i = 0; i < INPUTS && !drop.isEmpty(); i++) {
				ItemStack s = items.get(i);
				if (s.isEmpty() || !ItemStack.isSameItemSameTags(s, drop)) continue;
				int n = Math.min(drop.getCount(), s.getMaxStackSize() - s.getCount());
				s.grow(n);
				drop.shrink(n);
			}
			for (int i = 0; i < INPUTS && !drop.isEmpty(); i++) {
				if (items.get(i).isEmpty()) { items.set(i, drop.copy()); drop = ItemStack.EMPTY; }
			}
			setChanged();
			if (drop.isEmpty()) return;
		}
		outputOrDrop(level, p, drop);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("growthCharge", growthCharge);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		growthCharge = tag.getInt("growthCharge");
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11} : new int[]{0, 1, 2, FERTILIZER_SLOT};
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == FERTILIZER_SLOT) return CropFarmerBlockEntity.isFertilizer(stack);
		return slot < INPUTS && isValidInput(stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}
}
