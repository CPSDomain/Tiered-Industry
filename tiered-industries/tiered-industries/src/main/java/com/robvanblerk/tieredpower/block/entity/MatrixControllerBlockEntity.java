package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.matrix.MatrixPartBlock;
import com.robvanblerk.tieredpower.menu.MatrixControllerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.storage.CraftingPattern;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * The Assembly Matrix: a box (3x3x3 up to 7x7x7) of Matrix Casing / Matrix Glass with this controller in a wall.
 * Pattern Banks (27 patterns each) and Crafting Accelerators can take the place of any wall block, or go inside. Its crafting patterns are crafted inside
 * the matrix (2 crafts per cycle, +2 per accelerator); its processing patterns run on any machine of the right type that
 * has a Molecular Assembler or Machine Connector touching it, anywhere on the storage network.
 */
public class MatrixControllerBlockEntity extends BlockEntity implements MenuProvider {
	public static final int MIN = 3, MAX = 7;
	public static final int OK = 0, PROBLEM_SIZE = 1, PROBLEM_WALL = 2, PROBLEM_INSIDE = 3, PROBLEM_CONTROLLERS = 4, PROBLEM_NOT_CHECKED = 5;

	private boolean formed;
	private int problem = PROBLEM_NOT_CHECKED, accelerators, acceleratorCrafts, overclocks, ticks, page;
	private final List<BlockPos> banks = new ArrayList<>();
	private @Nullable BlockPos problemPos;

	public MatrixControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MATRIX_CONTROLLER.get(), pos, state);
	}

	public boolean isFormed() { return formed; }
	public int getProblem() { return problem; }
	public @Nullable BlockPos getProblemPos() { return problemPos; }
	public int getAccelerators() { return accelerators; }
	public int bankCount() { return banks.size(); }

	/** Crafts per cycle for its crafting patterns. */
	/** Extra Speed-upgrade levels given to machines the Matrix crafts with: 2 per Overclock Accelerator, up to 8. */
	public int overclockLevels() {
		return formed ? Math.min(MAX_OVERCLOCK, overclocks * 2) : 0;
	}

	public static final int MAX_OVERCLOCK = 8;

	public int craftsPerCycle() {
		return formed ? Math.min(com.robvanblerk.tieredpower.storage.CraftingTier.MATRIX_MAX_CRAFTS, 2 + acceleratorCrafts) : 0;
	}

	public List<CraftingPattern> patterns() {
		List<CraftingPattern> out = new ArrayList<>();
		if (!formed || level == null) return out;
		for (BlockPos p : banks) if (level.getBlockEntity(p) instanceof PatternBankBlockEntity b) out.addAll(b.patterns());
		return out;
	}

	private static boolean isPart(Block b) {
		return b == ModBlocks.MATRIX_CASING.get() || b == ModBlocks.MATRIX_GLASS.get() || b == ModBlocks.MATRIX_CONTROLLER.get()
				|| b == ModBlocks.PATTERN_BANK.get() || b == ModBlocks.CRAFTING_ACCELERATOR.get() || b == ModBlocks.OVERCLOCK_ACCELERATOR.get();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MatrixControllerBlockEntity be) {
		if (++be.ticks % 40 != 1) return;
		boolean was = be.formed;
		int banksBefore = be.banks.size();
		be.check(level, pos);
		if (was != be.formed || banksBefore != be.banks.size()) {
			be.setChanged();
			StorageNetwork.changed();
		}
	}

	/** Works out the box from the connected parts and checks every block of it. */
	private void check(Level level, BlockPos origin) {
		formed = false;
		banks.clear();
		accelerators = 0;
		acceleratorCrafts = 0;
		overclocks = 0;
		problemPos = null;
		// Flood through connected matrix parts to find the box.
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		int minX = origin.getX(), minY = origin.getY(), minZ = origin.getZ(), maxX = minX, maxY = minY, maxZ = minZ;
		while (!queue.isEmpty() && seen.size() < 400) {
			BlockPos p = queue.poll();
			if (!seen.add(p) || !level.isLoaded(p) || !isPart(level.getBlockState(p).getBlock())) continue;
			minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
			maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
			for (Direction d : Direction.values()) queue.add(p.relative(d));
		}
		int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
		if (sx < MIN || sy < MIN || sz < MIN || sx > MAX || sy > MAX || sz > MAX) {
			problem = PROBLEM_SIZE;
			return;
		}
		int controllers = 0;
		for (int x = minX; x <= maxX; x++)
			for (int y = minY; y <= maxY; y++)
				for (int z = minZ; z <= maxZ; z++) {
					BlockPos p = new BlockPos(x, y, z);
					Block b = level.getBlockState(p).getBlock();
					boolean shell = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
					if (b == ModBlocks.PATTERN_BANK.get()) { // walls or inside
						banks.add(p);
					} else if (b == ModBlocks.OVERCLOCK_ACCELERATOR.get()) {
						accelerators++;
						overclocks++;
						acceleratorCrafts += com.robvanblerk.tieredpower.storage.CraftingTier.ACCELERATOR_CRAFTS[com.robvanblerk.tieredpower.storage.CraftingTier.MAX];
					} else if (b == ModBlocks.CRAFTING_ACCELERATOR.get()) {
						accelerators++;
						acceleratorCrafts += com.robvanblerk.tieredpower.storage.CraftingTier.ACCELERATOR_CRAFTS[com.robvanblerk.tieredpower.storage.CraftingTier.of(level.getBlockState(p))];
					} else if (shell) {
						if (b == ModBlocks.MATRIX_CONTROLLER.get()) controllers++;
						else if (b != ModBlocks.MATRIX_CASING.get() && b != ModBlocks.MATRIX_GLASS.get()) {
							problem = PROBLEM_WALL;
							problemPos = p;
							banks.clear();
							return;
						}
					} else if (b == ModBlocks.PATTERN_BANK.get()) {
						banks.add(p);
					} else if (b == ModBlocks.OVERCLOCK_ACCELERATOR.get()) {
						accelerators++;
						overclocks++;
						acceleratorCrafts += com.robvanblerk.tieredpower.storage.CraftingTier.ACCELERATOR_CRAFTS[com.robvanblerk.tieredpower.storage.CraftingTier.MAX];
					} else if (b == ModBlocks.CRAFTING_ACCELERATOR.get()) {
						accelerators++;
						acceleratorCrafts += com.robvanblerk.tieredpower.storage.CraftingTier.ACCELERATOR_CRAFTS[com.robvanblerk.tieredpower.storage.CraftingTier.of(level.getBlockState(p))];
					} else if (!level.getBlockState(p).isAir() && !level.getBlockState(p).canBeReplaced()) { // grass, snow layers etc. don't count
						problem = PROBLEM_INSIDE;
						problemPos = p;
						banks.clear();
						return;
					}
				}
		if (controllers != 1) {
			problem = PROBLEM_CONTROLLERS;
			banks.clear();
			return;
		}
		formed = true;
		problem = OK;
		page = Math.min(page, Math.max(0, banks.size() - 1));
	}

	/**
	 * The machines the Matrix can use: everything touching a Molecular Assembler or Machine Connector on the network,
	 * counted by name ("Pulverizer x2"). For the controller's screen.
	 */
	public List<String> connectedMachines() {
		List<String> out = new ArrayList<>();
		if (level == null) return out;
		StorageControllerBlockEntity c = StorageNetwork.findController(level, worldPosition);
		if (c == null || c.getNetwork() == null) {
			out.add("(not on a powered storage network)");
			return out;
		}
		java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
		List<BlockPos> ifaces = new ArrayList<>(c.getNetwork().assemblers());
		ifaces.addAll(c.getNetwork().connectors());
		ifaces.addAll(c.getNetwork().buses());
		Set<BlockPos> counted = new HashSet<>();
		List<String> idle = new ArrayList<>();
		for (BlockPos f : ifaces) {
			boolean any = false;
			for (BlockPos n : MachineConnectorBlockEntityHelper.touching(level, f)) {
				any = true;
				if (counted.add(n)) counts.merge(level.getBlockState(n).getBlock().getName().getString(), 1, Integer::sum);
			}
			if (!any && level.getBlockState(f).getBlock() instanceof com.robvanblerk.tieredpower.block.MachineConnectorBlock)
				idle.add("! Connector at " + f.getX() + " " + f.getY() + " " + f.getZ() + ": no machine");
		}
		if (c.getNetwork().truncated()) out.add("! Network too big - some parts missed");
		for (var e : counts.entrySet()) out.add(e.getKey() + (e.getValue() > 1 ? " x" + e.getValue() : ""));
		out.addAll(idle);
		if (out.isEmpty()) out.add("None yet - put a Molecular Assembler or Machine Connector on a machine");
		return out;
	}

	// ---- the screen: one Pattern Bank (27 slots) per page ----

	public int getPage() { return page; }

	public void turnPage(int delta) {
		if (banks.isEmpty()) return;
		page = Math.floorMod(page + delta, banks.size());
	}

	private @Nullable Container currentBank() {
		if (!formed || level == null || banks.isEmpty()) return null;
		return level.getBlockEntity(banks.get(Math.min(page, banks.size() - 1))) instanceof PatternBankBlockEntity b ? b.container() : null;
	}

	/** The slots the screen shows: whichever bank is on the current page (empty while the matrix isn't formed). */
	public final Container pageView = new Container() {
		@Override public int getContainerSize() { return PatternBankBlockEntity.SLOTS; }
		@Override public boolean isEmpty() { Container c = currentBank(); return c == null || c.isEmpty(); }
		@Override public ItemStack getItem(int i) { Container c = currentBank(); return c == null ? ItemStack.EMPTY : c.getItem(i); }
		@Override public ItemStack removeItem(int i, int n) { Container c = currentBank(); return c == null ? ItemStack.EMPTY : c.removeItem(i, n); }
		@Override public ItemStack removeItemNoUpdate(int i) { Container c = currentBank(); return c == null ? ItemStack.EMPTY : c.removeItemNoUpdate(i); }
		@Override public void setItem(int i, ItemStack s) { Container c = currentBank(); if (c != null) c.setItem(i, s); }
		@Override public int getMaxStackSize() { return 1; }
		@Override public void setChanged() { Container c = currentBank(); if (c != null) c.setChanged(); }
		@Override public boolean stillValid(Player p) { return !isRemoved(); }
		@Override public boolean canPlaceItem(int i, ItemStack s) { Container c = currentBank(); return c != null && c.canPlaceItem(i, s); }
		@Override public void clearContent() {}
	};

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> page;
				case 1 -> banks.size();
				case 2 -> formed ? 1 : 0;
				case 3 -> problem;
				case 4 -> patterns().size();
				case 5 -> accelerators;
				case 6 -> craftsPerCycle();
				case 7 -> overclockLevels();
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return MatrixControllerMenu.DATA_COUNT; }
	};

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.matrix_controller");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new MatrixControllerMenu(id, inv, this, pageView, data);
	}
}
