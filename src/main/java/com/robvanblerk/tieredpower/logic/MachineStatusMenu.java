package com.robvanblerk.tieredpower.logic;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.StatusListPacket;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** The Machine Status Display's screen: no slots, just the list (sent every second while open). */
public class MachineStatusMenu extends AbstractContainerMenu {
	private final @Nullable MachineStatusDisplayBlockEntity display;
	private final Player player;
	private int ticks;
	private List<String> lines = List.of();

	public MachineStatusMenu(int id, Inventory inv) {
		this(id, inv, null, inv.player);
	}

	public MachineStatusMenu(int id, Inventory inv, @Nullable MachineStatusDisplayBlockEntity display, Player player) {
		super(ModMenus.MACHINE_STATUS.get(), id);
		this.display = display;
		this.player = player;
	}

	public List<String> lines() { return lines; }
	public void setLines(List<String> l) { lines = l; }

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		if (display != null && player instanceof ServerPlayer sp && ticks++ % 20 == 0) {
			List<String> out = new ArrayList<>();
			for (var e : display.entries()) out.add(e.encode());
			ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new StatusListPacket(containerId, out));
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return display == null || !display.isRemoved() && player.distanceToSqr(display.getBlockPos().getCenter()) < 64;
	}
}
