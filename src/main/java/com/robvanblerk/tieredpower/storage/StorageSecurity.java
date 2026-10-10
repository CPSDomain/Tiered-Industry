package com.robvanblerk.tieredpower.storage;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.entity.SecurityTerminalBlockEntity;
import com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity;

/**
 * Storage network security. A network with a Security Terminal on it can only be used, changed or broken by the
 * terminal's owner and the players they trust (and server operators). Networks without one are open to everyone.
 */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StorageSecurity {
	/** The Security Terminal guarding the network a block belongs to, if any. */
	public static @Nullable SecurityTerminalBlockEntity guard(Level level, BlockPos pos) {
		StorageControllerBlockEntity c = StorageNetwork.findController(level, pos);
		return c == null ? null : c.securityTerminal();
	}

	/** May this player use the network at pos? Tells them (in the action bar) if not. */
	public static boolean check(Player player, Level level, BlockPos pos) {
		SecurityTerminalBlockEntity guard = guard(level, pos);
		if (guard == null || guard.isAllowed(player)) return true;
		player.displayClientMessage(Component.literal("This storage network is locked by " + guard.getOwnerName()).withStyle(ChatFormatting.RED), true);
		return false;
	}

	/** The secured network touching this position (the block itself or any neighbour), if any. */
	private static @Nullable SecurityTerminalBlockEntity guardNear(LevelAccessor accessor, BlockPos pos, boolean includeSelf) {
		if (!(accessor instanceof Level level)) return null;
		if (includeSelf && level.getBlockState(pos).getBlock() instanceof StorageNetworkBlock) {
			SecurityTerminalBlockEntity g = guard(level, pos);
			if (g != null) return g;
		}
		for (Direction d : Direction.values()) {
			BlockPos n = pos.relative(d);
			if (level.getBlockState(n).getBlock() instanceof StorageNetworkBlock) {
				SecurityTerminalBlockEntity g = guard(level, n);
				if (g != null) return g;
			}
		}
		return null;
	}

	/** No breaking a secured network's blocks; only the owner may break the Security Terminal itself. */
	@SubscribeEvent
	public static void onBreak(BlockEvent.BreakEvent event) {
		if (event.getLevel().isClientSide() || !(event.getState().getBlock() instanceof StorageNetworkBlock)) return;
		Player player = event.getPlayer();
		if (event.getLevel().getBlockEntity(event.getPos()) instanceof SecurityTerminalBlockEntity t) {
			if (!t.isOwner(player)) {
				event.setCanceled(true);
				player.displayClientMessage(Component.literal("Only " + t.getOwnerName() + " can remove this Security Terminal").withStyle(ChatFormatting.RED), true);
			}
			return;
		}
		SecurityTerminalBlockEntity guard = guardNear(event.getLevel(), event.getPos(), true);
		if (guard != null && !guard.isAllowed(player)) {
			event.setCanceled(true);
			player.displayClientMessage(Component.literal("This storage network is locked by " + guard.getOwnerName()).withStyle(ChatFormatting.RED), true);
		}
	}

	/**
	 * No adding storage parts to a secured network, and no placing anything with an inventory (pipes, hoppers...)
	 * against it, unless you're trusted.
	 */
	@SubscribeEvent
	public static void onPlace(BlockEvent.EntityPlaceEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) return;
		BlockState placed = event.getPlacedBlock();
		boolean storagePart = placed.getBlock() instanceof StorageNetworkBlock;
		if (!storagePart && !placed.hasBlockEntity()) return;
		SecurityTerminalBlockEntity guard = guardNear(event.getLevel(), event.getPos(), false);
		if (guard != null && !guard.isAllowed(player)) {
			event.setCanceled(true);
			player.displayClientMessage(Component.literal("This storage network is locked by " + guard.getOwnerName()).withStyle(ChatFormatting.RED), true);
		}
	}

	private StorageSecurity() {}
}
