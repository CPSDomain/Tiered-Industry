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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.menu.ChargerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Charges any item that holds Forge Energy, from any mod (tools, armour, jetpacks, batteries...).
 * Fully charged items move to the output slot, so a hopper underneath can collect them.
 */
public class ChargerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 100_000;
	public static final int MAX_INPUT = 8_000;
	public static final int CHARGE_RATE = 8_000;
	public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1;

	private int itemCharge; // % of the item in the input slot, for the GUI
	private int lastRate;   // FE/t going into the item

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> itemCharge;
				case 3 -> lastRate;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return ChargerMenu.DATA_COUNT;
		}
	};

	public ChargerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CHARGER.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
	}

	/**
	 * The energy store of an item: Forge's energy capability (works for any mod's FE items), or - for this mod's own
	 * powered items - their store read directly, in case the capability isn't reachable. Null if it holds no FE.
	 */
	public static @Nullable IEnergyStorage energyOf(ItemStack stack) {
		if (stack.isEmpty()) return null;
		IEnergyStorage cap = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
		if (cap != null) return cap;
		int capacity = com.robvanblerk.tieredpower.item.powered.ItemEnergy.capacityOf(stack);
		return capacity > 0 ? com.robvanblerk.tieredpower.item.powered.ItemEnergy.storage(stack, capacity, capacity / 50, false) : null;
	}

	/** Anything that stores FE and has room for more - empty or part-charged. */
	public static boolean isChargeable(ItemStack stack) {
		IEnergyStorage e = energyOf(stack);
		return e != null && e.getMaxEnergyStored() > 0 && (e.canReceive() || e.getEnergyStored() < e.getMaxEnergyStored());
	}

	/** Why an item can't go in (for the slot's tooltip), or null if it can. */
	public static @Nullable String whyNot(ItemStack stack) {
		if (stack.isEmpty()) return null;
		IEnergyStorage e = energyOf(stack);
		if (e == null) return "This item doesn't store FE, so it can't be charged";
		if (e.getMaxEnergyStored() <= 0) return "This item reports no energy capacity";
		if (!e.canReceive() && e.getEnergyStored() >= e.getMaxEnergyStored()) return "This item is already full";
		if (!e.canReceive()) return "This item says it can't receive energy";
		return null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ChargerBlockEntity be) {
		if (be.preTick(level, pos, state)) return; // paused by redstone control
		ItemStack stack = be.items.get(INPUT_SLOT);
		be.lastRate = 0;
		be.itemCharge = 0;

		IEnergyStorage item = energyOf(stack);
		if (item != null) {
			// Give the item energy straight from our own buffer. (The buffer's public "extract" is closed - like every
			// machine it only takes power in - so a normal transfer would always move 0 FE.)
			int offer = Math.min(CHARGE_RATE, be.energy.getEnergyStored());
			int given = offer > 0 ? item.receiveEnergy(offer, false) : 0;
			if (given > 0) be.energy.removeInternal(given);
			be.lastRate = given;
			int max = item.getMaxEnergyStored();
			be.itemCharge = max <= 0 ? 100 : (int) ((long) item.getEnergyStored() * 100 / max);

			// Full: move it to the output slot if there's room.
			if (item.getEnergyStored() >= max && be.items.get(OUTPUT_SLOT).isEmpty()) {
				be.items.set(OUTPUT_SLOT, stack);
				be.items.set(INPUT_SLOT, ItemStack.EMPTY);
			}
			be.setChanged();
		}

		boolean working = be.lastRate > 0;
		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{INPUT_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == INPUT_SLOT && isChargeable(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == INPUT_SLOT && isChargeable(stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1; // one item charges at a time
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.charger");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new ChargerMenu(containerId, inventory, this, data);
	}
}
