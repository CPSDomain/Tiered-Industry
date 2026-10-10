package com.robvanblerk.tieredpower.menu;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.MatrixControllerBlockEntity;
import com.robvanblerk.tieredpower.item.CraftingPatternItem;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Slots: 0-26 the current Pattern Bank's patterns, 27+ player. */
public class MatrixControllerMenu extends AbstractContainerMenu {
	// 0 page, 1 pages, 2 formed, 3 problem, 4 patterns, 5 accelerators, 6 crafts per cycle
	public static final int DATA_COUNT = 8, SLOTS = 27, INV_Y = 98;
	public static final int BUTTON_PREV = 0, BUTTON_NEXT = 1;
	private final @Nullable MatrixControllerBlockEntity controller;
	private final Container view;
	private final ContainerData data;

	public MatrixControllerMenu(int id, Inventory inv) {
		this(id, inv, null, new SimpleContainer(SLOTS), new SimpleContainerData(DATA_COUNT));
	}

	public MatrixControllerMenu(int id, Inventory inv, @Nullable MatrixControllerBlockEntity be, Container view, ContainerData data) {
		super(ModMenus.MATRIX_CONTROLLER.get(), id);
		this.player = inv.player;
		this.controller = be;
		this.view = view;
		this.data = data;
		for (int i = 0; i < SLOTS; i++) {
			addSlot(new Slot(view, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
				// Only when there's a Pattern Bank to hold it - otherwise the pattern would have nowhere to go.
				@Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof CraftingPatternItem && isFormed() && getPages() > 0; }
				@Override public int getMaxStackSize() { return 1; }
			});
		}
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
		addDataSlots(data);
	}

	public int getPage() { return data.get(0); }
	public int getPages() { return data.get(1); }
	public boolean isFormed() { return data.get(2) == 1; }
	public int getProblem() { return data.get(3); }
	public int getPatternCount() { return data.get(4); }
	public int getAccelerators() { return data.get(5); }
	public int getCraftsPerCycle() { return data.get(6); }
	public int getOverclock() { return data.get(7); }

	private final Player player;
	private int ticks;
	private java.util.List<String> clientMachines = java.util.List.of();

	public java.util.List<String> getMachines() { return clientMachines; }
	public void setMachines(java.util.List<String> list) { clientMachines = list; }

	/** Server: every 2 seconds, tell the screen which machines the Matrix can use. */
	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (controller != null && player instanceof net.minecraft.server.level.ServerPlayer sp && ticks++ % 40 == 0)
			com.robvanblerk.tieredpower.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
					new com.robvanblerk.tieredpower.network.MatrixMachinesPacket(containerId, controller.connectedMachines()));
	}

	/**
	 * Clicking a processing pattern while holding a machine's item (any block item that isn't a pattern) sets which
	 * machine the pattern runs on, without picking the pattern up.
	 */
	@Override
	public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
		if (slotId >= 0 && slotId < SLOTS && type == net.minecraft.world.inventory.ClickType.PICKUP) {
			ItemStack carried = getCarried();
			ItemStack here = slots.get(slotId).getItem();
			var pattern = com.robvanblerk.tieredpower.storage.CraftingPattern.fromStack(here);
			if (pattern != null && pattern.processing() && carried.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
				var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bi.getBlock());
				if (id != null) {
					ItemStack updated = here.copy();
					pattern.withMachine(id.toString()).writeTo(updated);
					slots.get(slotId).set(updated);
					if (!player.level().isClientSide())
						player.displayClientMessage(net.minecraft.network.chat.Component.literal("Pattern now runs on: " + bi.getBlock().getName().getString())
								.withStyle(net.minecraft.ChatFormatting.GREEN), true);
				}
				return;
			}
		}
		super.clicked(slotId, button, type, player);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (controller == null) return false;
		controller.turnPage(id == BUTTON_PREV ? -1 : 1);
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < SLOTS) {
			if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
		} else {
			if (!(stack.getItem() instanceof CraftingPatternItem) || !isFormed() || getPages() <= 0 || !moveItemStackTo(stack, 0, SLOTS, false)) return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
		return copy;
	}

	@Override
	public boolean stillValid(Player player) {
		return view.stillValid(player);
	}
}
