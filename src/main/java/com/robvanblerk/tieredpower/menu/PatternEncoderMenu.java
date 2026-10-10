package com.robvanblerk.tieredpower.menu;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.PatternEncoderBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

/**
 * Slots: 0 blank pattern, 1 encoded pattern, 2-10 recipe grid (copies), 11 result preview (crafting mode),
 * 12-14 outputs, 15-17 fluid inputs, 18-20 fluid outputs (processing mode), 21+ player.
 */
public class PatternEncoderMenu extends AbstractContainerMenu {
	public static final int BLANK = 0, OUTPUT = 1, GRID_START = 2, PREVIEW = 11, OUT_START = 12, FLUID_IN_START = 15, FLUID_OUT_START = 18, MACHINE = 21, PLAYER_START = 22;
	public static final int INV_Y = 102;
	public static final int BUTTON_ENCODE = 0, BUTTON_CLEAR = 1, BUTTON_MODE = 2, BUTTON_SUBSTITUTES = 3, BUTTON_CHARGE = 4;
	private final @Nullable PatternEncoderBlockEntity encoder;
	private final Container slotsContainer, grid, outputs, fluidIn, fluidOut, machine;
	private final ContainerData data;

	public PatternEncoderMenu(int id, Inventory inv) {
		this(id, inv, null, new SimpleContainer(2), new SimpleContainer(9), new SimpleContainer(3), new SimpleContainer(3), new SimpleContainer(3),
				new SimpleContainer(1), new SimpleContainer(1), new SimpleContainerData(2));
	}

	public PatternEncoderMenu(int id, Inventory inv, PatternEncoderBlockEntity be) {
		this(id, inv, be, be.slots(), be.grid(), be.outputs(), be.fluidIn(), be.fluidOut(), be.machine(), be.preview(), be.data());
	}

	private PatternEncoderMenu(int id, Inventory inv, @Nullable PatternEncoderBlockEntity be, Container slots, Container grid, Container outputs,
			Container fluidIn, Container fluidOut, Container machine, Container preview, ContainerData data) {
		super(ModMenus.PATTERN_ENCODER.get(), id);
		this.encoder = be;
		this.slotsContainer = slots;
		this.grid = grid;
		this.outputs = outputs;
		this.fluidIn = fluidIn;
		this.fluidOut = fluidOut;
		this.machine = machine;
		this.data = data;
		addSlot(new Slot(slots, PatternEncoderBlockEntity.BLANK_SLOT, 146, 17) {
			@Override public boolean mayPlace(ItemStack s) { return s.is(ModBlocks.BLANK_PATTERN.get()); }
		});
		addSlot(new Slot(slots, PatternEncoderBlockEntity.OUTPUT_SLOT, 146, 53) {
			@Override public boolean mayPlace(ItemStack s) { return false; }
		});
		for (int i = 0; i < 9; i++) addSlot(new GhostSlot(grid, i, 26 + (i % 3) * 18, 17 + (i / 3) * 18, -1));
		addSlot(new GhostSlot(preview, 0, 110, 35, PatternEncoderBlockEntity.MODE_CRAFTING));
		for (int i = 0; i < 3; i++) addSlot(new GhostSlot(outputs, i, 110, 17 + i * 18, PatternEncoderBlockEntity.MODE_PROCESSING));
		for (int i = 0; i < 3; i++) addSlot(new GhostSlot(fluidIn, i, 26 + i * 18, 75, PatternEncoderBlockEntity.MODE_PROCESSING));
		for (int i = 0; i < 3; i++) addSlot(new GhostSlot(fluidOut, i, 110 + i * 18, 75, PatternEncoderBlockEntity.MODE_PROCESSING));
		addSlot(new GhostSlot(machine, 0, 34, 75, PatternEncoderBlockEntity.MODE_PROCESSING)); // which machine (bottom-left, clear of the Encode button)
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
		addDataSlots(data);
	}

	/** A slot holding a copy, never a real item. onlyInMode: -1 = always shown, else only in that mode. */
	private class GhostSlot extends Slot {
		private final int onlyInMode;

		GhostSlot(Container c, int index, int x, int y, int onlyInMode) {
			super(c, index, x, y);
			this.onlyInMode = onlyInMode;
		}

		@Override public boolean mayPlace(ItemStack s) { return false; }
		@Override public boolean mayPickup(Player p) { return false; }
		@Override public boolean isActive() { return onlyInMode < 0 || getMode() == onlyInMode; }
	}

	public int getMode() {
		return data.get(0);
	}

	public boolean substitutesOn() {
		return data.get(1) == 1;
	}

	/**
	 * Grid and output slots hold copies. Crafting grid: click with an item to set it, empty hand to clear.
	 * Processing: left-click with a stack sets that amount; right-click adds one (or with an empty hand takes one away).
	 */
	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		if (slotId == MACHINE) { // the machine slot: click with the machine's item (blocks only), empty hand to clear
			ItemStack carried = getCarried();
			if (carried.isEmpty()) machine.setItem(0, ItemStack.EMPTY);
			else if (carried.getItem() instanceof net.minecraft.world.item.BlockItem) machine.setItem(0, carried.copyWithCount(1));
			machine.setChanged();
			return;
		}
		if (slotId >= FLUID_IN_START && slotId < MACHINE) {
			clickFluid(slotId < FLUID_OUT_START ? fluidIn : fluidOut, slotId < FLUID_OUT_START ? slotId - FLUID_IN_START : slotId - FLUID_OUT_START, button);
			return;
		}
		boolean isGrid = slotId >= GRID_START && slotId < GRID_START + 9;
		boolean isOut = slotId >= OUT_START && slotId < OUT_START + 3;
		if (isGrid || isOut) {
			Container c = isGrid ? grid : outputs;
			int i = isGrid ? slotId - GRID_START : slotId - OUT_START;
			ItemStack carried = getCarried();
			ItemStack here = c.getItem(i);
			boolean processing = getMode() == PatternEncoderBlockEntity.MODE_PROCESSING;
			ItemStack set;
			if (!processing) set = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
			else if (button == 1) { // right-click: one more / one fewer
				if (carried.isEmpty()) set = here.getCount() <= 1 ? ItemStack.EMPTY : here.copyWithCount(here.getCount() - 1);
				else if (ItemStack.isSameItemSameTags(here, carried)) set = here.copyWithCount(Math.min(64, here.getCount() + 1));
				else set = carried.copyWithCount(1);
			} else {
				set = carried.isEmpty() ? ItemStack.EMPTY : carried.copy();
			}
			c.setItem(i, set);
			c.setChanged();
			return;
		}
		if (slotId == PREVIEW) return;
		super.clicked(slotId, button, type, player);
	}

	/**
	 * Fluid slots: click with a bucket or tank to set its fluid and amount; right-click to add another container's
	 * worth; with an empty hand, left-click clears and right-click takes 1,000 mB off.
	 */
	private void clickFluid(Container c, int i, int button) {
		ItemStack carried = getCarried();
		var here = com.robvanblerk.tieredpower.item.FluidDropItem.fluid(c.getItem(i));
		net.minecraftforge.fluids.FluidStack set;
		if (carried.isEmpty()) {
			set = button == 1 && here.getAmount() > 1000 ? new net.minecraftforge.fluids.FluidStack(here, here.getAmount() - 1000) : net.minecraftforge.fluids.FluidStack.EMPTY;
		} else {
			var inside = net.minecraftforge.fluids.FluidUtil.getFluidContained(carried).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
			if (inside.isEmpty()) return;
			if (button == 1 && here.isFluidEqual(inside)) set = new net.minecraftforge.fluids.FluidStack(here, Math.min(1_000_000, here.getAmount() + inside.getAmount()));
			else set = inside.copy();
		}
		c.setItem(i, set.isEmpty() ? ItemStack.EMPTY : com.robvanblerk.tieredpower.item.FluidDropItem.of(set));
		c.setChanged();
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (encoder == null) return false;
		switch (id) {
			case BUTTON_ENCODE -> encoder.encode();
			case BUTTON_CLEAR -> encoder.clearAll();
			case BUTTON_MODE -> encoder.toggleMode();
			case BUTTON_SUBSTITUTES -> encoder.toggleSubstitutes();
			case BUTTON_CHARGE -> encoder.chargeOutputs();
			default -> {
				return false;
			}
		}
		return true;
	}

	public void fill(boolean processing, List<ItemStack> ins, List<ItemStack> outs, List<ItemStack> fins, List<ItemStack> fouts, ItemStack machineItem) {
		if (encoder == null) return;
		if (processing) encoder.setProcessing(ins, outs, fins, fouts, machineItem); else encoder.setCrafting(ins);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index >= GRID_START && index < PLAYER_START) return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < GRID_START) {
			if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
		} else if (stack.is(ModBlocks.BLANK_PATTERN.get())) {
			if (!moveItemStackTo(stack, BLANK, BLANK + 1, false)) return ItemStack.EMPTY;
		} else {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
		return copy;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.index < GRID_START || slot.index >= PLAYER_START;
	}

	@Override
	public boolean stillValid(Player player) {
		return slotsContainer.stillValid(player);
	}
}
