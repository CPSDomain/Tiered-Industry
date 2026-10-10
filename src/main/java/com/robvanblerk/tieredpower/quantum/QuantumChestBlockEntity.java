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
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
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
 * Quantum Chest: a 54-slot chest. On its own it's a main chest. Link other Quantum Chests to it with a Quantum Linker -
 * any distance, any world. A linked chest is either:
 * <ul>
 * <li>Send (default): everything put in it (or only the filtered items, if a filter is set) goes to the main chest;</li>
 * <li>Receive: the main chest sends it the filtered items (needs a filter) - e.g. supplies out to a planet base.</li>
 * </ul>
 * When the chest at the other end isn't loaded, items wait in transit and land as soon as it is.
 */
public class QuantumChestBlockEntity extends BlockEntity implements ImplementedContainer, MenuProvider {
	public static final int SLOTS = 54, FILTER_SLOTS = 9, SEND_EVERY = 10;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final SimpleContainer filter = new SimpleContainer(FILTER_SLOTS);
	/** This chest's own id (others link to it). */
	private UUID id = UUID.randomUUID();
	/** The main chest this one is linked to, or null if it is a main chest. */
	private @Nullable UUID linkedTo;
	private boolean receive;
	/** The saved link info needs refreshing (link, mode or filter changed). */
	private boolean linkDirty = true;
	private int ticks;
	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new InvWrapper(this));

	/** 0 main?, 1 receive?, 2 linked chests sending, 3 linked chests receiving. */
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> isMain() ? 1 : 0;
				case 1 -> receive ? 1 : 0;
				case 2, 3 -> isMain() && level != null && level.getServer() != null ? QuantumNetworks.get(level.getServer()).linkCounts(id)[index - 2] : 0;
				default -> 0;
			};
		}

		@Override public void set(int index, int value) {}
		@Override public int getCount() { return 4; }
	};

	public QuantumChestBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.QUANTUM_CHEST.get(), pos, state);
		filter.addListener(c -> { linkDirty = true; if (level != null && !level.isClientSide()) setChanged(); });
	}

	public UUID getId() { return id; }
	public @Nullable UUID getLinkedTo() { return linkedTo; }
	public boolean isMain() { return linkedTo == null; }
	public boolean isReceive() { return receive; }
	public SimpleContainer getFilter() { return filter; }

	public void linkTo(@Nullable UUID main) {
		UUID next = main != null && main.equals(id) ? null : main;
		if (next == null && linkedTo != null) {
			receive = false;
			unloadTransit();
		}
		linkedTo = next;
		linkDirty = true;
		setChanged();
	}

	public void toggleMode() {
		if (isMain()) return;
		receive = !receive;
		if (!receive) unloadTransit();
		linkDirty = true;
		setChanged();
	}

	/** Anything still on its way to this chest as a Receive chest lands in it now (or drops if it's full). */
	private void unloadTransit() {
		if (!(level instanceof ServerLevel server)) return;
		IItemHandler me = new InvWrapper(this);
		for (ItemStack s : QuantumNetworks.get(server.getServer()).takePending(id)) {
			ItemStack left = ItemHandlerHelper.insertItemStacked(me, s, false);
			if (!left.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
		}
	}

	private List<Item> filterItems() {
		List<Item> out = new ArrayList<>();
		for (int i = 0; i < FILTER_SLOTS; i++) {
			ItemStack s = filter.getItem(i);
			if (!s.isEmpty() && !out.contains(s.getItem())) out.add(s.getItem());
		}
		return out;
	}

	private boolean hasFilter() {
		return !filter.isEmpty();
	}

	@Override
	public NonNullList<ItemStack> getItems() {
		return items;
	}

	public ContainerData getData() {
		return data;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, QuantumChestBlockEntity be) {
		if (!(level instanceof ServerLevel server) || ++be.ticks % SEND_EVERY != 0) return;
		QuantumNetworks network = QuantumNetworks.get(server.getServer());
		if (be.linkDirty) {
			be.linkDirty = false;
			network.setLink(be.id, be.isMain() ? null : new QuantumNetworks.Link(be.linkedTo, be.receive, be.filterItems(), GlobalPos.of(level.dimension(), pos)));
		}
		be.receivePending(network);
		if (be.isMain()) {
			network.setMain(be.id, GlobalPos.of(level.dimension(), pos));
			be.supplyReceivers(server, network);
		} else if (!be.receive) {
			be.send(server, network);
		}
	}

	/** Take in whatever arrived while this chest wasn't loaded. */
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

	/** The loaded chest with this id at this position, or null. */
	private static @Nullable QuantumChestBlockEntity chestAt(ServerLevel level, GlobalPos pos, UUID chestId) {
		ServerLevel there = level.getServer().getLevel(pos.dimension());
		if (there == null || !there.isLoaded(pos.pos())) return null;
		return there.getBlockEntity(pos.pos()) instanceof QuantumChestBlockEntity c && c.id.equals(chestId) ? c : null;
	}

	/** Send chest: move items to the main chest (straight in if it's loaded, otherwise into transit). */
	private void send(ServerLevel level, QuantumNetworks network) {
		GlobalPos main = network.main(linkedTo);
		if (main == null) return; // its main chest is gone
		QuantumChestBlockEntity m = chestAt(level, main, linkedTo);
		IItemHandler target = m == null ? null : new InvWrapper(m);
		List<Item> only = hasFilter() ? filterItems() : null;
		boolean changed = false;
		for (int i = 0; i < SLOTS; i++) {
			ItemStack s = items.get(i);
			if (s.isEmpty() || (only != null && !only.contains(s.getItem()))) continue;
			ItemStack left = target != null ? ItemHandlerHelper.insertItemStacked(target, s.copy(), false) : network.queue(linkedTo, s, QuantumNetworks.MAIN_PENDING);
			if (left.getCount() != s.getCount()) { items.set(i, left); changed = true; }
		}
		if (changed) setChanged();
	}

	/** Main chest: send each Receive chest the items on its filter. */
	private void supplyReceivers(ServerLevel level, QuantumNetworks network) {
		boolean changed = false;
		for (var e : network.receiversOf(id)) {
			QuantumNetworks.Link link = e.getValue();
			if (link.filter().isEmpty()) continue;
			QuantumChestBlockEntity r = chestAt(level, link.pos(), e.getKey());
			if (r == null) {
				ServerLevel there = level.getServer().getLevel(link.pos().dimension());
				if (there != null && there.isLoaded(link.pos().pos())) { network.setLink(e.getKey(), null); continue; } // chest is gone
			}
			IItemHandler target = r == null ? null : new InvWrapper(r);
			for (int i = 0; i < SLOTS; i++) {
				ItemStack s = items.get(i);
				if (s.isEmpty() || !link.matches(s)) continue;
				ItemStack left = target != null ? ItemHandlerHelper.insertItemStacked(target, s.copy(), false) : network.queue(e.getKey(), s, QuantumNetworks.RECEIVE_PENDING);
				if (left.getCount() != s.getCount()) { items.set(i, left); changed = true; }
			}
		}
		if (changed) setChanged();
	}

	/** Lines for Jade. */
	public List<String> info() {
		List<String> out = new ArrayList<>();
		if (level == null || level.getServer() == null) return out;
		QuantumNetworks network = QuantumNetworks.get(level.getServer());
		if (isMain()) {
			int[] n = network.linkCounts(id);
			out.add(n[0] + n[1] == 0 ? "Main chest - link others to it with a Quantum Linker"
					: "Main chest: " + (n[0] + n[1]) + " linked (" + n[0] + " sending, " + n[1] + " receiving)");
		} else {
			GlobalPos main = network.main(linkedTo);
			if (main == null) out.add("!Its main chest is gone - link it again");
			else out.add((receive ? "Receives from" : "Sends to") + " the main chest at " + main.pos().getX() + " " + main.pos().getY() + " " + main.pos().getZ()
					+ (main.dimension().equals(level.dimension()) ? "" : " (" + main.dimension().location().getPath() + ")"));
			List<Item> f = filterItems();
			if (receive && f.isEmpty()) out.add("!Receive: set a filter to choose what to receive");
			else if (!f.isEmpty()) out.add((receive ? "Receives: " : "Sends only: ") + f.size() + " filtered item" + (f.size() == 1 ? "" : "s"));
		}
		int waiting = network.pendingCount(id);
		if (waiting > 0) out.add(waiting + " items in transit (arrive when there's room)");
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
		net.minecraft.nbt.ListTag f = new net.minecraft.nbt.ListTag();
		for (int i = 0; i < FILTER_SLOTS; i++) {
			if (filter.getItem(i).isEmpty()) continue;
			CompoundTag c = filter.getItem(i).save(new CompoundTag());
			c.putByte("Slot", (byte) i);
			f.add(c);
		}
		tag.put("filter", f);
		tag.putUUID("id", id);
		if (linkedTo != null) tag.putUUID("linkedTo", linkedTo);
		tag.putBoolean("receive", receive);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		items.clear();
		ContainerHelper.loadAllItems(tag, items);
		for (int i = 0; i < FILTER_SLOTS; i++) filter.setItem(i, ItemStack.EMPTY);
		for (net.minecraft.nbt.Tag t : tag.getList("filter", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
			CompoundTag c = (CompoundTag) t;
			int slot = c.getByte("Slot");
			if (slot >= 0 && slot < FILTER_SLOTS) filter.setItem(slot, ItemStack.of(c));
		}
		if (tag.hasUUID("id")) id = tag.getUUID("id");
		linkedTo = tag.hasUUID("linkedTo") ? tag.getUUID("linkedTo") : null;
		receive = tag.getBoolean("receive");
		linkDirty = true;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable(isMain() ? "container.tieredpower.quantum_chest_main" : "container.tieredpower.quantum_chest_linked");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
		return new QuantumChestMenu(windowId, inventory, this);
	}

	@Override
	public boolean stillValid(Player player) {
		return !isRemoved() && player.distanceToSqr(worldPosition.getCenter()) < 64;
	}
}
