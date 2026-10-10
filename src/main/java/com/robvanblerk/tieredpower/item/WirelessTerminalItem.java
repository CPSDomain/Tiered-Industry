package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity;
import com.robvanblerk.tieredpower.item.powered.ItemEnergy;
import com.robvanblerk.tieredpower.menu.CraftingTerminalMenu;
import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * A Storage (or Crafting) Terminal in your hand. Sneak + right-click a Storage Controller to link it, then right-click
 * anywhere within range (or press the Open Wireless Terminal key). Runs on FE; charge it like any powered item.
 */
public class WirelessTerminalItem extends Item {
	public static final int CAPACITY = 500_000, COST_PER_SECOND = 100, RANGE = 64, ACCESS_POINT_RANGE = 128;
	private final boolean crafting;

	public WirelessTerminalItem(boolean crafting, Properties properties) {
		super(properties);
		this.crafting = crafting;
	}

	public boolean isCrafting() {
		return crafting;
	}

	// ---- link ----

	public static @Nullable BlockPos linkedPos(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains("LinkPos") ? NbtUtils.readBlockPos(tag.getCompound("LinkPos")) : null;
	}

	public static @Nullable ResourceKey<Level> linkedLevel(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains("LinkDim")) return null;
		ResourceLocation id = ResourceLocation.tryParse(tag.getString("LinkDim"));
		return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
	}

	/** Linked to this controller, same dimension and within range? */
	public boolean canReach(ItemStack stack, Player player, @Nullable BlockPos controller) {
		BlockPos linked = linkedPos(stack);
		if (linked == null || controller == null || !linked.equals(controller)) return false;
		if (!player.level().dimension().equals(linkedLevel(stack))) return false;
		if (player.distanceToSqr(linked.getX() + 0.5, linked.getY() + 0.5, linked.getZ() + 0.5) <= (double) RANGE * RANGE) return true;
		// ...or near any Wireless Access Point on the controller's network (server side, where the network is known)
		if (player.level().getBlockEntity(linked) instanceof StorageControllerBlockEntity c && c.getNetwork() != null) {
			for (BlockPos ap : c.getNetwork().accessPoints()) {
				if (player.distanceToSqr(ap.getX() + 0.5, ap.getY() + 0.5, ap.getZ() + 0.5) <= (double) ACCESS_POINT_RANGE * ACCESS_POINT_RANGE) return true;
			}
		}
		return false;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
		if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof StorageControllerBlockEntity)) return InteractionResult.PASS;
		if (!ctx.getLevel().isClientSide()) {
			if (!com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, ctx.getLevel(), ctx.getClickedPos())) return InteractionResult.SUCCESS;
			CompoundTag tag = ctx.getItemInHand().getOrCreateTag();
			tag.put("LinkPos", NbtUtils.writeBlockPos(ctx.getClickedPos()));
			tag.putString("LinkDim", ctx.getLevel().dimension().location().toString());
			player.displayClientMessage(Component.literal("Linked to the Storage Controller").withStyle(ChatFormatting.GREEN), true);
		}
		return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			int slot = hand == InteractionHand.MAIN_HAND ? sp.getInventory().selected : Inventory.SLOT_OFFHAND;
			open(sp, slot);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	/** Opens the terminal held in this inventory slot, with a message saying why if it can't. */
	public static void open(ServerPlayer player, int slot) {
		ItemStack stack = player.getInventory().getItem(slot);
		if (!(stack.getItem() instanceof WirelessTerminalItem item)) return;
		BlockPos linked = linkedPos(stack);
		String problem = null;
		if (linked == null) problem = "Not linked - sneak + right-click a Storage Controller with it";
		else if (!player.level().dimension().equals(linkedLevel(stack))) problem = "The linked Storage Controller is in another dimension";
		else if (!item.canReach(stack, player, linked)) problem = "Out of range - " + RANGE + " blocks from the controller, or " + ACCESS_POINT_RANGE + " from an Access Point";
		else if (ItemEnergy.get(stack) < COST_PER_SECOND) problem = "Out of power - charge it in a Charger";
		else if (!(player.level().getBlockEntity(linked) instanceof StorageControllerBlockEntity c)) problem = "The linked Storage Controller is gone";
		else if (!c.isOnline()) problem = "The Storage Controller is offline (no power?)";
		if (problem != null) {
			player.displayClientMessage(Component.literal(problem).withStyle(ChatFormatting.RED), true);
			return;
		}
		if (!com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, player.level(), linked)) return;
		if (item.crafting) player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingTerminalMenu(id, inv, linked, slot),
				Component.translatable("item.tieredpower.wireless_crafting_terminal")));
		else player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StorageTerminalMenu(id, inv, linked, slot),
				Component.translatable("item.tieredpower.wireless_terminal")));
	}

	/** Used by the hotkey: the first Wireless Terminal in the hands, then the inventory. -1 if none. */
	public static int findSlot(Player player) {
		Inventory inv = player.getInventory();
		if (inv.getItem(inv.selected).getItem() instanceof WirelessTerminalItem) return inv.selected;
		if (inv.getItem(Inventory.SLOT_OFFHAND).getItem() instanceof WirelessTerminalItem) return Inventory.SLOT_OFFHAND;
		for (int i = 0; i < inv.items.size(); i++) if (inv.items.get(i).getItem() instanceof WirelessTerminalItem) return i;
		return -1;
	}

	// ---- energy ----

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return ItemEnergy.provider(stack, CAPACITY, CAPACITY / 50, false);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return ItemEnergy.barWidth(stack, CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return ItemEnergy.BAR_COLOUR;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged; // don't bob every time the charge changes
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		ItemEnergy.tooltip(stack, CAPACITY, tooltip);
		BlockPos linked = linkedPos(stack);
		if (linked == null) tooltip.add(Component.literal("Not linked: sneak + right-click a Storage Controller").withStyle(ChatFormatting.YELLOW));
		else tooltip.add(Component.literal("Linked to " + linked.getX() + ", " + linked.getY() + ", " + linked.getZ()).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Range " + RANGE + " blocks (" + ACCESS_POINT_RANGE + " around Access Points), " + COST_PER_SECOND / 20 + " FE/t while open")
				.withStyle(ChatFormatting.DARK_GRAY));
	}
}
