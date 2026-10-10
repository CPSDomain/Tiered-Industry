package com.robvanblerk.tieredpower;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Gives each player the Tiered Industries Guide once, the first time they join a world. */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GuideEvents {
	private static final String GIVEN = "tieredpower_guide_given";

	@SubscribeEvent
	public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) return;
		CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
		if (persisted.getBoolean(GIVEN)) return;
		persisted.putBoolean(GIVEN, true);
		player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
		ItemStack book = new ItemStack(ModBlocks.GUIDE_BOOK.get());
		if (!player.getInventory().add(book)) player.drop(book, false);
	}

	private GuideEvents() {}
}
