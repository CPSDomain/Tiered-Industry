package com.robvanblerk.tieredpower.drone;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Drone Station: launches up to 4 Utility Drones and gives them work. Modes: Harvest (ripe crops around the station,
 * replanted), Collect (dropped items around the station), Fetch (items from a linked inventory into the station) and
 * Deliver (items from the station into a linked inventory). Everything the drones bring back lands in the station's
 * 15-slot buffer, which pipes, hoppers and belts can empty. Uses power for each drone that is out working.
 */
public class DroneStationBlockEntity extends MachineBlockEntity {
	public static final int DRONE_SLOTS = 4, BUFFER_START = 4, BUFFER_SIZE = 15, SIZE = BUFFER_START + BUFFER_SIZE;
	public static final int CAPACITY = 200_000, MAX_INPUT = 4_000, FE_PER_DRONE = 20;
	/** How far out (horizontally) Harvest and Collect drones work, and how far a linked inventory may be. */
	public static final int RADIUS = 12, HEIGHT = 4, LINK_RANGE = 64;
	public static final String[] MODES = {"Harvest", "Collect", "Fetch", "Deliver"};
	public static final int HARVEST = 0, COLLECT = 1, FETCH = 2, DELIVER = 3;

	private int mode;
	private @Nullable BlockPos link;
	private boolean powered;
	private int ticks;
	private final DroneEntity[] drones = new DroneEntity[DRONE_SLOTS];
	private final Deque<BlockPos> ripe = new ArrayDeque<>();
	private final Set<BlockPos> claimedBlocks = new HashSet<>();
	private final Set<UUID> claimedItems = new HashSet<>();
	private String status = "";

	/** Work given to one drone. */
	public record Job(int kind, BlockPos pos, @Nullable UUID item) {}

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> mode;
				case 3 -> busy();
				case 4 -> link == null ? 0 : 1;
				case 5 -> droneItems();
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return DroneStationMenu.DATA_COUNT; }
	};

	public DroneStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DRONE_STATION.get(), pos, state, SIZE, CAPACITY, MAX_INPUT, 0);
	}

	// ---- settings ----

	public int getMode() { return mode; }

	public void cycleMode() {
		mode = (mode + 1) % MODES.length;
		ripe.clear();
		setChanged();
	}

	public @Nullable BlockPos getLink() { return link; }

	public void setLink(@Nullable BlockPos link) {
		this.link = link;
		setChanged();
	}

	public String status() { return status; }

	// ---- drones ----

	private int droneItems() {
		int n = 0;
		for (int i = 0; i < DRONE_SLOTS; i++) if (items.get(i).is(ModBlocks.UTILITY_DRONE.get())) n++;
		return n;
	}

	private int busy() {
		int n = 0;
		for (DroneEntity d : drones) if (d != null && !d.isRemoved() && !d.isHome()) n++;
		return n;
	}

	/** Should the drone for this slot keep working? (False sends it home to be put away.) */
	public boolean wantsDrone(int slot, DroneEntity drone) {
		return slot >= 0 && slot < DRONE_SLOTS && drones[slot] == drone && items.get(slot).is(ModBlocks.UTILITY_DRONE.get())
				&& level != null && !redstoneBlocked(level, worldPosition, getBlockState());
	}

	public void forget(DroneEntity drone) {
		for (int i = 0; i < DRONE_SLOTS; i++) if (drones[i] == drone) drones[i] = null;
	}

	/** Where drone 'slot' parks: hovering over a corner of the station's pad. */
	public Vec3 dock(int slot) {
		double dx = (slot % 2 == 0 ? -0.25 : 0.25), dz = (slot < 2 ? -0.25 : 0.25);
		return Vec3.atCenterOf(worldPosition).add(dx, 0.75, dz);
	}

	/** Stations in Collect mode, so Item Magnets leave their items alone. */
	private static final java.util.Set<DroneStationBlockEntity> COLLECTING = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	/** True if a Drone Station in Collect mode covers this spot. */
	public static boolean collectingNear(Level level, Vec3 at) {
		synchronized (COLLECTING) {
			for (DroneStationBlockEntity s : COLLECTING) {
				if (s.isRemoved() || s.level != level || s.droneItems() == 0) continue;
				BlockPos p = s.worldPosition;
				if (Math.abs(at.x - p.getX() - 0.5) <= RADIUS + 0.5 && Math.abs(at.z - p.getZ() - 0.5) <= RADIUS + 0.5 && Math.abs(at.y - p.getY()) <= HEIGHT + 1) return true;
			}
		}
		return false;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DroneStationBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		be.ticks++;
		synchronized (COLLECTING) {
			if (be.mode == COLLECT) COLLECTING.add(be);
			else COLLECTING.remove(be);
		}
		boolean paused = be.preTick(level, pos, state);
		int busy = be.busy();
		int cost = be.energyCost(FE_PER_DRONE) * busy;
		be.powered = !paused && be.energy.getEnergyStored() >= Math.max(cost, FE_PER_DRONE);
		if (cost > 0) be.energy.removeInternal(Math.min(cost, be.energy.getEnergyStored()));

		// Launch a drone for each filled slot (they go home and vanish by themselves when the slot is emptied).
		if (!paused) {
			for (int i = 0; i < DRONE_SLOTS; i++) {
				if (!be.items.get(i).is(ModBlocks.UTILITY_DRONE.get())) continue;
				if (be.drones[i] != null && !be.drones[i].isRemoved()) continue;
				DroneEntity d = new DroneEntity(server, pos, i);
				Vec3 dock = be.dock(i);
				d.moveTo(dock.x, dock.y, dock.z, 0, 0);
				server.addFreshEntity(d);
				be.drones[i] = d;
			}
		}
		if (be.mode == HARVEST && be.ticks % 40 == 0) be.scanCrops(server);
		if (be.ticks % 200 == 0) { be.claimedBlocks.clear(); be.claimedItems.clear(); } // forget stale claims now and then
		be.status = be.makeStatus();

		boolean lit = busy > 0;
		if (state.getValue(MachineBlock.LIT) != lit) level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
		be.setChanged();
	}

	private String makeStatus() {
		if (droneItems() == 0) return "Put Utility Drones in the drone slots";
		if (!powered) return "Needs power";
		if ((mode == FETCH || mode == DELIVER) && link == null) return "Link an inventory with a Drone Remote";
		return busy() + " of " + droneItems() + " drones working";
	}

	// ---- jobs ----

	/** The next piece of work for a drone at home, or null. For Deliver, fills the drone's cargo from the buffer. */
	public @Nullable Job nextJob(DroneEntity drone, List<ItemStack> cargo) {
		if (!powered || level == null) return null;
		switch (mode) {
			case HARVEST -> {
				while (!ripe.isEmpty()) {
					BlockPos p = ripe.poll();
					if (claimedBlocks.contains(p) || !isRipe(level, p, level.getBlockState(p))) continue;
					claimedBlocks.add(p);
					return new Job(HARVEST, p, null);
				}
			}
			case COLLECT -> {
				if (!hasRoom()) return null;
				AABB box = new AABB(worldPosition).inflate(RADIUS, HEIGHT, RADIUS);
				ItemEntity best = null;
				double bestD = Double.MAX_VALUE;
				for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, box)) {
					if (!e.isAlive() || claimedItems.contains(e.getUUID()) || e.getItem().is(ModBlocks.UTILITY_DRONE.get())) continue;
					double d = e.distanceToSqr(Vec3.atCenterOf(worldPosition));
					if (d < 2.5) continue; // sitting on the station
					if (d < bestD) { bestD = d; best = e; }
				}
				if (best != null) {
					claimedItems.add(best.getUUID());
					return new Job(COLLECT, best.blockPosition(), best.getUUID());
				}
			}
			case FETCH -> {
				if (link != null && linkHandler() != null && hasRoom() && anyExtractable(linkHandler())) return new Job(FETCH, link, null);
			}
			case DELIVER -> {
				if (link == null || linkHandler() == null) return null;
				for (int i = BUFFER_START; i < SIZE; i++) {
					ItemStack s = items.get(i);
					if (s.isEmpty()) continue;
					// Only send what the target can take right now.
					ItemStack fits = s.copy();
					ItemStack left = ItemHandlerHelper.insertItemStacked(linkHandler(), fits, true);
					int n = s.getCount() - left.getCount();
					if (n <= 0) continue;
					cargo.add(s.split(n));
					setChanged();
					return new Job(DELIVER, link, null);
				}
			}
			default -> {}
		}
		return null;
	}

	public void release(Job job) {
		if (job == null) return;
		claimedBlocks.remove(job.pos());
		if (job.item() != null) claimedItems.remove(job.item());
	}

	public @Nullable IItemHandler linkHandler() {
		if (link == null || level == null || !level.isLoaded(link)) return null;
		var be = level.getBlockEntity(link);
		if (be == null || be == this) return null;
		IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
		return h != null ? h : be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
	}

	private static boolean anyExtractable(IItemHandler h) {
		for (int i = 0; i < h.getSlots(); i++) if (!h.extractItem(i, 1, true).isEmpty()) return true;
		return false;
	}

	private boolean hasRoom() {
		for (int i = BUFFER_START; i < SIZE; i++) if (items.get(i).isEmpty()) return true;
		return false;
	}

	/** Puts a drone's cargo into the buffer; returns what didn't fit. */
	public List<ItemStack> unload(List<ItemStack> cargo) {
		List<ItemStack> left = new ArrayList<>();
		for (ItemStack c : cargo) {
			ItemStack rest = c.copy();
			for (int i = BUFFER_START; i < SIZE && !rest.isEmpty(); i++) {
				ItemStack slot = items.get(i);
				if (slot.isEmpty()) { items.set(i, rest); rest = ItemStack.EMPTY; }
				else if (ItemStack.isSameItemSameTags(slot, rest)) {
					int move = Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount());
					slot.grow(move);
					rest.shrink(move);
				}
			}
			if (!rest.isEmpty()) left.add(rest);
		}
		setChanged();
		return left;
	}

	// ---- crops ----

	private void scanCrops(ServerLevel level) {
		ripe.clear();
		for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RADIUS, -HEIGHT, -RADIUS), worldPosition.offset(RADIUS, HEIGHT, RADIUS))) {
			if (!level.isLoaded(p)) continue;
			if (isRipe(level, p, level.getBlockState(p)) && !claimedBlocks.contains(p)) ripe.add(p.immutable());
			if (ripe.size() >= 64) break;
		}
	}

	public static boolean isRipe(Level level, BlockPos p, BlockState s) {
		Block b = s.getBlock();
		if (b instanceof CropBlock crop) return crop.isMaxAge(s);
		if (b instanceof NetherWartBlock) return s.getValue(NetherWartBlock.AGE) >= 3;
		if (b instanceof CocoaBlock) return s.getValue(CocoaBlock.AGE) >= 2;
		if (b == Blocks.MELON || b == Blocks.PUMPKIN) return true;
		if (b == Blocks.SUGAR_CANE) return level.getBlockState(p.below()).is(Blocks.SUGAR_CANE) && !level.getBlockState(p.below(2)).is(Blocks.SUGAR_CANE);
		return false;
	}

	/** Harvests (and replants) a ripe crop; returns the drops. */
	public static List<ItemStack> harvest(ServerLevel level, BlockPos p) {
		BlockState s = level.getBlockState(p);
		if (!isRipe(level, p, s)) return List.of();
		Block b = s.getBlock();
		List<ItemStack> drops = new ArrayList<>();
		if (b == Blocks.SUGAR_CANE) {
			// cut everything from here up, leave the bottom block
			for (BlockPos up = p; level.getBlockState(up).is(Blocks.SUGAR_CANE); up = up.above()) drops.addAll(Block.getDrops(level.getBlockState(up), level, up, null));
			for (BlockPos up = p; level.getBlockState(up).is(Blocks.SUGAR_CANE); up = up.above()) level.setBlock(up, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			level.levelEvent(2001, p, Block.getId(s));
			return drops;
		}
		BlockState replant = b instanceof CropBlock crop ? crop.getStateForAge(0)
				: b instanceof NetherWartBlock ? s.setValue(NetherWartBlock.AGE, 0)
				: b instanceof CocoaBlock ? s.setValue(CocoaBlock.AGE, 0)
				: Blocks.AIR.defaultBlockState();
		drops.addAll(Block.getDrops(s, level, p, null));
		level.levelEvent(2001, p, Block.getId(s));
		level.setBlock(p, replant, Block.UPDATE_ALL);
		return drops;
	}

	// ---- when the station goes away ----

	/** Brings every drone in. Their cargo goes into the buffer, or onto the ground if the station is being broken. */
	private void recallAll(boolean broken) {
		for (int i = 0; i < DRONE_SLOTS; i++) {
			DroneEntity d = drones[i];
			if (d == null) continue;
			List<ItemStack> cargo = d.takeCargo();
			List<ItemStack> left = broken ? cargo : unload(cargo);
			if (level != null) for (ItemStack s : left) Block.popResource(level, worldPosition, s);
			d.discard();
			drones[i] = null;
		}
	}

	@Override
	public void setRemoved() {
		boolean broken = level != null && !(level.getBlockState(worldPosition).getBlock() instanceof DroneStationBlock);
		recallAll(broken);
		super.setRemoved();
	}

	@Override
	public void onChunkUnloaded() {
		recallAll(false);
		super.onChunkUnloaded();
	}

	// ---- items ----

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot < DRONE_SLOTS ? stack.is(ModBlocks.UTILITY_DRONE.get()) : true;
	}

	private static final int[] BUFFER_SLOTS = java.util.stream.IntStream.range(BUFFER_START, SIZE).toArray();

	@Override
	public int[] getSlotsForFace(Direction side) {
		return BUFFER_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot >= BUFFER_START;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot >= BUFFER_START;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("mode", mode);
		if (link != null) tag.putLong("link", link.asLong());
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		mode = tag.getInt("mode");
		link = tag.contains("link") ? BlockPos.of(tag.getLong("link")) : null;
	}

	/** Lines for Jade. */
	public List<String> info() {
		List<String> out = new ArrayList<>();
		out.add("Mode: " + MODES[mode]);
		if (link != null && (mode == FETCH || mode == DELIVER)) out.add("Linked to " + link.getX() + ", " + link.getY() + ", " + link.getZ());
		String s = makeStatus();
		out.add(s.startsWith("Needs") || s.startsWith("Put") || s.startsWith("Link") ? "!" + s : s);
		return out;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.drone_station");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new DroneStationMenu(id, inventory, this, data);
	}
}
