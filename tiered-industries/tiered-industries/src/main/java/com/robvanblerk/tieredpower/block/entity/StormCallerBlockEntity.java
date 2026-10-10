package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.menu.StormCallerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Starts a thunderstorm: once it holds 4,000,000 FE and a Storm Charge, it uses both to call a 5-minute storm (only
 * where there is weather, and not while it's already storming). Follows its redstone setting, so a lever - or a
 * comparator on a Lightning Collector - can decide when.
 */
public class StormCallerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 4_000_000, MAX_INPUT = 40_000, COST = 4_000_000, STORM_TICKS = 6_000, CHARGE_SLOT = 0;
	public static final int STATUS_READY = 0, STATUS_CHARGING = 1, STATUS_NO_CHARGE = 2, STATUS_STORMING = 3, STATUS_NO_WEATHER = 4, STATUS_OFF = 5;

	private int status = STATUS_CHARGING;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> status;
				default -> 0;
			};
		}

		@Override public void set(int i, int v) {}
		@Override public int getCount() { return StormCallerMenu.DATA_COUNT; }
	};

	public StormCallerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STORM_CALLER.get(), pos, state, 1, CAPACITY, MAX_INPUT, 0);
	}

	public int getStatus() {
		return status;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StormCallerBlockEntity be) {
		if (be.preTick(level, pos, state)) {
			be.status = STATUS_OFF;
			return;
		}
		int cost = Config.use(COST);
		if (be.energy.getMaxEnergyStored() < cost) be.energy.setCapacity(cost); // a higher energy multiplier needs a bigger buffer
		if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) be.status = STATUS_NO_WEATHER;
		else if (level.isThundering()) be.status = STATUS_STORMING;
		else if (!be.items.get(CHARGE_SLOT).is(ModBlocks.STORM_CHARGE.get())) be.status = STATUS_NO_CHARGE;
		else if (be.energy.getEnergyStored() < cost) be.status = STATUS_CHARGING;
		else if (level instanceof ServerLevel server) {
			be.energy.removeInternal(cost);
			be.items.get(CHARGE_SLOT).shrink(1);
			server.setWeatherParameters(0, STORM_TICKS, true, true);
			level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4.0f, 0.8f);
			be.status = STATUS_STORMING;
			be.setChanged();
		}
	}

	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.is(ModBlocks.STORM_CHARGE.get()); }
	@Override public int[] getSlotsForFace(Direction side) { return new int[]{CHARGE_SLOT}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.storm_caller"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new StormCallerMenu(id, inv, this, data);
	}
}
