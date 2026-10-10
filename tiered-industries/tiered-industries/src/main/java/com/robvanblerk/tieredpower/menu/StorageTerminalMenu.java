package com.robvanblerk.tieredpower.menu;

import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.StorageListPacket;
import com.robvanblerk.tieredpower.registry.ModMenus;
import com.robvanblerk.tieredpower.storage.ItemKey;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * The terminal's container. The item grid isn't made of real slots: the server sends the stored list to the client
 * (StorageListPacket) and the client sends clicks back (StorageActionPacket).
 */
public class StorageTerminalMenu extends AbstractContainerMenu {
	public static final int ACTION_TAKE = 0, ACTION_TAKE_HALF = 1, ACTION_TAKE_TO_INVENTORY = 2, ACTION_PUT_ALL = 3, ACTION_PUT_ONE = 4, ACTION_CLEAR_GRID = 5, ACTION_FILL_CONTAINER = 6,
			ACTION_CRAFT_MISSING = 7;
	public static final int INV_Y = 140;
	/** Player inventory is slots 0-35. */
	public static final int PLAYER_SLOTS = 36;

	public record Entry(ItemStack item, long count) {}

	public record FluidEntry(FluidStack fluid, long amount) {}

	protected final Player player;
	protected final @Nullable BlockPos pos;
	private final int invY;
	/** Inventory slot of the Wireless Terminal this was opened with, or -1 for a placed terminal. */
	protected final int wirelessSlot;
	private StorageControllerBlockEntity controller;
	private long sentVersion = -1;
	private int ticks;

	// Client side: what the server last sent.
	private List<Entry> clientList = List.of();
	private List<FluidEntry> clientFluids = List.of();
	private List<ItemStack> clientCraftables = List.of();
	private StorageNetwork.Stats clientStats = StorageNetwork.Stats.OFFLINE;

	/** Client. */
	public StorageTerminalMenu(int id, Inventory inv) {
		this(id, inv, null);
	}

	/** Server. */
	public StorageTerminalMenu(int id, Inventory inv, @Nullable BlockPos pos) {
		this(ModMenus.STORAGE_TERMINAL.get(), id, inv, pos, INV_Y, -1);
	}

	/** Server, opened with a Wireless Terminal: pos is the linked Storage Controller. */
	public StorageTerminalMenu(int id, Inventory inv, BlockPos controllerPos, int wirelessSlot) {
		this(ModMenus.STORAGE_TERMINAL.get(), id, inv, controllerPos, INV_Y, wirelessSlot);
	}

	protected StorageTerminalMenu(net.minecraft.world.inventory.MenuType<?> type, int id, Inventory inv, @Nullable BlockPos pos, int invY, int wirelessSlot) {
		super(type, id);
		this.player = inv.player;
		this.pos = pos;
		this.invY = invY;
		this.wirelessSlot = wirelessSlot;
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(playerSlot(inv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
		for (int col = 0; col < 9; col++) addSlot(playerSlot(inv, col, 8 + col * 18, invY + 58));
	}

	/** The Wireless Terminal's own slot is locked while it's open. */
	private Slot playerSlot(Inventory inv, int index, int x, int y) {
		if (index != wirelessSlot) return new Slot(inv, index, x, y);
		return new Slot(inv, index, x, y) {
			@Override public boolean mayPickup(Player p) { return false; }
			@Override public boolean mayPlace(ItemStack s) { return false; }
		};
	}

	/** Wireless: still linked to this controller, in range, same dimension and charged? */
	private boolean wirelessOk(Player p) {
		ItemStack held = p.getInventory().getItem(wirelessSlot);
		if (!(held.getItem() instanceof com.robvanblerk.tieredpower.item.WirelessTerminalItem w)) return false;
		return w.canReach(held, p, pos) && com.robvanblerk.tieredpower.item.powered.ItemEnergy.get(held) >= com.robvanblerk.tieredpower.item.WirelessTerminalItem.COST_PER_SECOND;
	}

	public int invY() {
		return invY;
	}

	/** Actions only some terminals understand (e.g. clearing the crafting grid). */
	protected void handleOtherAction(int action) {}

	/** The storage controller this terminal talks to (refreshed by network()). */
	protected @Nullable StorageControllerBlockEntity controller() {
		network();
		return controller;
	}

	protected @Nullable StorageNetwork network() {
		if (pos == null) return null;
		if (controller == null || controller.isRemoved() || ticks % 40 == 0) controller = StorageNetwork.findController(player.level(), pos);
		return controller == null ? null : controller.getNetwork();
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (pos == null || !(player instanceof ServerPlayer sp)) return;
		ticks++;
		if (wirelessSlot >= 0 && ticks % 20 == 0) {
			com.robvanblerk.tieredpower.item.powered.ItemEnergy.use(sp.getInventory().getItem(wirelessSlot), com.robvanblerk.tieredpower.item.WirelessTerminalItem.COST_PER_SECOND);
		}
		if (ticks % 5 != 0 && sentVersion >= 0) return;
		StorageNetwork net = network();
		long version = StorageNetwork.version() * 2 + (net == null ? 0 : 1);
		if (version == sentVersion) return;
		sentVersion = version;
		List<Entry> list = new ArrayList<>();
		List<FluidEntry> fluids = new ArrayList<>();
		List<ItemStack> craftables = new ArrayList<>();
		StorageNetwork.Stats stats = StorageNetwork.Stats.OFFLINE;
		if (net != null) {
			for (Map.Entry<ItemKey, Long> e : net.listing()) list.add(new Entry(e.getKey().proto(), e.getValue()));
			for (Map.Entry<com.robvanblerk.tieredpower.storage.FluidKey, Long> e : net.fluidListing()) fluids.add(new FluidEntry(e.getKey().proto(), e.getValue()));
			stats = net.stats();
			java.util.Set<ItemKey> seen = new java.util.HashSet<>();
			for (var pattern : net.patterns()) if (pattern.hasItemOutput() && seen.add(pattern.outputKey())) craftables.add(pattern.output().copyWithCount(1));
		}
		ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new StorageListPacket(containerId, stats, list, fluids, craftables));
	}

	/** A click on the item grid, from StorageActionPacket. */
	public void handleAction(int action, ItemStack clicked, FluidStack clickedFluid) {
		StorageNetwork net = network();
		if (net == null) {
			handleOtherAction(action);
			return;
		}
		// Holding a bucket or tank: fill it from a clicked fluid, or pour its contents into storage.
		if (action == ACTION_FILL_CONTAINER && !clickedFluid.isEmpty() && fillCarried(net, clickedFluid)) {
			broadcastChanges();
			return;
		}
		if ((action == ACTION_PUT_ALL || action == ACTION_PUT_ONE || action == ACTION_FILL_CONTAINER) && pourCarried(net)) {
			broadcastChanges();
			return;
		}
		if (action == ACTION_FILL_CONTAINER) action = ACTION_PUT_ALL;
		ItemStack carried = getCarried();
		switch (action) {
			case ACTION_PUT_ALL -> setCarried(net.insert(carried, false));
			case ACTION_PUT_ONE -> {
				if (!carried.isEmpty() && net.insert(carried.copyWithCount(1), false).isEmpty()) carried.shrink(1);
				setCarried(carried);
			}
			case ACTION_TAKE, ACTION_TAKE_HALF -> {
				if (!carried.isEmpty() || clicked.isEmpty()) return;
				ItemKey key = new ItemKey(clicked);
				int amount = clicked.getMaxStackSize();
				if (action == ACTION_TAKE_HALF) amount = (int) Math.max(1, (Math.min(amount, net.count(key)) + 1) / 2);
				setCarried(net.extract(key, amount, false));
			}
			case ACTION_TAKE_TO_INVENTORY -> {
				if (clicked.isEmpty()) return;
				ItemKey key = new ItemKey(clicked);
				ItemStack got = net.extract(key, clicked.getMaxStackSize(), false);
				if (got.isEmpty()) return;
				player.getInventory().add(got);
				if (!got.isEmpty()) net.insert(got, false); // no room: put it back
			}
			default -> handleOtherAction(action);
		}
		broadcastChanges();
	}

	/** The Jobs screen: optionally cancel one job, then send the list. */
	public void handleJobsRequest(int cancel) {
		if (!(player instanceof ServerPlayer sp)) return;
		network(); // refreshes the controller
		List<com.robvanblerk.tieredpower.network.JobsListPacket.Job> list = new ArrayList<>();
		int cpus = 0;
		if (controller != null) {
			if (cancel >= 0 && controller.cancelJob(cancel))
				sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Crafting job cancelled - anything already made stays in storage")
						.withStyle(net.minecraft.ChatFormatting.YELLOW), true);
			for (var job : controller.getJobs()) list.add(new com.robvanblerk.tieredpower.network.JobsListPacket.Job(job.target, job.amount, job.craftsLeft(), job.status,
					com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity.describeSteps(job)));
			cpus = controller.getNetwork() == null ? 0 : controller.getNetwork().jobSlots();
		}
		ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new com.robvanblerk.tieredpower.network.JobsListPacket(list, cpus));
	}

	/** A craft request from the terminal: plan it (reply with the plan) or start it. */
	public void handleCraftRequest(ItemStack item, long amount, boolean start) {
		if (!(player instanceof ServerPlayer sp) || item.isEmpty()) return;
		network(); // refreshes the controller
		amount = Math.max(1, Math.min(amount, 1_000_000));
		if (controller == null || !controller.isOnline()) {
			ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new com.robvanblerk.tieredpower.network.CraftPlanPacket(false, "The storage network is offline", List.of()));
			return;
		}
		ItemKey key = new ItemKey(item);
		if (start) {
			String msg = controller.startJob(key, amount, player.getUUID());
			boolean ok = msg.startsWith("Crafting");
			sp.displayClientMessage(net.minecraft.network.chat.Component.literal(msg).withStyle(ok ? net.minecraft.ChatFormatting.AQUA : net.minecraft.ChatFormatting.RED), false);
			ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new com.robvanblerk.tieredpower.network.CraftPlanPacket(ok, msg, List.of()));
			return;
		}
		var plan = controller.plan(key, amount);
		List<com.robvanblerk.tieredpower.network.CraftPlanPacket.Line> lines = new ArrayList<>();
		for (var e : plan.missing().entrySet()) lines.add(new com.robvanblerk.tieredpower.network.CraftPlanPacket.Line(e.getKey().proto(), e.getValue(), com.robvanblerk.tieredpower.network.CraftPlanPacket.MISSING));
		for (var e : plan.crafted().entrySet()) lines.add(new com.robvanblerk.tieredpower.network.CraftPlanPacket.Line(e.getKey().proto(), e.getValue(), com.robvanblerk.tieredpower.network.CraftPlanPacket.CRAFTED));
		for (var e : plan.used().entrySet()) lines.add(new com.robvanblerk.tieredpower.network.CraftPlanPacket.Line(e.getKey().proto(), e.getValue(), com.robvanblerk.tieredpower.network.CraftPlanPacket.USED));
		// fluids and gases, shown as Fluid Drops (the count is mB)
		for (var e : plan.missingFluids().entrySet()) lines.add(0, fluidLine(e.getKey(), e.getValue(), com.robvanblerk.tieredpower.network.CraftPlanPacket.MISSING));
		for (var e : plan.madeFluids().entrySet()) lines.add(fluidLine(e.getKey(), e.getValue(), com.robvanblerk.tieredpower.network.CraftPlanPacket.CRAFTED));
		for (var e : plan.usedFluids().entrySet()) lines.add(fluidLine(e.getKey(), e.getValue(), com.robvanblerk.tieredpower.network.CraftPlanPacket.USED));
		String msg;
		if (plan.possible()) msg = plan.steps().isEmpty() ? "Nothing to craft" : "Ready to craft";
		else if (plan.crafted().isEmpty() && plan.used().isEmpty() && plan.usedFluids().isEmpty()) msg = "No pattern for this item";
		else msg = "Missing ingredients (red)";
		ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new com.robvanblerk.tieredpower.network.CraftPlanPacket(false, msg, lines));
	}

	private static com.robvanblerk.tieredpower.network.CraftPlanPacket.Line fluidLine(com.robvanblerk.tieredpower.storage.FluidKey key, long mb, int kind) {
		return new com.robvanblerk.tieredpower.network.CraftPlanPacket.Line(
				com.robvanblerk.tieredpower.item.FluidDropItem.of(key.toStack((int) Math.min(Integer.MAX_VALUE, Math.max(1, mb)))), mb, kind);
	}

	/** Hands back a used container: into the hand if it's free, else the inventory (or dropped). */
	private void giveBack(ItemStack carried, ItemStack result) {
		carried.shrink(1);
		if (carried.isEmpty()) {
			setCarried(result);
		} else {
			setCarried(carried);
			if (!player.getInventory().add(result)) player.drop(result, false);
		}
	}

	/** Fill the held bucket/tank with a fluid from storage. */
	private boolean fillCarried(StorageNetwork net, FluidStack fluid) {
		ItemStack carried = getCarried();
		if (carried.isEmpty()) return false;
		var handler = FluidUtil.getFluidHandler(carried.copyWithCount(1)).resolve().orElse(null);
		if (handler == null) return false;
		com.robvanblerk.tieredpower.storage.FluidKey key = new com.robvanblerk.tieredpower.storage.FluidKey(fluid);
		int available = (int) Math.min(Integer.MAX_VALUE, net.fluidCount(key));
		if (available <= 0) return false;
		int fits = handler.fill(key.toStack(available), IFluidHandler.FluidAction.SIMULATE);
		if (fits <= 0) return false;
		FluidStack got = net.extractFluid(key, fits, false);
		int filled = handler.fill(got, IFluidHandler.FluidAction.EXECUTE);
		if (filled < got.getAmount()) net.insertFluid(new FluidStack(got, got.getAmount() - filled), false);
		giveBack(carried, handler.getContainer());
		return true;
	}

	/** Pour the held bucket/tank into storage (only if the network has room for that fluid). */
	private boolean pourCarried(StorageNetwork net) {
		ItemStack carried = getCarried();
		if (carried.isEmpty()) return false;
		var handler = FluidUtil.getFluidHandler(carried.copyWithCount(1)).resolve().orElse(null);
		if (handler == null) return false;
		FluidStack inside = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
		if (inside.isEmpty()) return false;
		int fits = net.insertFluid(inside, true);
		if (fits <= 0) return false;
		FluidStack drained = handler.drain(new FluidStack(inside, fits),
				IFluidHandler.FluidAction.EXECUTE);
		if (drained.isEmpty()) return false; // e.g. a bucket that can only be emptied all at once
		net.insertFluid(drained, false);
		giveBack(carried, handler.getContainer());
		return true;
	}

	/** Shift-click in your inventory stores the stack. */
	@Override
	public ItemStack quickMoveStack(Player p, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		StorageNetwork net = network();
		if (net == null) return ItemStack.EMPTY;
		ItemStack left = net.insert(slot.getItem(), false);
		slot.set(left);
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player p) {
		if (pos == null) return true;
		if (wirelessSlot >= 0) return wirelessOk(p);
		return p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
	}

	// ---- client ----
	public void setClientData(StorageNetwork.Stats stats, List<Entry> list, List<FluidEntry> fluids, List<ItemStack> craftables) {
		this.clientStats = stats;
		this.clientList = list;
		this.clientFluids = fluids;
		this.clientCraftables = craftables;
	}

	public List<ItemStack> getClientCraftables() { return clientCraftables; }

	public List<Entry> getClientList() { return clientList; }
	public List<FluidEntry> getClientFluids() { return clientFluids; }
	public StorageNetwork.Stats getClientStats() { return clientStats; }
}
