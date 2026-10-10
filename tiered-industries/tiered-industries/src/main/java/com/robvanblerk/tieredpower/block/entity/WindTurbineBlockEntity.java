package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.menu.WindTurbineMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Makes power from wind. Output grows with height (nothing at sea level, full at Y 256), rain gives x1.5 and thunder x2,
 * and blocks within 3 blocks of it slow it down, so give it open space.
 */
public class WindTurbineBlockEntity extends MachineBlockEntity {
	public static final int MAX_OUTPUT = 128;
	public static final int MIN_Y = 64, FULL_Y = 256;
	public static final int CAPACITY = 100_000;

	private int generating;
	private int clearance = 100; // % of the space around it that's open air
	private int weather;         // 0 clear, 1 rain, 2 thunder

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> generating;
				case 3 -> worldPosition.getY();
				case 4 -> clearance;
				case 5 -> weather;
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return WindTurbineMenu.DATA_COUNT;
		}
	};

	public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIND_TURBINE.get(), pos, state, 0, CAPACITY, 0, MAX_OUTPUT * 4);
	}

	@Override
	public net.minecraft.world.phys.AABB getRenderBoundingBox() {
		return new net.minecraft.world.phys.AABB(worldPosition).inflate(1.6);
	}

	public int getGenerating() {
		return generating;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, WindTurbineBlockEntity be) {
		if (level.getGameTime() % 100 == 0 || be.generating < 0) be.measure(level, pos);
		if (level.getGameTime() % 20 == 0) {
			be.weather = level.isThundering() ? 2 : level.isRaining() ? 1 : 0;
			double height = Math.max(0, Math.min(1, (pos.getY() - MIN_Y) / (double) (FULL_Y - MIN_Y)));
			double weatherBoost = be.weather == 2 ? 2.0 : be.weather == 1 ? 1.5 : 1.0;
			be.generating = level.dimensionType().hasSkyLight() ? (int) Math.round(MAX_OUTPUT * height * weatherBoost * be.clearance / 100.0) : 0;
		}
		if (be.generating > 0) be.energy.addInternal(com.robvanblerk.tieredpower.Config.gen(be.generating));
		for (Direction dir : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), MAX_OUTPUT * 4);
		}
		boolean spinning = be.generating > 0;
		if (state.getValue(MachineBlock.LIT) != spinning) level.setBlock(pos, state.setValue(MachineBlock.LIT, spinning), Block.UPDATE_ALL);
	}

	/** How much of the 7x7x7 space around the turbine is open air. */
	private void measure(Level level, BlockPos pos) {
		int air = 0, total = 0;
		for (int dx = -3; dx <= 3; dx++) {
			for (int dy = -3; dy <= 3; dy++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) continue;
					BlockPos p = pos.offset(dx, dy, dz);
					if (!level.isLoaded(p)) continue;
					total++;
					if (level.getBlockState(p).isAir()) air++;
				}
			}
		}
		clearance = total == 0 ? 100 : air * 100 / total;
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
		return Component.translatable("block.tieredpower.wind_turbine");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new WindTurbineMenu(containerId, inventory, data);
	}
}
