package com.robvanblerk.tieredpower.block.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.CropFarmerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.util.ItemUtil;

/**
 * Tends crops around itself at its own height: harvests ripe crops (wheat, carrots, potatoes, beetroot, nether wart)
 * and replants them, harvests melons and pumpkins, cuts sugar cane and cactus above the bottom block, and plants from its
 * seed slots: crop seeds on farmland, nether wart on soul sand, melon and pumpkin seeds, sugar cane next to water and
 * cactus on sand. Fertilizer (or bone meal) in its own slot speeds up whatever is still growing.
 * Slots: 0-2 seeds, 3-11 output, 12-13 upgrades, 14 fertilizer.
 */
public class CropFarmerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 40_000;
	public static final int MAX_INPUT = 2_000;
	public static final int ENERGY_PER_TICK = 20;
	public static final int TICKS_PER_SPOT = 4;
	public static final int SEED_SLOTS = 3, OUTPUT_START = 3, OUTPUT_END = 12, FERTILIZER_SLOT = 14;

	/** Growth left from the last Fertilizer or bone meal used: each pulse grows one plant one step. */
	public static final int PULSES_PER_FERTILIZER = 32, PULSES_PER_BONE_MEAL = 8;
	private int growthCharge;
	private int index; // which spot in the area is next
	private int progress;
	private int harvested;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> harvested & 0xFFFF;
				case 3 -> energyCost(ENERGY_PER_TICK);
				case 4 -> growthCharge;
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return CropFarmerMenu.DATA_COUNT;
		}
	};

	public CropFarmerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CROP_FARMER.get(), pos, state, 15, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(12);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CropFarmerBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		if (be.preTick(level, pos, state)) return;

		if (level.getGameTime() % 40 == 0) be.restock(level, pos);
		if (level.getGameTime() % 10 == 0) {
			for (int i = OUTPUT_START; i < OUTPUT_END; i++) {
				ItemStack stack = be.items.get(i);
				if (!stack.isEmpty()) be.items.set(i, ItemUtil.pushToNeighbours(level, pos, stack));
			}
		}

		int cost = be.energyCost(ENERGY_PER_TICK);
		boolean hasRoom = be.items.subList(OUTPUT_START, OUTPUT_END).stream().anyMatch(ItemStack::isEmpty);
		boolean working = hasRoom && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			while (be.progress >= TICKS_PER_SPOT * 100) {
				be.progress -= TICKS_PER_SPOT * 100;
				be.tendNext(server, pos);
			}
			be.setChanged();
		}
		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	private void tendNext(ServerLevel level, BlockPos origin) {
		int r = Config.get(Config.FARMER_RADIUS);
		int size = r * 2 + 1;
		index = Math.floorMod(index, size * size);
		BlockPos spot = origin.offset(index % size - r, 0, index / size - r);
		index = (index + 1) % (size * size);
		if (spot.equals(origin) || !level.isLoaded(spot)) return;

		BlockState state = level.getBlockState(spot);
		Block block = state.getBlock();

		if (block instanceof CropBlock crop) {
			if (crop.isMaxAge(state)) harvest(level, spot, state, crop.getStateForAge(0));
			else fertilize(level, spot, state);
		} else if (block instanceof NetherWartBlock) {
			if (state.getValue(NetherWartBlock.AGE) >= 3) harvest(level, spot, state, state.setValue(NetherWartBlock.AGE, 0));
			else fertilize(level, spot, state);
		} else if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
			harvest(level, spot, state, Blocks.AIR.defaultBlockState());
		} else if (block instanceof net.minecraft.world.level.block.StemBlock) {
			fertilize(level, spot, state);
		} else if (block == Blocks.SUGAR_CANE || block == Blocks.CACTUS) {
			// Leave the bottom block so it regrows; cut everything above it.
			boolean cut = false;
			for (BlockPos up = spot.above(); level.getBlockState(up).is(block); up = up.above()) {
				harvest(level, up, level.getBlockState(up), Blocks.AIR.defaultBlockState());
				cut = true;
			}
			if (!cut) fertilize(level, spot, state);
		} else if (state.isAir()) {
			plant(level, spot);
		}
	}

	public static boolean isFertilizer(ItemStack s) {
		return s.is(com.robvanblerk.tieredpower.registry.ModBlocks.FERTILIZER.get()) || s.is(net.minecraft.world.item.Items.BONE_MEAL);
	}

	/**
	 * Grows a plant that's still growing by one step, from the growth charge. When the charge runs out the farmer uses
	 * one Fertilizer (32 steps) or bone meal (8 steps) from the fertilizer slot - or, as before, from a seed slot. So a
	 * stack of Fertilizer lasts a long time: 2,048 growth steps.
	 */
	private void fertilize(ServerLevel level, BlockPos spot, BlockState state) {
		Block block = state.getBlock();
		boolean growing = block instanceof CropBlock crop ? !crop.isMaxAge(state)
				: block instanceof NetherWartBlock ? state.getValue(NetherWartBlock.AGE) < 3
				: block instanceof net.minecraft.world.level.block.StemBlock ? ((net.minecraft.world.level.block.BonemealableBlock) block).isValidBonemealTarget(level, spot, state, false)
				: block == Blocks.SUGAR_CANE || block == Blocks.CACTUS;
		if (!growing) return;
		if (growthCharge <= 0 && !refuel()) return;
		net.minecraft.world.level.block.state.properties.IntegerProperty age = ageOf(state);
		if ((block instanceof CropBlock || block instanceof NetherWartBlock) && age != null) {
			int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
			level.setBlock(spot, state.setValue(age, Math.min(max, state.getValue(age) + 1)), Block.UPDATE_CLIENTS);
		} else if (block instanceof net.minecraft.world.level.block.BonemealableBlock b) {
			b.performBonemeal(level, level.random, spot, state);
		} else {
			// sugar cane and cactus: a few growth ticks
			for (int n = 0; n < 4 && level.getBlockState(spot).is(block); n++) level.getBlockState(spot).randomTick(level, spot, level.random);
		}
		growthCharge--;
		if (level.random.nextInt(4) == 0) level.levelEvent(1505, spot, 0);
		setChanged();
	}

	/** A crop's "age" property, whatever the crop calls it internally. */
	private static net.minecraft.world.level.block.state.properties.IntegerProperty ageOf(BlockState state) {
		for (var p : state.getProperties())
			if (p instanceof net.minecraft.world.level.block.state.properties.IntegerProperty ip && p.getName().equals("age")) return ip;
		return null;
	}

	/** Uses one Fertilizer or bone meal for more growth charge. */
	private boolean refuel() {
		int slot = -1;
		if (isFertilizer(items.get(FERTILIZER_SLOT))) slot = FERTILIZER_SLOT;
		else for (int i = 0; i < SEED_SLOTS && slot < 0; i++) if (isFertilizer(items.get(i))) slot = i;
		if (slot < 0) return false;
		ItemStack s = items.get(slot);
		growthCharge += s.is(com.robvanblerk.tieredpower.registry.ModBlocks.FERTILIZER.get()) ? PULSES_PER_FERTILIZER : PULSES_PER_BONE_MEAL;
		s.shrink(1);
		return true;
	}

	/**
	 * Next to a storage cable (or anything else on a storage network), the farmer keeps itself stocked: it tops the
	 * fertilizer slot up with Fertilizer (or bone meal if there's none) and its seed slots with more of the seeds in them.
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
			for (ItemStack want : new ItemStack[] {new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.FERTILIZER.get()), new ItemStack(net.minecraft.world.item.Items.BONE_MEAL)}) {
				if (!fert.isEmpty() && !ItemStack.isSameItemSameTags(fert, want)) continue;
				int room = want.getMaxStackSize() - fert.getCount();
				ItemStack got = network.extract(new com.robvanblerk.tieredpower.storage.ItemKey(want), room, false);
				if (got.isEmpty()) continue;
				if (fert.isEmpty()) items.set(FERTILIZER_SLOT, got); else fert.grow(got.getCount());
				setChanged();
				break;
			}
		}
		for (int i = 0; i < SEED_SLOTS; i++) {
			ItemStack s = items.get(i);
			if (s.isEmpty() || s.getCount() >= 32 || !isPlantable(s)) continue;
			ItemStack got = network.extract(new com.robvanblerk.tieredpower.storage.ItemKey(s), s.getMaxStackSize() - s.getCount(), false);
			if (!got.isEmpty()) { s.grow(got.getCount()); setChanged(); }
		}
	}

	private void harvest(ServerLevel level, BlockPos spot, BlockState state, BlockState replacement) {
		List<ItemStack> drops = Block.getDrops(state, level, spot, null);
		level.levelEvent(2001, spot, Block.getId(state));
		level.setBlock(spot, replacement, Block.UPDATE_ALL);
		for (ItemStack drop : drops) store(drop);
		harvested++;
	}

	/** Plants the first seed (from the seed slots) that can grow here. */
	private void plant(ServerLevel level, BlockPos spot) {
		for (int i = 0; i < SEED_SLOTS; i++) {
			ItemStack seeds = items.get(i);
			if (!isPlantable(seeds)) continue;
			Block block = seeds.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : Blocks.SUGAR_CANE; // Sugar Cane Seeds
			BlockState planted = block.defaultBlockState();
			if (!planted.canSurvive(level, spot)) continue;
			level.setBlock(spot, planted, Block.UPDATE_ALL);
			seeds.shrink(1);
			return;
		}
	}

	private void store(ItemStack drop) {
		ItemStack remaining = drop;
		// Seeds top up the seed slots first.
		for (int i = 0; i < SEED_SLOTS && !remaining.isEmpty(); i++) remaining = merge(i, remaining);
		for (int i = OUTPUT_START; i < OUTPUT_END && !remaining.isEmpty(); i++) remaining = merge(i, remaining);
	}

	private ItemStack merge(int slot, ItemStack stack) {
		ItemStack existing = items.get(slot);
		if (slot < SEED_SLOTS) {
			if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, stack)) return stack; // don't start new seed stacks
		}
		if (existing.isEmpty()) {
			items.set(slot, stack);
			return ItemStack.EMPTY;
		}
		if (!ItemStack.isSameItemSameTags(existing, stack)) return stack;
		int move = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
		existing.grow(move);
		stack.shrink(move);
		return stack;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("index", index);
		tag.putInt("harvested", harvested);
		tag.putInt("growthCharge", growthCharge);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		index = tag.getInt("index");
		harvested = tag.getInt("harvested");
		growthCharge = tag.getInt("growthCharge");
	}

	// Hoppers: seeds in from the top/sides, harvest out from the bottom.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11} : new int[]{0, 1, 2, FERTILIZER_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= OUTPUT_START && slot < OUTPUT_END;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == FERTILIZER_SLOT) return isFertilizer(stack);
		return slot < SEED_SLOTS && isPlantable(stack);
	}

	/** Things it can plant: crop seeds, nether wart, melon and pumpkin seeds, sugar cane and cactus. */
	public static boolean isPlantable(ItemStack stack) {
		if (stack.is(com.robvanblerk.tieredpower.registry.ModBlocks.SUGAR_CANE_SEEDS.get())) return true;
		if (!(stack.getItem() instanceof BlockItem b)) return false;
		Block block = b.getBlock();
		return block instanceof CropBlock || block instanceof NetherWartBlock || block instanceof net.minecraft.world.level.block.StemBlock
				|| block == Blocks.SUGAR_CANE || block == Blocks.CACTUS;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.crop_farmer");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new CropFarmerMenu(containerId, inventory, this, data);
	}
}
