package com.robvanblerk.tieredpower.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.menu.ItemFilterMenu;

/**
 * Item Filter. Right-click in the air to set up to 9 items and Whitelist/Blacklist. Right-click a pipe connection
 * with it to install it there: an INTO connection then only delivers matching items; a PULL connection only pulls them.
 */
public class ItemFilterItem extends Item {
	public ItemFilterItem(Properties properties) {
		super(properties);
	}

	public static List<Item> getItems(ItemStack filter) {
		List<Item> items = new ArrayList<>();
		CompoundTag tag = filter.getTag();
		if (tag == null) return items;
		ListTag list = tag.getList("Items", 8);
		for (int i = 0; i < list.size(); i++) {
			String id = list.getString(i);
			if (id.isEmpty()) {
				items.add(null);
				continue;
			}
			ResourceLocation rl = ResourceLocation.tryParse(id);
			items.add(rl == null ? null : BuiltInRegistries.ITEM.get(rl));
		}
		return items;
	}

	public static void setItem(ItemStack filter, int index, ItemStack pattern) {
		CompoundTag tag = filter.getOrCreateTag();
		ListTag list = tag.getList("Items", 8);
		while (list.size() < 9) list.add(StringTag.valueOf(""));
		list.set(index, StringTag.valueOf(pattern.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(pattern.getItem()).toString()));
		tag.put("Items", list);
	}

	/** 0 = exact item, 1 = shares a tag, 2 = same mod, 3 = id starts with the same word (deepslate_..., nether_...). */
	public static final String[] MODES = {"Item", "Tag", "Mod", "Name"};

	public static int getMode(ItemStack filter) {
		CompoundTag tag = filter.getTag();
		return tag == null ? 0 : Math.floorMod(tag.getInt("Mode"), MODES.length);
	}

	public static void setMode(ItemStack filter, int mode) {
		filter.getOrCreateTag().putInt("Mode", Math.floorMod(mode, MODES.length));
	}

	private static boolean matches(Item filterItem, ItemStack stack, int mode) {
		return switch (mode) {
			case 1 -> stack.is(filterItem) || filterItem.builtInRegistryHolder().tags().anyMatch(stack::is);
			case 2 -> BuiltInRegistries.ITEM.getKey(filterItem).getNamespace().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
			case 3 -> {
				String word = BuiltInRegistries.ITEM.getKey(filterItem).getPath().split("_")[0];
				yield BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith(word + "_") || stack.is(filterItem);
			}
			default -> stack.is(filterItem);
		};
	}

	public static boolean isWhitelist(ItemStack filter) {
		CompoundTag tag = filter.getTag();
		return tag == null || !tag.contains("Whitelist") || tag.getBoolean("Whitelist");
	}

	public static void setWhitelist(ItemStack filter, boolean whitelist) {
		filter.getOrCreateTag().putBoolean("Whitelist", whitelist);
	}

	/** Does this item pass the filter? An empty (or missing) filter lets everything through. */
	public static boolean passes(@Nullable ItemStack filter, ItemStack stack) {
		if (filter == null || filter.isEmpty()) return true;
		List<Item> items = getItems(filter);
		int mode = getMode(filter);
		boolean any = false, match = false;
		for (Item item : items) {
			if (item == null) continue;
			any = true;
			if (matches(item, stack, mode)) match = true;
		}
		if (!any) return true;
		return isWhitelist(filter) == match;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
		if (!level.isClientSide()) {
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ItemFilterMenu(id, inventory, stack),
					Component.translatable("item.tieredpower.item_filter")));
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal((isWhitelist(stack) ? "Allow" : "Block") + " - match by " + MODES[getMode(stack)].toLowerCase()).withStyle(ChatFormatting.AQUA));
		for (Item item : getItems(stack)) {
			if (item != null) tooltip.add(Component.literal("- ").append(item.getDescription()).withStyle(ChatFormatting.GRAY));
		}
		tooltip.add(Component.literal("Right-click air to edit; right-click a pipe connection to install").withStyle(ChatFormatting.DARK_GRAY));
	}
}
