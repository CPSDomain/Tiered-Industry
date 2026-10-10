package com.robvanblerk.tieredpower.block.entity;

import java.util.List;

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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.WirelessChargerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Charges every Forge Energy item carried or worn by players within 16 blocks. */
public class WirelessChargerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 2_000_000;
	public static final int MAX_INPUT = 64_000;
	public static final int RATE_PER_PLAYER = 16_000;
	public static final int RANGE = 16;

	private int playersInRange;
	private int lastRate;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> playersInRange;
				case 3 -> lastRate & 0xFFFF;
				case 4 -> (lastRate >>> 16) & 0xFFFF;
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return WirelessChargerMenu.DATA_COUNT;
		}
	};

	public WirelessChargerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIRELESS_CHARGER.get(), pos, state, 0, CAPACITY, MAX_INPUT, 0);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, WirelessChargerBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		be.lastRate = 0;
		if (level.getGameTime() % 5 == 0) {
			List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(RANGE));
			be.playersInRange = players.size();
			int before = be.energy.getEnergyStored();
			for (Player player : players) {
				int budget = Math.min(be.energy.getEnergyStored(), RATE_PER_PLAYER * 5);
				for (ItemStack stack : player.getInventory().items) budget = be.charge(stack, budget);
				for (ItemStack stack : player.getInventory().armor) budget = be.charge(stack, budget);
				for (ItemStack stack : player.getInventory().offhand) budget = be.charge(stack, budget);
			}
			int moved = before - be.energy.getEnergyStored();
			be.lastRate = moved / 5;
			be.setChanged();
			boolean working = moved > 0;
			if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	private int charge(ItemStack stack, int budget) {
		if (budget <= 0 || stack.isEmpty()) return budget;
		IEnergyStorage target = ChargerBlockEntity.energyOf(stack);
		if (target == null || !target.canReceive()) return budget;
		int given = target.receiveEnergy(budget, false);
		energy.removeInternal(given);
		return budget - given;
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
		return Component.translatable("block.tieredpower.wireless_charger");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new WirelessChargerMenu(containerId, inventory, data);
	}
}
