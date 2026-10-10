package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.SpawnerControllerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Place next to a vanilla mob spawner. While powered, the spawner works without a player nearby and spawns much faster
 * (Speed Upgrades make it faster still). Without power (or switched off with redstone) the spawner goes back to its
 * original settings.
 * Slots: 0-1 upgrades.
 */
public class SpawnerControllerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 500_000, MAX_INPUT = 8_000, ENERGY_PER_TICK = 200;

	@Nullable
	private BlockPos spawnerPos;
	@Nullable
	private CompoundTag original; // the spawner's settings before we changed them
	private boolean boosting;
	private int appliedSpeed = -1;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> spawnerPos != null ? 1 : 0;
				case 3 -> boosting ? 1 : 0;
				case 4 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return SpawnerControllerMenu.DATA_COUNT;
		}
	};

	public SpawnerControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SPAWNER_CONTROLLER.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(0);
	}

	private @Nullable SpawnerBlockEntity findSpawner(Level level) {
		for (Direction dir : Direction.values()) {
			BlockPos p = worldPosition.relative(dir);
			if (level.isLoaded(p) && level.getBlockEntity(p) instanceof SpawnerBlockEntity s) {
				spawnerPos = p;
				return s;
			}
		}
		spawnerPos = null;
		return null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SpawnerControllerBlockEntity be) {
		boolean paused = be.preTick(level, pos, state);
		SpawnerBlockEntity spawner = be.findSpawner(level);
		int cost = be.energyCost(ENERGY_PER_TICK);
		boolean shouldBoost = !paused && spawner != null && be.energy.getEnergyStored() >= cost;
		if (shouldBoost) {
			be.energy.removeInternal(cost);
			int speed = be.speedUpgrades();
			if (!be.boosting || be.appliedSpeed != speed) be.apply(level, spawner, speed);
		} else if (be.boosting) {
			be.restore(level, spawner);
		}
		be.setChanged();
		boolean lit = be.boosting;
		BlockState now = level.getBlockState(pos);
		if (now.hasProperty(MachineBlock.LIT) && now.getValue(MachineBlock.LIT) != lit) level.setBlock(pos, now.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
	}

	/** Rewrites the spawner's settings: no player needed, much shorter delays, more mobs per spawn. */
	private void apply(Level level, SpawnerBlockEntity spawner, int speed) {
		CompoundTag tag = spawner.getSpawner().save(new CompoundTag());
		if (original == null) {
			original = new CompoundTag();
			for (String key : new String[]{"MinSpawnDelay", "MaxSpawnDelay", "SpawnCount", "MaxNearbyEntities", "RequiredPlayerRange"}) {
				if (tag.contains(key)) original.putShort(key, tag.getShort(key));
			}
		}
		int max = Math.max(20, 100 - speed * 20);
		tag.putShort("MinSpawnDelay", (short) (max / 2));
		tag.putShort("MaxSpawnDelay", (short) max);
		tag.putShort("SpawnCount", (short) (4 + speed));
		tag.putShort("MaxNearbyEntities", (short) 12);
		tag.putShort("RequiredPlayerRange", (short) -1); // negative = any player in the dimension counts
		if (tag.getShort("Delay") > max) tag.putShort("Delay", (short) max);
		spawner.getSpawner().load(level, spawner.getBlockPos(), tag);
		spawner.setChanged();
		boosting = true;
		appliedSpeed = speed;
	}

	private void restore(Level level, @Nullable SpawnerBlockEntity spawner) {
		if (spawner != null && original != null) {
			CompoundTag tag = spawner.getSpawner().save(new CompoundTag());
			for (String key : original.getAllKeys()) tag.putShort(key, original.getShort(key));
			spawner.getSpawner().load(level, spawner.getBlockPos(), tag);
			spawner.setChanged();
		}
		original = null;
		boosting = false;
		appliedSpeed = -1;
	}

	/** When the controller is broken, put the spawner back to normal. */
	public void releaseSpawner() {
		if (level != null && boosting) restore(level, findSpawner(level));
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("boosting", boosting);
		if (original != null) tag.put("original", original);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		boosting = tag.getBoolean("boosting");
		original = tag.contains("original") ? tag.getCompound("original") : null;
		appliedSpeed = -1;
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
		return Component.translatable("block.tieredpower.spawner_controller");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new SpawnerControllerMenu(containerId, inventory, this, data);
	}
}
