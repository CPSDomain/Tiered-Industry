package com.robvanblerk.tieredpower.block.entity;

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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.world.ForgeChunkManager;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.ChunkLoaderMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Keeps the chunks around it loaded (and ticking) while it has power: a square of chunks chosen in its GUI, from 1x1 up
 * to 5x5 (Basic), 7x7 (Advanced), 9x9 (Elite), 11x11 (Ultimate) or 15x15 (Quantum) with Tier Installers.
 * Costs FE per chunk per tick (config). Releases the chunks when out of power, switched off by redstone, or broken.
 * Slots: 0-1 upgrades (Efficiency lowers the cost).
 */
public class ChunkLoaderBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 500_000;
	public static final int MAX_INPUT = 8_000;

	/** Largest radius per tier (Basic..Quantum): 5x5, 7x7, 9x9, 11x11, 15x15 chunks. */
	public static final int[] MAX_RADIUS = {2, 3, 4, 5, 7, 9};

	private int radius = 1; // chunks out from the centre: 0 = 1x1, 1 = 3x3, 2 = 5x5 ...
	private boolean loading;
	private int loadedRadius = -1;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> radius;
				case 3 -> loading ? 1 : 0;
				case 4 -> cost();
				case 5 -> maxRadius();
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return ChunkLoaderMenu.DATA_COUNT;
		}
	};

	public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CHUNK_LOADER.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(0);
	}

	private int chunkCount() {
		int size = radius * 2 + 1;
		return size * size;
	}

	/** FE per tick at the current size. */
	public int cost() {
		return energyCost(Config.get(Config.CHUNK_LOADER_FE_PER_CHUNK) * chunkCount());
	}

	public int maxRadius() {
		return MAX_RADIUS[Math.min(getTier(), MAX_RADIUS.length - 1)];
	}

	/** Next size up, wrapping back to 1x1 after the largest this tier allows (backwards when 'back'). */
	public void cycleRadius(boolean back) {
		int n = maxRadius() + 1;
		radius = Math.floorMod(Math.min(radius, n - 1) + (back ? -1 : 1), n);
		setChanged();
	}

	@Override
	public boolean supportsTiers() {
		return true;
	}

	@Override
	public String tierEffect(int tier) {
		int size = MAX_RADIUS[Math.min(tier, MAX_RADIUS.length - 1)] * 2 + 1;
		return "up to " + size + "x" + size + " chunks";
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ChunkLoaderBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		boolean allowed = !be.preTick(level, pos, state);
		if (be.radius > be.maxRadius()) be.radius = be.maxRadius();
		int cost = be.cost();
		boolean shouldLoad = allowed && be.energy.getEnergyStored() >= cost;

		if (shouldLoad) {
			be.energy.removeInternal(cost);
			if (!be.loading || be.loadedRadius != be.radius) {
				be.setForced(server, false);
				be.loadedRadius = be.radius;
				be.setForced(server, true);
				be.loading = true;
			}
		} else if (be.loading) {
			be.setForced(server, false);
			be.loading = false;
		}
		be.setChanged();
		if (state.getValue(MachineBlock.LIT) != be.loading) {
			level.setBlock(pos, level.getBlockState(pos).setValue(MachineBlock.LIT, be.loading), Block.UPDATE_ALL);
		}
	}

	private void setForced(ServerLevel level, boolean add) {
		if (loadedRadius < 0) return;
		ChunkPos centre = new ChunkPos(worldPosition);
		for (int dx = -loadedRadius; dx <= loadedRadius; dx++) {
			for (int dz = -loadedRadius; dz <= loadedRadius; dz++) {
				ForgeChunkManager.forceChunk(level, TieredPower.MOD_ID, worldPosition, centre.x + dx, centre.z + dz, add, true);
			}
		}
	}

	/** Called when the block is broken: release everything. */
	public void releaseAll() {
		if (level instanceof ServerLevel server && loading) {
			setForced(server, false);
			loading = false;
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("radius", radius);
		tag.putBoolean("loading", loading);
		tag.putInt("loadedRadius", loadedRadius);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		radius = tag.contains("radius") ? tag.getInt("radius") : 1;
		loading = tag.getBoolean("loading");
		loadedRadius = tag.contains("loadedRadius") ? tag.getInt("loadedRadius") : -1;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.chunk_loader");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new ChunkLoaderMenu(containerId, inventory, this, data);
	}
}
