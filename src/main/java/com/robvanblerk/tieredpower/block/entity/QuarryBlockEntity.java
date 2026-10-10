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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.QuarryMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.util.ItemUtil;

/**
 * Mines a square area layer by layer. Settings (in its GUI): area size, centred on the quarry or behind it,
 * how deep to go, and tool mode (normal / Silk Touch / Fortune III). Items in the void filter are thrown away.
 * Drops are as if mined with a diamond pickaxe. Skips air, fluids, unbreakable blocks and anything with a block entity (chests, spawners...).
 * Items go into its 9-slot buffer and are pushed into neighbouring inventories; it pauses when everything is full.
 * Slots: 0-8 buffer, 9-10 upgrades.
 */
public class QuarryBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 200_000;
	public static final int MAX_INPUT = 16_000;
	public static final int ENERGY_PER_TICK = 80;
	public static final int TICKS_PER_BLOCK = 5;
	public static final int BUFFER_SLOTS = 9;
	private static final int SCAN_PER_TICK = 256;
	public static final int MAX_RADIUS = 32;
	public static final String[] TOOL_MODES = {"Normal", "Silk Touch", "Fortune III"};

	private static ItemStack tool(int mode) {
		ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
		if (mode == 1) pick.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, 1);
		if (mode == 2) pick.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, 3);
		return pick;
	}

	// Settings
	private int radius = -1;          // -1 = use the config default
	private boolean behind;           // false = centred on the quarry, true = area behind it
	private int minY = Integer.MIN_VALUE; // lowest layer to mine (MIN_VALUE = bottom of the world)
	private int toolMode;
	private final net.minecraft.world.SimpleContainer voidFilter = new net.minecraft.world.SimpleContainer(6);
	private int centreX, centreZ, topY;
	/** A custom rectangle set with a Quarry Planner (instead of centred/behind). */
	private boolean custom;
	private int customMinX, customMaxX, customMinZ, customMaxZ, customTopY;
	/** The area being mined right now (worked out when mining starts). */
	private int areaMinX, areaMaxX, areaMinZ, areaMaxZ;
	public static final int MAX_CUSTOM_SIZE = 65;

	private int cursorX, cursorY, cursorZ;
	private boolean started, finished, stuck;
	private int mined;
	private int progress;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> cursorY;
				case 3 -> mined & 0xFFFF;
				case 4 -> (mined >>> 16) & 0xFFFF;
				case 5 -> finished ? 2 : stuck ? 1 : 0;
				case 6 -> energyCost(ENERGY_PER_TICK) * (toolMode > 0 ? 3 : 2) / 2;
				case 7 -> radius();
				case 8 -> behind ? 1 : 0;
				case 9 -> effectiveMinY();
				case 10 -> toolMode;
				case 11 -> custom ? 1 : 0;
				case 12 -> customMaxX - customMinX + 1;
				case 13 -> customMaxZ - customMinZ + 1;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return QuarryMenu.DATA_COUNT;
		}
	};

	public QuarryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.QUARRY.get(), pos, state, BUFFER_SLOTS + 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(BUFFER_SLOTS); // slots 9 and 10: Speed and Efficiency upgrades
		voidFilter.addListener(c -> setChanged());
	}

	public int radius() {
		return radius < 0 ? Config.get(Config.QUARRY_RADIUS) : radius;
	}

	private int effectiveMinY() {
		return level == null ? minY : Math.max(level.getMinBuildHeight(), minY);
	}

	public net.minecraft.world.SimpleContainer getVoidFilter() {
		return voidFilter;
	}

	/** Buttons in the Quarry GUI (ids 2-10). */
	public void handleButton(int id) {
		int floor = level == null ? -64 : level.getMinBuildHeight();
		switch (id) {
			case 2 -> { radius = Math.max(1, radius() - 1); restart(); }
			case 3 -> { radius = Math.min(MAX_RADIUS, radius() + 1); restart(); }
			case 4 -> { if (custom) custom = false; else behind = !behind; restart(); } // leaves a custom area
			case 5 -> { minY = Math.max(floor, effectiveMinY() - 1); restart(); }
			case 6 -> { minY = Math.min(worldPosition.getY() - 1, effectiveMinY() + 1); restart(); }
			case 7 -> { minY = Math.max(floor, effectiveMinY() - 10); restart(); }
			case 8 -> { minY = Math.min(worldPosition.getY() - 1, effectiveMinY() + 10); restart(); }
			case 9 -> toolMode = (toolMode + 1) % TOOL_MODES.length;
			case 10 -> restart();
			default -> { }
		}
		setChanged();
	}

	/** From a Quarry Planner: mine exactly this rectangle, starting at the higher corner's level. */
	public boolean setCustomArea(BlockPos a, BlockPos b) {
		int w = Math.abs(a.getX() - b.getX()) + 1, l = Math.abs(a.getZ() - b.getZ()) + 1;
		if (w > MAX_CUSTOM_SIZE || l > MAX_CUSTOM_SIZE) return false;
		custom = true;
		customMinX = Math.min(a.getX(), b.getX());
		customMaxX = Math.max(a.getX(), b.getX());
		customMinZ = Math.min(a.getZ(), b.getZ());
		customMaxZ = Math.max(a.getZ(), b.getZ());
		customTopY = Math.max(a.getY(), b.getY());
		restart();
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		if (be.preTick(level, pos, state)) return;
		int r = be.radius();
		if (!be.started) {
			be.started = true;
			if (be.behind) {
				Direction back = state.getValue(MachineBlock.FACING).getOpposite();
				be.centreX = pos.getX() + back.getStepX() * (r + 1);
				be.centreZ = pos.getZ() + back.getStepZ() * (r + 1);
				be.topY = pos.getY();
			} else {
				be.centreX = pos.getX();
				be.centreZ = pos.getZ();
				be.topY = pos.getY() - 1;
			}
			be.areaMinX = be.centreX - r;
			be.areaMaxX = be.centreX + r;
			be.areaMinZ = be.centreZ - r;
			be.areaMaxZ = be.centreZ + r;
			if (be.custom) {
				be.areaMinX = be.customMinX;
				be.areaMaxX = be.customMaxX;
				be.areaMinZ = be.customMinZ;
				be.areaMaxZ = be.customMaxZ;
				be.topY = be.customTopY;
			}
			be.cursorX = be.areaMinX;
			be.cursorZ = be.areaMinZ;
			be.cursorY = be.topY;
		}

		// Empty the buffer into neighbouring inventories.
		be.stuck = false;
		for (int i = 0; i < BUFFER_SLOTS; i++) {
			ItemStack stack = be.items.get(i);
			if (stack.isEmpty()) continue;
			for (Direction dir : Direction.values()) { // only through faces set to Out in the Sides window
				if (stack.isEmpty()) break;
				if (!com.robvanblerk.tieredpower.energy.SideConfig.canOutput(be.sideMode(dir))) continue;
				var target = ItemUtil.neighbour(level, pos, dir);
				if (target != null) stack = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(target, stack, false);
			}
			be.items.set(i, stack);
		}
		boolean bufferHasRoom = be.items.subList(0, BUFFER_SLOTS).stream().anyMatch(ItemStack::isEmpty);

		int cost = be.energyCost(ENERGY_PER_TICK) * (be.toolMode > 0 ? 3 : 2) / 2; // enchanted modes cost 50% more
		boolean working = !be.finished && bufferHasRoom && be.energy.getEnergyStored() >= cost;
		if (!bufferHasRoom) be.stuck = true;

		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS_PER_BLOCK * 100) {
				be.progress = 0;
				be.mineNext(server, r);
			}
			be.setChanged();
		}
		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	/** Advances the cursor to the next minable block (checking a limited number per tick) and mines it. */
	private void mineNext(ServerLevel level, int r) {
		for (int checked = 0; checked < SCAN_PER_TICK; checked++) {
			if (cursorY < effectiveMinY()) {
				finished = true;
				return;
			}
			BlockPos target = new BlockPos(cursorX, cursorY, cursorZ);
			if (!level.isLoaded(target)) return; // wait for the chunk (a Chunk Loader helps)
			advance(r);
			BlockState state = level.getBlockState(target);
			if (state.isAir() || !state.getFluidState().isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) continue;
			if (state.getDestroySpeed(level, target) < 0 || state.hasBlockEntity()) continue;
			if (target.equals(worldPosition)) continue;

			List<ItemStack> drops = Block.getDrops(state, level, target, null, null, tool(toolMode));
			level.levelEvent(2001, target, Block.getId(state)); // break particles and sound
			level.setBlock(target, state.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
			for (ItemStack drop : drops) if (!isVoided(drop)) storeDrop(drop);
			mined++;
			return;
		}
	}

	private void advance(int r) {
		cursorX++;
		if (cursorX > areaMaxX) {
			cursorX = areaMinX;
			cursorZ++;
			if (cursorZ > areaMaxZ) {
				cursorZ = areaMinZ;
				cursorY--;
			}
		}
	}

	private boolean isVoided(ItemStack drop) {
		for (int i = 0; i < voidFilter.getContainerSize(); i++) {
			if (!voidFilter.getItem(i).isEmpty() && voidFilter.getItem(i).is(drop.getItem())) return true;
		}
		return false;
	}

	private void storeDrop(ItemStack drop) {
		ItemStack remaining = drop;
		for (int i = 0; i < BUFFER_SLOTS && !remaining.isEmpty(); i++) {
			ItemStack slot = items.get(i);
			if (slot.isEmpty()) {
				items.set(i, remaining);
				remaining = ItemStack.EMPTY;
			} else if (ItemStack.isSameItemSameTags(slot, remaining)) {
				int move = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(move);
				remaining.shrink(move);
			}
		}
		// If it didn't fit it's lost - the quarry only mines while there's at least one empty buffer slot, so this is rare.
	}

	/** Start again from the top (e.g. after changing the config radius). */
	public void restart() {
		started = false;
		finished = false;
		mined = 0;
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("started", started);
		tag.putBoolean("finished", finished);
		tag.putInt("cx", cursorX);
		tag.putInt("cy", cursorY);
		tag.putInt("cz", cursorZ);
		tag.putInt("mined", mined);
		tag.putInt("radius", radius);
		tag.putBoolean("behind", behind);
		tag.putInt("minY", minY);
		tag.putInt("toolMode", toolMode);
		tag.putInt("centreX", centreX);
		tag.putInt("centreZ", centreZ);
		tag.putInt("topY", topY);
		tag.putBoolean("custom", custom);
		tag.putIntArray("customArea", new int[]{customMinX, customMaxX, customMinZ, customMaxZ, customTopY});
		tag.putIntArray("area", new int[]{areaMinX, areaMaxX, areaMinZ, areaMaxZ});
		tag.put("voidFilter", voidFilter.createTag());
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		started = tag.getBoolean("started");
		finished = tag.getBoolean("finished");
		cursorX = tag.getInt("cx");
		cursorY = tag.getInt("cy");
		cursorZ = tag.getInt("cz");
		mined = tag.getInt("mined");
		radius = tag.contains("radius") ? tag.getInt("radius") : -1;
		behind = tag.getBoolean("behind");
		minY = tag.contains("minY") ? tag.getInt("minY") : Integer.MIN_VALUE;
		toolMode = tag.getInt("toolMode");
		centreX = tag.getInt("centreX");
		centreZ = tag.getInt("centreZ");
		topY = tag.getInt("topY");
		custom = tag.getBoolean("custom");
		int[] c = tag.getIntArray("customArea");
		if (c.length == 5) { customMinX = c[0]; customMaxX = c[1]; customMinZ = c[2]; customMaxZ = c[3]; customTopY = c[4]; }
		int[] a = tag.getIntArray("area");
		if (a.length == 4) { areaMinX = a[0]; areaMaxX = a[1]; areaMinZ = a[2]; areaMaxZ = a[3]; }
		else if (started) { int r = radius(); areaMinX = centreX - r; areaMaxX = centreX + r; areaMinZ = centreZ - r; areaMaxZ = centreZ + r; } // older saves
		voidFilter.fromTag(tag.getList("voidFilter", 10));
		if (!tag.contains("centreX")) started = false; // quarries from before 0.16 restart with the new settings
	}

	// Hoppers/pipes can pull from the buffer on any side.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot < BUFFER_SLOTS;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.quarry");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new QuarryMenu(containerId, inventory, this, data, voidFilter);
	}

	/** Needs its bottom for its work area, so it takes power from any side. */
	@Override
	protected boolean powerFromBottomOnly() {
		return false;
	}
}
