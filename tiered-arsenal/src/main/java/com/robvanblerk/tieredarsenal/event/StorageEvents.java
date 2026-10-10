package com.robvanblerk.tieredarsenal.event;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.item.QuiverItem;
import com.robvanblerk.tieredarsenal.item.StorageItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Quiver: when a bow or crossbow finds no arrows in the inventory, the quiver supplies the next stack. */
@Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StorageEvents {
    private StorageEvents() {}

    @SubscribeEvent
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        if (!event.getProjectileItemStack().isEmpty() || !(event.getEntity() instanceof Player player)) return;
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack quiver = inv.getItem(i);
            if (!(quiver.getItem() instanceof QuiverItem)) continue;
            if (StorageItem.getContents(quiver).isEmpty()) continue;

            if (player.level().isClientSide()) {
                // Client only needs to know there is an arrow (so the draw starts); the server moves it.
                event.setProjectileItemStack(StorageItem.getContents(quiver).get(0));
                return;
            }

            int free = inv.getFreeSlot();
            if (free < 0) return; // nowhere to put the arrows

            ItemStack arrows = StorageItem.removeFirst(quiver);
            inv.setItem(free, arrows);
            event.setProjectileItemStack(arrows); // the bow uses this very stack, so it shrinks in the inventory
            return;
        }
    }
}
