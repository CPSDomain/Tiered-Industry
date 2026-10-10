package com.robvanblerk.tieredpower.spatial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Spatial Projector: with a Spatial Cell in it, captures the cube of blocks above it into the cell (blocks, chests and
 * machines with everything inside them), leaving air - or puts a captured space back down into empty space. The cube
 * sits on top of the projector, centred over it. Costs power per block moved.
 */
public class SpatialProjectorBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 1_000_000, MAX_INPUT = 20_000, FE_PER_BLOCK = 40;
	public static final int[] SIZES = {0, 3, 7, 15};

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> size();
				case 3 -> SpatialCellItem.isFull(items.get(0)) ? 1 : 0;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return SpatialProjectorMenu.DATA_COUNT; }
	};

	public SpatialProjectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SPATIAL_PROJECTOR.get(), pos, state, 1, CAPACITY, MAX_INPUT, 0);
	}

	/** Width of the cube for the cell in the slot (0 if none). */
	public int size() {
		return items.get(0).getItem() instanceof SpatialCellItem cell ? cell.size() : 0;
	}

	/** Lowest corner of the cube: on top of the projector, centred over it. */
	public BlockPos origin() {
		int h = (size() - 1) / 2;
		return worldPosition.above().offset(-h, 0, -h);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SpatialProjectorBlockEntity be) {
		be.preTick(level, pos, state);
		int sizeIndex = switch (be.size()) { case 3 -> 1; case 7 -> 2; case 15 -> 3; default -> 0; };
		if (state.getValue(SpatialProjectorBlock.SIZE) != sizeIndex)
			level.setBlock(pos, state.setValue(SpatialProjectorBlock.SIZE, sizeIndex), Block.UPDATE_ALL);
	}

	/** Capture if the cell is empty, deploy if it's full. Tells the player how it went. */
	public void activate(Player player) {
		if (!(level instanceof ServerLevel server)) return;
		ItemStack cell = items.get(0);
		Component result;
		if (!(cell.getItem() instanceof SpatialCellItem)) result = Component.literal("Put a Spatial Cell in first").withStyle(ChatFormatting.RED);
		else if (redstoneBlocked(server, worldPosition, getBlockState())) result = Component.literal("Switched off by redstone").withStyle(ChatFormatting.RED);
		else result = SpatialCellItem.isFull(cell) ? deploy(server, cell) : capture(server, cell);
		player.displayClientMessage(result, false);
		setChanged();
	}

	private Component capture(ServerLevel level, ItemStack cell) {
		int n = size();
		BlockPos min = origin();
		Map<BlockState, Integer> index = new HashMap<>();
		ListTag palette = new ListTag();
		int[] ids = new int[n * n * n];
		ListTag entities = new ListTag();
		int solid = 0;
		for (int y = 0; y < n; y++) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
			BlockPos p = min.offset(x, y, z);
			BlockState s = level.getBlockState(p);
			if (!s.isAir() && s.getDestroySpeed(level, p) < 0)
				return Component.literal("Can't capture " + s.getBlock().getName().getString() + " at " + p.getX() + ", " + p.getY() + ", " + p.getZ()).withStyle(ChatFormatting.RED);
			if (!s.isAir()) solid++;
			Integer id = index.get(s);
			if (id == null) {
				id = palette.size();
				index.put(s, id);
				palette.add(NbtUtils.writeBlockState(s));
			}
			ids[(y * n + z) * n + x] = id;
		}
		if (solid == 0) return Component.literal("Nothing to capture - the area above is empty").withStyle(ChatFormatting.YELLOW);
		int cost = com.robvanblerk.tieredpower.Config.use(solid * FE_PER_BLOCK);
		if (energy.getEnergyStored() < cost) return Component.literal("Needs " + cost + " FE (" + FE_PER_BLOCK + " per block)").withStyle(ChatFormatting.RED);

		// Save block entities, then remove everything top-down without drops or neighbour updates.
		for (int y = 0; y < n; y++) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
			BlockPos p = min.offset(x, y, z);
			BlockEntity be = level.getBlockEntity(p);
			if (be == null) continue;
			CompoundTag t = be.saveWithoutMetadata();
			t.putInt("rx", x); t.putInt("ry", y); t.putInt("rz", z);
			entities.add(t);
		}
		for (int y = n - 1; y >= 0; y--) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
			BlockPos p = min.offset(x, y, z);
			if (level.getBlockState(p).isAir()) continue;
			BlockEntity be = level.getBlockEntity(p);
			if (be != null) {
				Clearable.tryClear(be);
				level.removeBlockEntity(p);
			}
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
		}
		for (int y = 0; y < n; y++) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
			BlockPos p = min.offset(x, y, z);
			level.updateNeighborsAt(p, Blocks.AIR);
		}

		CompoundTag space = new CompoundTag();
		space.putInt("size", n);
		space.put("palette", palette);
		space.putIntArray("blocks", ids);
		space.put("entities", entities);
		UUID id = UUID.randomUUID();
		SpatialStorage.get(level.getServer()).put(id, space);
		SpatialCellItem.fill(cell, id, solid);
		energy.removeInternal(cost);
		level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 0.6f);
		return Component.literal("Captured " + solid + " blocks").withStyle(ChatFormatting.GREEN);
	}

	private Component deploy(ServerLevel level, ItemStack cell) {
		UUID id = SpatialCellItem.space(cell);
		SpatialStorage storage = SpatialStorage.get(level.getServer());
		CompoundTag space = id == null ? null : storage.get(id);
		if (space == null) {
			SpatialCellItem.empty(cell);
			return Component.literal("This cell's space is missing - the cell has been emptied").withStyle(ChatFormatting.RED);
		}
		int n = space.getInt("size");
		if (n != size()) return Component.literal("Wrong cell size").withStyle(ChatFormatting.RED);
		BlockPos min = origin();
		ListTag paletteTag = space.getList("palette", Tag.TAG_COMPOUND);
		List<BlockState> palette = new ArrayList<>();
		var blocks = level.holderLookup(Registries.BLOCK);
		for (int i = 0; i < paletteTag.size(); i++) palette.add(NbtUtils.readBlockState(blocks, paletteTag.getCompound(i)));
		int[] ids = space.getIntArray("blocks");
		int solid = 0;
		for (int y = 0; y < n; y++) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
			BlockState want = palette.get(ids[(y * n + z) * n + x]);
			if (want.isAir()) continue;
			solid++;
			BlockPos p = min.offset(x, y, z);
			BlockState here = level.getBlockState(p);
			if (!here.isAir() && !here.canBeReplaced())
				return Component.literal("Blocked by " + here.getBlock().getName().getString() + " at " + p.getX() + ", " + p.getY() + ", " + p.getZ()
						+ " - the area must be empty").withStyle(ChatFormatting.RED);
		}
		int cost = com.robvanblerk.tieredpower.Config.use(solid * FE_PER_BLOCK);
		if (energy.getEnergyStored() < cost) return Component.literal("Needs " + cost + " FE (" + FE_PER_BLOCK + " per block)").withStyle(ChatFormatting.RED);

		// Place everything first (no shape updates, so torches and doors don't pop), then restore contents, then update.
		for (int y = 0; y < n; y++) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
			BlockState want = palette.get(ids[(y * n + z) * n + x]);
			if (want.isAir()) continue;
			level.setBlock(min.offset(x, y, z), want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
		ListTag entities = space.getList("entities", Tag.TAG_COMPOUND);
		for (int i = 0; i < entities.size(); i++) {
			CompoundTag t = entities.getCompound(i).copy();
			BlockPos p = min.offset(t.getInt("rx"), t.getInt("ry"), t.getInt("rz"));
			t.putInt("x", p.getX()); t.putInt("y", p.getY()); t.putInt("z", p.getZ());
			BlockEntity be = level.getBlockEntity(p);
			if (be != null) {
				be.load(t);
				be.setChanged();
				level.sendBlockUpdated(p, be.getBlockState(), be.getBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		for (int y = 0; y < n; y++) for (int z = 0; z < n; z++) for (int x = 0; x < n; x++) {
			BlockPos p = min.offset(x, y, z);
			level.updateNeighborsAt(p, level.getBlockState(p).getBlock());
		}
		storage.remove(id);
		SpatialCellItem.empty(cell);
		energy.removeInternal(cost);
		level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1.2f);
		return Component.literal("Placed " + solid + " blocks").withStyle(ChatFormatting.GREEN);
	}

	@Override
	public net.minecraft.world.phys.AABB getRenderBoundingBox() {
		return new net.minecraft.world.phys.AABB(worldPosition).inflate(8, 0, 8).expandTowards(0, 16, 0);
	}

	// ---- items ----

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return stack.getItem() instanceof SpatialCellItem;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[] {0};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return stack.getItem() instanceof SpatialCellItem && items.get(0).isEmpty();
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.spatial_projector");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new SpatialProjectorMenu(id, inventory, this, data);
	}
}
