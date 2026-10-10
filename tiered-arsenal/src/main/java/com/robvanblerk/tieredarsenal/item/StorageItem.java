package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An item that stores a few stacks of other items in its NBT, filled and emptied like a bundle:
 * right-click it onto an item in your inventory (or an item onto it) to put it in, right-click it
 * onto an empty slot to take the first stored stack out.
 */
public abstract class StorageItem extends Item {
    private static final String KEY = "Stored";
    private final int maxStacks;

    protected StorageItem(int maxStacks, Properties properties) {
        super(properties.stacksTo(1));
        this.maxStacks = maxStacks;
    }

    /** Which items this container accepts. */
    public abstract boolean accepts(ItemStack stack);

    // ---- Contents ----

    public static List<ItemStack> getContents(ItemStack container) {
        List<ItemStack> list = new ArrayList<>();
        CompoundTag tag = container.getTag();
        if (tag == null) return list;
        for (Tag t : tag.getList(KEY, Tag.TAG_COMPOUND)) {
            ItemStack s = ItemStack.of((CompoundTag) t);
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    public static void setContents(ItemStack container, List<ItemStack> contents) {
        ListTag list = new ListTag();
        for (ItemStack s : contents) {
            if (!s.isEmpty()) list.add(s.save(new CompoundTag()));
        }
        if (list.isEmpty()) {
            CompoundTag tag = container.getTag();
            if (tag != null) tag.remove(KEY);
        } else {
            container.getOrCreateTag().put(KEY, list);
        }
    }

    /** Puts as much of the stack in as fits; shrinks the given stack by what went in. Returns the amount moved. */
    public int insert(ItemStack container, ItemStack stack) {
        if (stack.isEmpty() || !accepts(stack)) return 0;
        List<ItemStack> contents = getContents(container);
        int before = stack.getCount();
        for (ItemStack s : contents) {
            if (stack.isEmpty()) break;
            if (ItemStack.isSameItemSameTags(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                stack.shrink(move);
            }
        }
        if (!stack.isEmpty() && contents.size() < maxStacks) {
            contents.add(stack.split(stack.getCount()));
        }
        setContents(container, contents);
        return before - stack.getCount();
    }

    /** Takes the first stored stack out (or an empty stack if there is none). */
    public static ItemStack removeFirst(ItemStack container) {
        List<ItemStack> contents = getContents(container);
        if (contents.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = contents.remove(0);
        setContents(container, contents);
        return out;
    }

    // ---- Bundle-style clicking ----

    @Override
    public boolean overrideStackedOnOther(ItemStack container, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack inSlot = slot.getItem();
        if (inSlot.isEmpty()) {
            ItemStack out = removeFirst(container);
            if (!out.isEmpty()) {
                ItemStack left = slot.safeInsert(out);
                if (!left.isEmpty()) insert(container, left);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 1.0F);
            }
        } else if (accepts(inSlot)) {
            ItemStack taken = slot.safeTake(inSlot.getCount(), inSlot.getCount(), player);
            insert(container, taken);
            if (!taken.isEmpty()) slot.safeInsert(taken); // put back what didn't fit
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
        }
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack container, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess carried) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (other.isEmpty()) {
            ItemStack out = removeFirst(container);
            if (!out.isEmpty()) {
                carried.set(out);
                player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 1.0F);
            }
        } else if (insert(container, other) > 0) {
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> contents = getContents(stack);
        tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".storage", contents.size(), maxStacks)
                .withStyle(ChatFormatting.GRAY));
        for (ItemStack s : contents) {
            tooltip.add(Component.literal("  " + s.getCount() + "x ").append(s.getHoverName())
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + "." + getHelpKey())
                .withStyle(ChatFormatting.GOLD));
    }

    protected abstract String getHelpKey();
}
