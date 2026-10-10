package com.robvanblerk.tieredpower.menu;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.registry.ModMenus;
import com.robvanblerk.tieredpower.storage.ItemKey;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * A Storage Terminal with a 3x3 crafting grid. After each craft, emptied grid slots are refilled from storage, so
 * shift-clicking the result crafts as many as your stored items allow. The grid empties back into storage on close.
 * Slots: 0-35 player inventory, 36-44 crafting grid, 45 result.
 */
public class CraftingTerminalMenu extends StorageTerminalMenu {
	public static final int INV_Y = 178, GRID_START = 36, RESULT_SLOT = 45;
	public static final int CRAFT_X = 26, CRAFT_Y = 108, RESULT_X = 124, RESULT_Y = 126;

	private final CraftingContainer craft = new TransientCraftingContainer(this, 3, 3);
	private final ResultContainer result = new ResultContainer();

	/** Client. */
	public CraftingTerminalMenu(int id, Inventory inv) {
		this(id, inv, null);
	}

	/** Server. */
	public CraftingTerminalMenu(int id, Inventory inv, @Nullable BlockPos pos) {
		this(id, inv, pos, -1);
	}

	/** Server; wirelessSlot >= 0 when opened with a Wireless Crafting Terminal (pos = the linked controller). */
	public CraftingTerminalMenu(int id, Inventory inv, @Nullable BlockPos pos, int wirelessSlot) {
		super(ModMenus.CRAFTING_TERMINAL.get(), id, inv, pos, INV_Y, wirelessSlot);
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 3; c++) addSlot(new Slot(craft, c + r * 3, CRAFT_X + c * 18, CRAFT_Y + r * 18));
		addSlot(new RefillResultSlot(inv.player, RESULT_X, RESULT_Y));
		addDataSlot(new net.minecraft.world.inventory.DataSlot() {
			@Override public int get() { return missingCount; }
			@Override public void set(int v) { missingCount = v; }
		});
	}

	public CraftingContainer craftGrid() {
		return craft;
	}

	/** Grid changed: work out the result (server only), like the vanilla crafting table. */
	@Override
	public void slotsChanged(Container container) {
		if (container != craft) {
			super.slotsChanged(container);
			return;
		}
		if (!(player instanceof ServerPlayer sp)) return;
		Level level = sp.level();
		ItemStack out = ItemStack.EMPTY;
		Optional<CraftingRecipe> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craft, level);
		if (recipe.isPresent() && result.setRecipeUsed(level, sp, recipe.get())) {
			ItemStack made = recipe.get().assemble(craft, level.registryAccess());
			if (made.isItemEnabled(level.enabledFeatures())) out = made;
		}
		result.setItem(0, out);
		setRemoteSlot(RESULT_SLOT, out);
		sp.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, out));
	}

	/** The result slot: after a craft, refill any emptied grid slot with the same item from storage. */
	private class RefillResultSlot extends ResultSlot {
		RefillResultSlot(Player player, int x, int y) {
			super(player, craft, result, 0, x, y);
		}

		@Override
		public void onTake(Player p, ItemStack stack) {
			ItemStack[] before = new ItemStack[9];
			for (int i = 0; i < 9; i++) before[i] = craft.getItem(i).isEmpty() ? ItemStack.EMPTY : craft.getItem(i).copyWithCount(1);
			super.onTake(p, stack);
			if (p.level().isClientSide()) return;
			StorageNetwork net = network();
			if (net == null) return;
			for (int i = 0; i < 9; i++) {
				if (before[i].isEmpty() || !craft.getItem(i).isEmpty()) continue;
				ItemStack got = net.extract(new ItemKey(before[i]), 1, false);
				if (!got.isEmpty()) craft.setItem(i, got);
			}
		}
	}

	/** Sends the grid back into storage (or your inventory if storage is full / offline). */
	public void clearGrid() {
		if (player.level().isClientSide()) return;
		StorageNetwork net = network();
		for (int i = 0; i < 9; i++) {
			ItemStack stack = craft.removeItemNoUpdate(i);
			if (stack.isEmpty()) continue;
			ItemStack left = net == null ? stack : net.insert(stack, false);
			if (!left.isEmpty()) player.getInventory().placeItemBackInInventory(left);
		}
		slotsChanged(craft);
	}

	@Override
	protected void handleOtherAction(int action) {
		if (action == ACTION_CLEAR_GRID) {
			clearGrid();
			lastRequest = List.of();
			refillTicks = 0;
		}
		if (action == ACTION_CRAFT_MISSING) craftMissing();
	}

	// ---- Craft All Missing ----

	/** What JEI's "+" last asked for (the allowed items per grid slot), so missing ingredients can be crafted. */
	private List<List<ItemStack>> lastRequest = List.of();
	private boolean lastMax;
	/** After Craft All Missing: keep filling the empty grid slots as the crafted items arrive (for up to 5 minutes). */
	private int refillTicks;
	private int missingCount;

	/** How many grid slots JEI wanted filled but couldn't be (synced to the screen for the button). */
	public int getMissingCount() { return missingCount; }

	private List<Integer> missingSlots() {
		List<Integer> out = new java.util.ArrayList<>();
		for (int i = 0; i < 9 && i < lastRequest.size(); i++) {
			boolean wanted = lastRequest.get(i).stream().anyMatch(s -> !s.isEmpty());
			if (wanted && craft.getItem(i).isEmpty()) out.add(i);
		}
		return out;
	}

	/** Starts crafting jobs for every ingredient the grid is missing, then fills them in as they're made. */
	public void craftMissing() {
		if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
		List<Integer> missing = missingSlots();
		if (missing.isEmpty()) {
			sp.displayClientMessage(net.minecraft.network.chat.Component.literal(lastRequest.isEmpty() ? "Use JEI's + on a recipe first" : "Nothing is missing")
					.withStyle(net.minecraft.ChatFormatting.YELLOW), true);
			return;
		}
		var controller = controller();
		if (controller == null || !controller.isOnline()) {
			sp.displayClientMessage(net.minecraft.network.chat.Component.literal("The storage network is offline").withStyle(net.minecraft.ChatFormatting.RED), true);
			return;
		}
		// How many of each item to make: one per empty slot (a stack per slot with shift + JEI's "+").
		java.util.Map<ItemKey, Long> want = new java.util.LinkedHashMap<>();
		List<String> noPattern = new java.util.ArrayList<>();
		for (int slot : missing) {
			ItemKey pick = null;
			for (ItemStack option : lastRequest.get(slot)) {
				if (option.isEmpty()) continue;
				ItemKey key = new ItemKey(option);
				var plan = controller.plan(key, 1);
				if (plan.possible() || !plan.crafted().isEmpty()) { pick = key; break; }
			}
			if (pick == null) {
				ItemStack first = lastRequest.get(slot).stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
				String n = first.getHoverName().getString();
				if (!noPattern.contains(n)) noPattern.add(n);
				continue;
			}
			long per = lastMax ? pick.proto().getMaxStackSize() : 1;
			want.merge(pick, per, Long::sum);
		}
		List<String> started = new java.util.ArrayList<>(), failed = new java.util.ArrayList<>();
		for (var e : want.entrySet()) {
			String msg = controller.startJob(e.getKey(), e.getValue(), player.getUUID());
			String n = e.getValue() + " " + e.getKey().proto().getHoverName().getString();
			if (msg.startsWith("Crafting")) started.add(n); else failed.add(n + " (" + msg + ")");
		}
		StringBuilder sb = new StringBuilder();
		if (!started.isEmpty()) sb.append("Crafting ").append(String.join(", ", started));
		if (!failed.isEmpty()) sb.append(sb.length() > 0 ? ". " : "").append("Couldn't start: ").append(String.join(", ", failed));
		if (!noPattern.isEmpty()) sb.append(sb.length() > 0 ? ". " : "").append("No pattern for: ").append(String.join(", ", noPattern));
		sp.displayClientMessage(net.minecraft.network.chat.Component.literal(sb.toString())
				.withStyle(started.isEmpty() ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.AQUA), false);
		if (!started.isEmpty()) refillTicks = 20 * 60 * 5;
	}

	/** Server: fill empty grid slots from storage while waiting for crafted ingredients; keep the button's count fresh. */
	@Override
	public void broadcastChanges() {
		if (!player.level().isClientSide()) {
			if (refillTicks > 0) {
				refillTicks--;
				if (refillTicks % 10 == 0) {
					List<Integer> missing = missingSlots();
					StorageNetwork net = missing.isEmpty() ? null : network();
					if (missing.isEmpty()) refillTicks = 0;
					else if (net != null) {
						boolean changed = false;
						for (int slot : missing) for (ItemStack option : lastRequest.get(slot)) {
							if (option.isEmpty()) continue;
							ItemStack got = net.extract(new ItemKey(option), lastMax ? option.getMaxStackSize() : 1, false);
							if (!got.isEmpty()) { craft.setItem(slot, got); changed = true; break; }
						}
						if (changed) slotsChanged(craft);
					}
				}
			}
			boolean empty = true;
			for (int i = 0; i < 9; i++) if (!craft.getItem(i).isEmpty()) { empty = false; break; }
			if (empty && refillTicks == 0) lastRequest = List.of(); // crafted it all, or cleared by hand
			missingCount = missingSlots().size();
		}
		super.broadcastChanges();
	}

	/**
	 * JEI's "+" button: clear the grid, then for each grid slot take one of the allowed items (or a stack each with
	 * shift) from storage, falling back to your inventory.
	 */
	public void fillGrid(List<List<ItemStack>> slots, boolean max) {
		if (player.level().isClientSide()) return;
		clearGrid();
		lastRequest = slots;
		lastMax = max;
		refillTicks = 0;
		StorageNetwork net = network();
		for (int i = 0; i < 9 && i < slots.size(); i++) {
			for (ItemStack option : slots.get(i)) {
				if (option.isEmpty()) continue;
				int want = max ? option.getMaxStackSize() : 1;
				ItemStack got = net == null ? ItemStack.EMPTY : net.extract(new ItemKey(option), want, false);
				if (got.isEmpty()) got = takeFromInventory(option, want);
				if (!got.isEmpty()) {
					craft.setItem(i, got);
					break;
				}
			}
		}
		slotsChanged(craft);
	}

	private ItemStack takeFromInventory(ItemStack option, int want) {
		Inventory inv = player.getInventory();
		for (int s = 0; s < inv.items.size(); s++) {
			ItemStack have = inv.items.get(s);
			if (!have.isEmpty() && ItemStack.isSameItemSameTags(have, option)) return have.split(Math.min(want, have.getCount()));
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack quickMoveStack(Player p, int index) {
		if (index < PLAYER_SLOTS) return super.quickMoveStack(p, index);
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		if (index == RESULT_SLOT) { // same as the vanilla crafting table: craft into the inventory
			ItemStack stack = slot.getItem();
			ItemStack copy = stack.copy();
			stack.getItem().onCraftedBy(stack, p.level(), p);
			if (!moveItemStackTo(stack, 0, PLAYER_SLOTS, true)) return ItemStack.EMPTY;
			slot.onQuickCraft(stack, copy);
			if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
			if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
			slot.onTake(p, stack);
			if (!stack.isEmpty()) p.drop(stack, false);
			return copy;
		}
		// grid slot: back into storage, else the inventory
		ItemStack stack = slot.getItem();
		StorageNetwork net = network();
		ItemStack left = net == null ? stack : net.insert(stack, false);
		if (!left.isEmpty()) moveItemStackTo(left, 0, PLAYER_SLOTS, false);
		slot.set(left.isEmpty() ? ItemStack.EMPTY : left);
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != result && super.canTakeItemForPickAll(stack, slot);
	}

	@Override
	public void removed(Player p) {
		super.removed(p);
		clearGrid();
	}
}
