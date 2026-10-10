package com.robvanblerk.tieredpower.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import com.robvanblerk.tieredpower.block.entity.SecurityTerminalBlockEntity;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.SecurityListPacket;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** The Security Terminal's screen: no slots - the player list travels in SecurityListPacket / SecurityActionPacket. */
public class SecurityTerminalMenu extends AbstractContainerMenu {
	private final Player player;
	private final @Nullable BlockPos pos;
	private String clientOwner = "";
	private List<SecurityListPacket.Member> clientMembers = List.of();

	public SecurityTerminalMenu(int id, Inventory inv) {
		this(id, inv, null);
	}

	public SecurityTerminalMenu(int id, Inventory inv, @Nullable BlockPos pos) {
		super(ModMenus.SECURITY_TERMINAL.get(), id);
		this.player = inv.player;
		this.pos = pos;
	}

	private @Nullable SecurityTerminalBlockEntity terminal() {
		return pos != null && player.level().getBlockEntity(pos) instanceof SecurityTerminalBlockEntity t ? t : null;
	}

	/** From SecurityActionPacket: 0 = send the list, 1 = add a player by name, 2 = remove a player. */
	public void handle(int action, String name, @Nullable UUID id) {
		SecurityTerminalBlockEntity t = terminal();
		if (t == null || !(player instanceof ServerPlayer sp) || !t.isOwner(player)) return;
		if (action == 1 && !name.isBlank()) {
			var server = sp.getServer();
			var online = server == null ? null : server.getPlayerList().getPlayerByName(name.trim());
			var profile = online != null ? java.util.Optional.of(online.getGameProfile())
					: server == null || server.getProfileCache() == null ? java.util.Optional.<com.mojang.authlib.GameProfile>empty() : server.getProfileCache().get(name.trim());
			if (profile.isEmpty()) {
				sp.displayClientMessage(Component.literal("No player called " + name.trim() + " has joined this server").withStyle(ChatFormatting.RED), true);
			} else {
				t.addMember(profile.get().getId(), profile.get().getName());
				sp.displayClientMessage(Component.literal(profile.get().getName() + " can now use this storage network").withStyle(ChatFormatting.GREEN), true);
			}
		} else if (action == 2 && id != null) {
			t.removeMember(id);
		}
		List<SecurityListPacket.Member> list = new ArrayList<>();
		for (Map.Entry<UUID, String> e : t.getMembers().entrySet()) list.add(new SecurityListPacket.Member(e.getKey(), e.getValue()));
		ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SecurityListPacket(containerId, t.getOwnerName(), list));
	}

	public void setClientData(String owner, List<SecurityListPacket.Member> members) {
		this.clientOwner = owner;
		this.clientMembers = members;
	}

	public String getClientOwner() { return clientOwner; }
	public List<SecurityListPacket.Member> getClientMembers() { return clientMembers; }

	@Override public ItemStack quickMoveStack(Player p, int index) { return ItemStack.EMPTY; }

	@Override
	public boolean stillValid(Player p) {
		return pos == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
	}
}
