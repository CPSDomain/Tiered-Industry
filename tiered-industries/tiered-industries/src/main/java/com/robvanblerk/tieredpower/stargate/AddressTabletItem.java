package com.robvanblerk.tieredpower.stargate;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * A Stargate Address: an old stone tablet carved with the six glyphs of a hidden world. Found in chests around the
 * world (temples, strongholds, ancient cities, shipwrecks, end cities...) and on every planet's landing platform.
 * Right-click to learn it: the world then shows in your DHD's address list. Anyone can also just type the glyphs.
 */
public class AddressTabletItem extends Item {
	public AddressTabletItem(Properties properties) {
		super(properties);
	}

	public static ItemStack of(Planet p) {
		ItemStack s = new ItemStack(ModBlocks.ADDRESS_TABLET.get());
		s.getOrCreateTag().putString("planet", p.id);
		return s;
	}

	/** A tablet for a random hidden world. */
	public static ItemStack random(RandomSource rnd) {
		List<Planet> hidden = Planet.hidden();
		return of(hidden.get(rnd.nextInt(hidden.size())));
	}

	public static @Nullable Planet planet(ItemStack s) {
		return s.hasTag() ? Planet.byId(s.getTag().getString("planet")) : null;
	}

	@Override
	public Component getName(ItemStack stack) {
		Planet p = planet(stack);
		return p == null ? super.getName(stack) : Component.translatable(getDescriptionId(stack)).append(": " + p.title);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		Planet p = planet(stack);
		if (p == null) {
			tooltip.add(Component.literal("Blank - the carving has worn away").withStyle(ChatFormatting.GRAY));
			return;
		}
		StringBuilder sb = new StringBuilder("Glyphs: ");
		for (int g : GateAddress.of(p)) sb.append(g).append(' ');
		sb.append("+ origin");
		tooltip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.literal(p.description).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Right-click to learn this address").withStyle(ChatFormatting.DARK_AQUA));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return planet(stack) != null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack s = player.getItemInHand(hand);
		Planet p = planet(s);
		if (p == null) return InteractionResultHolder.pass(s);
		if (player instanceof ServerPlayer sp) {
			StargateData data = StargateData.get(sp.getServer());
			if (data.knows(sp.getUUID(), p)) {
				sp.displayClientMessage(Component.literal("You already know the address of " + p.title).withStyle(ChatFormatting.YELLOW), true);
			} else {
				data.learn(sp.getUUID(), p);
				sp.displayClientMessage(Component.literal("Address learned: " + p.title + " - it's now in your DHD's address list").withStyle(ChatFormatting.AQUA), false);
				level.playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
			}
		}
		return InteractionResultHolder.sidedSuccess(s, level.isClientSide());
	}
}
