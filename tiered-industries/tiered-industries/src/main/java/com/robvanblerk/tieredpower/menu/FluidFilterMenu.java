package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import com.robvanblerk.tieredpower.item.FluidDropItem;
import com.robvanblerk.tieredpower.item.FluidFilterItem;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** The Fluid Filter's screen: 9 fluid slots (click with a bucket or tank; empty hand clears) and Allow/Block. */
public class FluidFilterMenu extends AbstractContainerMenu {
	public static final int GRID_X = 62, GRID_Y = 17, BUTTON_TOGGLE = 0;
	private final ItemStack filter; // EMPTY on the client
	private final SimpleContainer slots9 = new SimpleContainer(9);
	private final DataSlot whitelist;
	private final int lockedSlot;

	public FluidFilterMenu(int id, Inventory inventory) {
		this(id, inventory, ItemStack.EMPTY);
	}

	public FluidFilterMenu(int id, Inventory inventory, ItemStack filter) {
		super(ModMenus.FLUID_FILTER.get(), id);
		this.filter = filter;
		if (!filter.isEmpty()) {
			var fluids = FluidFilterItem.getFluids(filter);
			for (int i = 0; i < 9; i++) if (!fluids.get(i).isEmpty()) slots9.setItem(i, FluidDropItem.of(fluids.get(i)));
		}
		for (int i = 0; i < 9; i++) {
			addSlot(new Slot(slots9, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18) {
				@Override public boolean mayPlace(ItemStack stack) { return false; }
				@Override public boolean mayPickup(Player player) { return false; }
			});
		}
		this.lockedSlot = inventory.selected;
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
		for (int col = 0; col < 9; col++) {
			final int index = col;
			addSlot(new Slot(inventory, col, 8 + col * 18, 142) {
				@Override public boolean mayPickup(Player player) { return index != lockedSlot; }
			});
		}
		whitelist = new DataSlot() {
			private int clientValue = 1;
			@Override public int get() { return filter.isEmpty() ? clientValue : (FluidFilterItem.isWhitelist(filter) ? 1 : 0); }
			@Override public void set(int value) { clientValue = value; }
		};
		addDataSlot(whitelist);
	}

	public boolean isWhitelist() {
		return whitelist.get() == 1;
	}

	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		if (slotId >= 0 && slotId < 9) {
			ItemStack carried = getCarried();
			FluidStack fluid = carried.isEmpty() ? FluidStack.EMPTY : FluidUtil.getFluidContained(carried).orElse(FluidStack.EMPTY);
			if (!carried.isEmpty() && fluid.isEmpty()) return; // not a fluid container
			slots9.setItem(slotId, fluid.isEmpty() ? ItemStack.EMPTY : FluidDropItem.of(new FluidStack(fluid, 1000)));
			if (!filter.isEmpty()) FluidFilterItem.setFluid(filter, slotId, fluid);
			return;
		}
		super.clicked(slotId, button, type, player);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == BUTTON_TOGGLE && !filter.isEmpty()) {
			FluidFilterItem.setWhitelist(filter, !FluidFilterItem.isWhitelist(filter));
			return true;
		}
		return false;
	}

	@Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
	@Override public boolean stillValid(Player player) { return filter.isEmpty() || player.getMainHandItem() == filter; }
}
