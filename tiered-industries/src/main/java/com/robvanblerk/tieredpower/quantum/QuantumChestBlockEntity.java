package com.robvanblerk.tieredpower.quantum;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.util.ImplementedContainer;

/**
 * Quantum Chest: a 54-slot chest. On its own it's a main chest. Link other Quantum Chests to it with a Quantum Linker
 * and they send everything put into them to the main chest - any distance, any world, no Stargate needed. If the main
 * chest's area isn't loaded, the items wait "in transit" (up to 270 stacks) and land in it as soon as it is.
 */
public class QuantumChestBlockEntity extends BlockEntity implements ImplementedContainer, MenuProvider {
	public static final int SLOTS = 54, SEND_EVERY = 10;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	/** This chest's own id (others link to it). */
	private UUID id = UUID.randomUUID();
	/** The main chest this one sends to, or null if it is a main chest. */
	private @Nullable UUID linkedTo;
	private int ticks;
	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new InvWrapper(this));

	public QuantumChestBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.QUANTUM_CHEST.get(), pos, state);
	}

	public UUID getId() { return id; }
	public @Nullable UUID getLinkedTo() { return linkedTo; }
	public boolean isMain() { return linkedTo == null; }

	public void linkTo(@Nullable UUID main) {
		linkedTo = main != null && main.equals(id) ? null : main;
		setChanged();
	}

	@Override
	public NonNullList<ItemStack> getItems() {
		return items;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, QuantumChestBlockEntity be) {
		if (!(level instanceof ServerLevel server) || ++be.ticks % SEND_EVERY != 0) return;
		QuantumNetworks network = QuantumNetworks.get(server.getServer());
		if (be.isMain()) {
			network.setMain(be.id, GlobalPos.of(level.dimension(), pos));
			be.receivePending(network);
		} else {
			be.send(server, network);
		}
	}

	/** Main chest: take in whatever arrived while it wasn't loaded. */
	private void receivePending(QuantumNetworks network) {
		List<ItemStack> waiting = network.pending(id);
		if (waiting.isEmpty()) return;
		IItemHandler me = new InvWrapper(this);
		boolean changed = false;
		for (int i = 0; i < waiting.size(); i++) {
			ItemStack s = waiting.get(i);
			ItemStack left = ItemHandlerHelper.insertItemStacked(me, s, false);
			if (left.getCount() != s.getCount()) { waiting.set(i, left); changed = true; }
		}
		waiting.removeIf(ItemStack::isEmpty);
		if (changed) { network.changed(); setChanged(); }
	}

	/** Linked chest: send everything to the main chest (straight in if it's loaded, otherwise into transit). */
	private void send(ServerLevel level, QuantumNetworks network) {
		GlobalPos main = linkedTo == null ? null : network.main(linkedTo);
		if (main == null) return; // its main chest is gone
		ServerLevel there = level.getServer().getLevel(main.dimension());
		IItemHandler target = null;
		if (there != null && there.isLoaded(main.pos()) && there.getBlockEntity(main.pos()) instanceof QuantumChestBlockEntity m && m.id.equals(linkedTo))
			target = new InvWrapper(m);
		boolean changed = false;
		for (int i = 0; i < SLOTS; i++) {
			ItemStack s = items.get(i);
			if (s.isEmpty()) continue;
			ItemStack left = target != null ? ItemHandlerHelper.insertItemStacked(target, s.copy(), false) : network.queue(linkedTo, s);
			if (left.getCount() != s.getCount()) { items.set(i, left); changed = true; }
		}
		if (changed) setChanged();
	}

	/** Lines for Jade. */
	public List<String> info() {
		List<String> out = new ArrayList<>();
		if (level == null || level.getServer() == null) return out;
		QuantumNetworks network = QuantumNetworks.get(level.getServer());
		if (isMain()) {
			out.add("Main chest - link others to it with a Quantum Linker");
			int waiting = network.pendingCount(id);
			if (waiting > 0) out.add(waiting + " items in transit (arrive when there's room)");
		} else {
			GlobalPos main = network.main(linkedTo);
			if (main == null) out.add("!Its main chest is gone - link it again");
			else out.add("Sends to the main chest at " + main.pos().getX() + " " + main.pos().getY() + " " + main.pos().getZ()
					+ (main.dimension().equals(level.dimension()) ? "" : " (" + main.dimension().location().getPath() + ")"));
		}
		return out;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); itemCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); itemCap = LazyOptional.of(() -> new InvWrapper(this)); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ContainerHelper.saveAllItems(tag, items);
		tag.putUUID("id", id);
		if (linkedTo != null) tag.putUUID("linkedTo", linkedTo);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		items.clear();
		ContainerHelper.loadAllItems(tag, items);
		if (tag.hasUUID("id")) id = tag.getUUID("id");
		linkedTo = tag.hasUUID("linkedTo") ? tag.getUUID("linkedTo") : null;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(isMain() ? "container.tieredpower.quantum_chest_main" : "container.tieredpower.quantum_chest_linked");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
		return ChestMenu.sixRows(windowId, inventory, this);
	}

	@Override
	public boolean stillValid(Player player) {
		return !isRemoved() && player.distanceToSqr(worldPosition.getCenter()) < 64;
	}
}
