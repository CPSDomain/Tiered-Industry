package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.registries.ForgeRegistries;

import com.robvanblerk.tieredpower.item.powered.ItemEnergy;

/**
 * Item Magnet: pulls dropped items and XP orbs to you while switched on (sneak + right-click). The Basic one works
 * from the hotbar with a 6-block reach and needs no power. The Advanced one reaches 12 blocks, works from anywhere in
 * your inventory, runs on FE (charge it in a Charger) and can ignore up to 9 items: hold the item in your off hand
 * and right-click with the magnet. Items riding a conveyor belt, or near a Drone Station that's collecting, are left
 * alone.
 */
public class MagnetItem extends Item {
	public static final int ADVANCED_CAPACITY = 200_000, FE_PER_PULL = 5, FILTER_SIZE = 9;
	private final boolean advanced;

	public MagnetItem(boolean advanced, Properties properties) {
		super(properties);
		this.advanced = advanced;
	}

	public boolean isAdvanced() {
		return advanced;
	}

	public int range() {
		return advanced ? 12 : 6;
	}

	public static boolean isOn(ItemStack stack) {
		return stack.hasTag() && stack.getTag().getBoolean("on");
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isOn(stack);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) return InteractionResultHolder.success(stack);
		ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
		if (advanced && !player.isShiftKeyDown() && !other.isEmpty()) {
			String id = String.valueOf(ForgeRegistries.ITEMS.getKey(other.getItem()));
			ListTag list = stack.getOrCreateTag().getList("ignore", Tag.TAG_STRING);
			boolean removed = false;
			for (int i = 0; i < list.size(); i++) if (list.getString(i).equals(id)) { list.remove(i); removed = true; break; }
			if (!removed && list.size() >= FILTER_SIZE) {
				player.displayClientMessage(Component.literal("The ignore list is full (" + FILTER_SIZE + " items)").withStyle(ChatFormatting.RED), true);
				return InteractionResultHolder.fail(stack);
			}
			if (!removed) list.add(StringTag.valueOf(id));
			stack.getTag().put("ignore", list);
			player.displayClientMessage(Component.literal((removed ? "No longer ignoring " : "Ignoring ") + other.getHoverName().getString()).withStyle(ChatFormatting.YELLOW), true);
			return InteractionResultHolder.success(stack);
		}
		if (player.isShiftKeyDown()) {
			boolean on = !isOn(stack);
			stack.getOrCreateTag().putBoolean("on", on);
			level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, on ? 1.2f : 0.6f);
			player.displayClientMessage(Component.literal("Magnet " + (on ? "on" : "off")).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
			return InteractionResultHolder.success(stack);
		}
		return InteractionResultHolder.pass(stack);
	}

	private boolean ignores(ItemStack magnet, ItemStack item) {
		if (!advanced || !magnet.hasTag()) return false;
		ListTag list = magnet.getTag().getList("ignore", Tag.TAG_STRING);
		if (list.isEmpty()) return false;
		String id = String.valueOf(ForgeRegistries.ITEMS.getKey(item.getItem()));
		for (int i = 0; i < list.size(); i++) if (list.getString(i).equals(id)) return true;
		return false;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide() || !(entity instanceof Player p) || !isOn(stack) || p.isSpectator() || p.isShiftKeyDown()) return;
		if (p.tickCount % 2 != 0) return;
		if (!advanced && slot >= 9) return; // Basic: hotbar only
		if (advanced && ItemEnergy.get(stack) < FE_PER_PULL) return;
		if (firstActiveMagnet(p) != stack) return; // several magnets don't stack up
		AABB area = p.getBoundingBox().inflate(range());
		Vec3 target = p.position().add(0, 0.5, 0);
		int pulled = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive() && !e.hasPickUpDelay())) {
			if (ignores(stack, item.getItem()) || onBelt(level, item) || com.robvanblerk.tieredpower.drone.DroneStationBlockEntity.collectingNear(level, item.position())) continue;
			Vec3 to = target.subtract(item.position());
			if (to.lengthSqr() > 1) item.setDeltaMovement(to.normalize().scale(0.4));
			pulled++;
		}
		for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, ExperienceOrb::isAlive)) {
			Vec3 to = target.subtract(orb.position());
			if (to.lengthSqr() > 1) orb.setDeltaMovement(to.normalize().scale(0.4));
			pulled++;
		}
		if (advanced && pulled > 0) ItemEnergy.use(stack, FE_PER_PULL * Math.min(pulled, 20));
	}

	private static boolean onBelt(Level level, ItemEntity item) {
		var at = item.blockPosition();
		return level.getBlockState(at).getBlock() instanceof com.robvanblerk.tieredpower.conveyor.ConveyorBlock
				|| level.getBlockState(at.below()).getBlock() instanceof com.robvanblerk.tieredpower.conveyor.ConveyorBlock;
	}

	private static @Nullable ItemStack firstActiveMagnet(Player p) {
		for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof MagnetItem m && isOn(s) && (!m.advanced || ItemEnergy.get(s) >= FE_PER_PULL)) {
			if (!m.advanced && p.getInventory().items.indexOf(s) >= 9) continue;
			return s;
		}
		return null;
	}

	// ---- energy (Advanced only) ----

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return advanced ? ItemEnergy.provider(stack, ADVANCED_CAPACITY, ADVANCED_CAPACITY / 50, false) : null;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return advanced;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return ItemEnergy.barWidth(stack, ADVANCED_CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return ItemEnergy.BAR_COLOUR;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal((isOn(stack) ? "On" : "Off") + " - sneak + right-click to switch").withStyle(isOn(stack) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		tooltip.add(Component.literal("Reach " + range() + " blocks, " + (advanced ? "works anywhere in your inventory" : "works from the hotbar")).withStyle(ChatFormatting.GRAY));
		if (advanced) {
			ItemEnergy.tooltip(stack, ADVANCED_CAPACITY, tooltip);
			ListTag list = stack.hasTag() ? stack.getTag().getList("ignore", Tag.TAG_STRING) : new ListTag();
			if (list.isEmpty()) tooltip.add(Component.literal("Ignore list: hold an item in your off hand and right-click").withStyle(ChatFormatting.DARK_GRAY));
			else {
				StringBuilder s = new StringBuilder("Ignoring: ");
				for (int i = 0; i < list.size(); i++) {
					var it = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(list.getString(i)));
					s.append(i > 0 ? ", " : "").append(it == null ? list.getString(i) : new ItemStack(it).getHoverName().getString());
				}
				tooltip.add(Component.literal(s.toString()).withStyle(ChatFormatting.YELLOW));
			}
		}
	}
}
