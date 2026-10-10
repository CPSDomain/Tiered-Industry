package com.robvanblerk.tieredarsenal.item;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * Holds up to 4 tools or weapons (anything that doesn't stack and has durability).
 * Press the Tool Belt key (G by default) to swap what's in your hand for the next tool on the belt.
 */
public class ToolBeltItem extends StorageItem {
    public ToolBeltItem(Properties properties) {
        super(4, properties);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return stack.getMaxStackSize() == 1 && stack.isDamageableItem() && !(stack.getItem() instanceof ArmorItem)
                && !(stack.getItem() instanceof StorageItem);
    }

    @Override
    protected String getHelpKey() {
        return "tool_belt";
    }

    /** Finds the first tool belt in the player's inventory, or an empty stack. */
    public static ItemStack find(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof ToolBeltItem) return s;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Server side: puts the held item on the belt (if it fits) and takes the next tool off it.
     * With an empty hand it just takes the first tool.
     */
    public static void swap(Player player) {
        ItemStack belt = find(player);
        if (belt.isEmpty() || !(belt.getItem() instanceof ToolBeltItem beltItem)) return;
        List<ItemStack> contents = getContents(belt);
        if (contents.isEmpty()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && !beltItem.accepts(held)) return; // holding something that can't go on the belt

        ItemStack next = contents.remove(0);
        if (!held.isEmpty()) contents.add(held.copy());
        setContents(belt, contents);
        player.getInventory().setItem(player.getInventory().selected, next);
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.6F, 1.2F);
    }
}
