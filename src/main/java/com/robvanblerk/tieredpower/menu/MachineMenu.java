package com.robvanblerk.tieredpower.menu;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;
import com.robvanblerk.tieredpower.energy.RedstoneMode;

/**
 * Shared GUI logic: syncs the energy number (split into two 16-bit halves, because Minecraft only
 * syncs 16-bit GUI values) and handles shift-clicking between machine slots and the player inventory.
 */
public abstract class MachineMenu extends AbstractContainerMenu {
	protected final ContainerData data;
	private final int machineSlots;
	private final Set<Integer> bigSlots = new HashSet<>();
	/** Empty upgrade slots show a faded picture of the item they take. */
	private final Map<Integer, ItemStack> ghostItems = new HashMap<>();

	/** Shows a faded picture of what an empty slot takes. */
	protected void setGhostItem(int slotIndex, ItemStack stack) {
		ghostItems.put(slotIndex, stack);
	}
	private int speedSlot = -1;
	private int costDataIndex = -1;

	protected MachineMenu(@Nullable MenuType<?> type, int containerId, ContainerData data, int machineSlots) {
		super(type, containerId);
		this.data = data;
		this.machineSlots = machineSlots;
		addDataSlots(data);
		// Redstone mode: read from the machine on the server, remembered on the client.
		DataSlot redstone = new DataSlot() {
			private int clientValue;

			@Override
			public int get() {
				MachineBlockEntity machine = machine();
				return machine != null ? machine.getRedstoneMode().ordinal() : clientValue;
			}

			@Override
			public void set(int value) {
				clientValue = value;
			}
		};
		addDataSlot(redstone);
		this.redstoneSlot = redstone;
		DataSlot autoOutput = new DataSlot() {
			private int clientValue;

			@Override
			public int get() {
				MachineBlockEntity machine = machine();
				return machine != null ? (machine.isAutoOutput() ? 1 : 0) : clientValue;
			}

			@Override
			public void set(int value) {
				clientValue = value;
			}
		};
		addDataSlot(autoOutput);
		this.autoOutputSlot = autoOutput;
		DataSlot sides = new DataSlot() {
			private int clientValue = 0xFFF;

			@Override
			public int get() {
				MachineBlockEntity machine = machine();
				if (machine == null) return clientValue;
				int packed = 0;
				for (int i = 0; i < 6; i++) packed |= machine.getRelativeMode(i) << (i * 2);
				return packed;
			}

			@Override
			public void set(int value) {
				clientValue = value;
			}
		};
		addDataSlot(sides);
		this.sidesSlot = sides;
		DataSlot facing = new DataSlot() {
			private int clientValue = -1;

			@Override
			public int get() {
				MachineBlockEntity machine = machine();
				if (machine == null) return clientValue;
				var state = machine.getBlockState();
				return state.hasProperty(com.robvanblerk.tieredpower.block.MachineBlock.FACING)
						? state.getValue(com.robvanblerk.tieredpower.block.MachineBlock.FACING).get2DDataValue() : -1;
			}

			@Override
			public void set(int value) {
				clientValue = value;
			}
		};
		addDataSlot(facing);
		this.facingSlot = facing;
		DataSlot info = new DataSlot() {
			private int clientValue;

			@Override
			public int get() {
				MachineBlockEntity machine = machine();
				if (machine == null) return clientValue;
				return machine.getTier() | (machine.isMuffled() ? 0x10 : 0) | (machine.supportsTiers() ? 0x20 : 0);
			}

			@Override
			public void set(int value) {
				clientValue = value;
			}
		};
		addDataSlot(info);
		this.upgradeInfo = info;
	}

	private final DataSlot upgradeInfo;

	private final DataSlot sidesSlot;
	private final DataSlot facingSlot;

	/** Which way the machine faces (its front), or null if it has no front. */
	public @org.jetbrains.annotations.Nullable net.minecraft.core.Direction getFacing() {
		int v = facingSlot.get();
		return v < 0 || v > 3 ? null : net.minecraft.core.Direction.from2DDataValue(v);
	}

	/** Item mode of relative face 0-5 (front, back, left, right, top, bottom). */
	public int getSideMode(int face) {
		return (sidesSlot.get() >> (face * 2)) & 3;
	}

	private final DataSlot autoOutputSlot;

	public boolean isAutoOutput() {
		return autoOutputSlot.get() != 0;
	}

	/** Machines that have output slots get the auto-output button. */
	public boolean hasAutoOutput() {
		return hasRedstoneControl();
	}

	private final DataSlot redstoneSlot;

	/** The machine behind this menu (server side only; null on the client). */
	private @Nullable MachineBlockEntity machine() {
		if (machineSlots <= 0 || slots.isEmpty()) return null;
		return slots.get(0).container instanceof MachineBlockEntity m ? m : null;
	}

	/** Machines with item slots get a redstone control button. */
	public boolean hasRedstoneControl() {
		return machineSlots > 0;
	}

	public RedstoneMode getRedstoneMode() {
		return RedstoneMode.byId(redstoneSlot.get());
	}

	/** Button 0 in the GUI cycles the redstone mode. */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		MachineBlockEntity machine = machine();
		if (id == 0 && machine != null) {
			machine.cycleRedstoneMode();
			return true;
		}
		if (id == 20 && machine != null) {
			machine.toggleAutoOutput();
			return true;
		}
		if (id >= 40 && id < 46 && machine != null) {
			machine.cycleSide(id - 40);
			return true;
		}
		if (id == BUTTON_REMOVE_MUFFLER && machine != null && machine.isMuffled()) {
			machine.setMuffled(false);
			ItemStack muffler = new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.MUFFLER.get());
			if (!player.getInventory().add(muffler)) player.drop(muffler, false);
			return true;
		}
		return false;
	}

	/** Adds an output-only slot, drawn as a large slot in the GUI. */
	protected void addOutputSlot(Container container, int index, int x, int y, boolean big) {
		Slot slot = addSlot(new Slot(container, index, x, y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
		if (big) bigSlots.add(slot.index);
	}

	public static final int BUTTON_REMOVE_MUFFLER = 60;
	/** Where the upgrade slots sit: inside the Upgrades window, to the right of the 176-wide machine screen. */
	public static final int UPGRADE_SLOT_X = 176 + 4 + 8, UPGRADE_SLOT_Y = 18;
	/** Client: is the Upgrades window open? (Upgrade slots only show, and can only be clicked, while it is.) */
	public boolean upgradesVisible;

	/** Adds the Speed and Energy upgrade slots (shown in the Upgrades window). */
	protected void addUpgradeSlots(Container container, int firstIndex, int costIndex) {
		this.costDataIndex = costIndex;
		Slot speed = addSlot(new UpgradeSlot(container, firstIndex, UPGRADE_SLOT_X, UPGRADE_SLOT_Y, com.robvanblerk.tieredpower.registry.ModBlocks.SPEED_UPGRADE.get()));
		Slot efficiency = addSlot(new UpgradeSlot(container, firstIndex + 1, UPGRADE_SLOT_X, UPGRADE_SLOT_Y + 22, com.robvanblerk.tieredpower.registry.ModBlocks.EFFICIENCY_UPGRADE.get()));
		speedSlot = speed.index;
		ghostItems.put(speed.index, new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.SPEED_UPGRADE.get()));
		ghostItems.put(efficiency.index, new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.EFFICIENCY_UPGRADE.get()));
	}

	private class UpgradeSlot extends Slot {
		private final net.minecraft.world.item.Item accepts;

		UpgradeSlot(Container container, int index, int x, int y, net.minecraft.world.item.Item accepts) {
			super(container, index, x, y);
			this.accepts = accepts;
		}

		@Override
		public boolean isActive() {
			return upgradesVisible;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			if (stack.is(accepts)) return true;
			// Mk II upgrades (from planet crystals) go in the same slots.
			return accepts == com.robvanblerk.tieredpower.registry.ModBlocks.SPEED_UPGRADE.get() ? stack.is(com.robvanblerk.tieredpower.registry.ModBlocks.SPEED_UPGRADE_2.get())
					: accepts == com.robvanblerk.tieredpower.registry.ModBlocks.EFFICIENCY_UPGRADE.get() && stack.is(com.robvanblerk.tieredpower.registry.ModBlocks.EFFICIENCY_UPGRADE_2.get());
		}

		@Override
		public int getMaxStackSize() {
			return com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.MAX_UPGRADES;
		}
	}

	public ItemStack getGhostItem(int slotIndex) {
		return ghostItems.getOrDefault(slotIndex, ItemStack.EMPTY);
	}

	public boolean hasUpgrades() {
		return speedSlot >= 0;
	}

	/** Upgrade levels: Mk II cards count double (up to 16). */
	private static int levels(ItemStack s) {
		int n = Math.min(com.robvanblerk.tieredpower.block.entity.MachineBlockEntity.MAX_UPGRADES, s.getCount());
		return s.is(com.robvanblerk.tieredpower.registry.ModBlocks.SPEED_UPGRADE_2.get()) || s.is(com.robvanblerk.tieredpower.registry.ModBlocks.EFFICIENCY_UPGRADE_2.get()) ? n * 2 : n;
	}

	public int getEfficiencyCount() {
		return speedSlot < 0 ? 0 : levels(slots.get(speedSlot + 1).getItem());
	}

	public int getSpeedCount() {
		return speedSlot < 0 ? 0 : levels(slots.get(speedSlot).getItem());
	}

	/** Tier (0-5), from the machine. */
	public int getTier() {
		return upgradeInfo.get() & 0xF;
	}

	public boolean isMuffled() {
		return (upgradeInfo.get() & 0x10) != 0;
	}

	public boolean supportsTiers() {
		return (upgradeInfo.get() & 0x20) != 0;
	}

	/** Speed multiplier shown in the GUI, e.g. 1.5 with one Speed upgrade. */
	public float getSpeedMultiplier() {
		if (speedSlot < 0) return 1f;
		return 1f + 0.5f * getSpeedCount();
	}

	/** Current energy use per tick, including upgrades. */
	public int getEnergyCost() {
		return costDataIndex < 0 ? 0 : data.get(costDataIndex);
	}

	public int getMachineSlotCount() {
		return machineSlots;
	}

	public boolean isBigSlot(int index) {
		return bigSlots.contains(index);
	}

	/** Adds the standard 27 inventory + 9 hotbar slots. Call after adding the machine's slots. */
	protected void addPlayerInventory(Inventory inventory) {
		addPlayerInventory(inventory, 0);
	}

	/** extraHeight moves the player inventory down for taller GUIs (e.g. the Quarry). */
	protected void addPlayerInventory(Inventory inventory, int extraHeight) {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + extraHeight + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, 142 + extraHeight));
		}
	}

	public long getEnergy() {
		return ((long) (data.get(1) & 0xFFFF) << 16) | (data.get(0) & 0xFFFF);
	}

	public abstract long getCapacity();

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		int playerStart = machineSlots;
		int playerEnd = playerStart + 36;

		if (index < machineSlots) {
			if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
		} else {
			if (machineSlots == 0 || !moveItemStackTo(stack, 0, machineSlots, false)) return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}
}
