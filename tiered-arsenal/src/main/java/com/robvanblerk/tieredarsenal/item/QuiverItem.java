package com.robvanblerk.tieredarsenal.item;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;

/**
 * Holds up to 4 stacks of arrows (any kind). When you shoot a bow or crossbow and have no arrows
 * left in your inventory, the quiver hands you its next stack (event/StorageEvents.java).
 * Right-click it in the air to pack every arrow in your inventory into it.
 */
public class QuiverItem extends StorageItem {
    public QuiverItem(Properties properties) {
        super(4, properties);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return stack.is(ItemTags.ARROWS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack quiver = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            Inventory inv = player.getInventory();
            int moved = 0;
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack s = inv.items.get(i);
                if (s != quiver && accepts(s)) {
                    moved += insert(quiver, s);
                }
            }
            if (moved > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(quiver, level.isClientSide());
    }

    @Override
    protected String getHelpKey() {
        return "quiver";
    }
}
