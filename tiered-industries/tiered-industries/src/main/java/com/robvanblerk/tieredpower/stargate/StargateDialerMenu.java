package com.robvanblerk.tieredpower.stargate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.menu.MachineMenu;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class StargateDialerMenu extends MachineMenu {
	// 0 energy (per mille), 1 ring found, 2 on a planet, 3 open ticks (-1 held open by redstone), 4 redstone target,
	// 5 which hidden worlds the player knows (low 16 bits), 6 the rest (high bits)
	public static final int DATA_COUNT = 8; // 7: this gate is the far end of someone else's connection
	public static final int BUTTON_CLOSE = 97, BUTTON_REDSTONE = 98, BUTTON_HOME = 99, BUTTON_PLANET = 100;
	private final ContainerLevelAccess access;

	public StargateDialerMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
	}

	public StargateDialerMenu(int id, Inventory inv, ContainerData data, ContainerLevelAccess access) {
		super(ModMenus.STARGATE_DIALER.get(), id, data, 0);
		this.access = access;
		// no inventory: the screen is the DHD keypad
	}

	public float getEnergyFraction() { return data.get(0) / 1000f; }
	public boolean hasRing() { return data.get(1) != 0; }
	public boolean onPlanet() { return data.get(2) != 0; }
	/** Ticks the gate stays open; -1 while held open by redstone; 0 when shut. */
	public int getOpenTicks() { return data.get(3); }
	public boolean isOpen() { return data.get(3) != 0; }
	public boolean isIncoming() { return data.get(7) != 0; }
	public int getRedstoneTarget() { return data.get(4); }

	/** Whether the player looking at this DHD knows a world's address (the first eight are always known). */
	public boolean knows(Planet p) {
		if (p.known) return true;
		int mask = (data.get(5) & 0xFFFF) | ((data.get(6) & 0xFFFF) << 16);
		return (mask & (1 << p.ordinal())) != 0;
	}

	@Override public long getCapacity() { return 0; }

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if ((id == BUTTON_HOME || (id >= BUTTON_PLANET && id < BUTTON_PLANET + Planet.values().length)) && player instanceof ServerPlayer sp) {
			access.execute((level, pos) -> {
				if (level.getBlockEntity(pos) instanceof StargateDialerBlockEntity d) d.dial(sp, id == BUTTON_HOME ? -1 : id - BUTTON_PLANET, false);
			});
			sp.closeContainer();
			return true;
		}
		if (id == BUTTON_CLOSE || id == BUTTON_REDSTONE) {
			access.execute((level, pos) -> {
				if (level.getBlockEntity(pos) instanceof StargateDialerBlockEntity d) {
					if (id == BUTTON_CLOSE) {
						if (d.isGateOpen()) d.close();
						else d.closeIncoming(player instanceof ServerPlayer sp2 ? sp2 : null);
					}
					else d.cycleRedstoneTarget(player);
				}
			});
			return true;
		}
		return super.clickMenuButton(player, id);
	}

	@Override
	public boolean stillValid(Player player) {
		return access.evaluate((level, pos) -> level.getBlockEntity(pos) instanceof StargateDialerBlockEntity
				&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
	}
}
