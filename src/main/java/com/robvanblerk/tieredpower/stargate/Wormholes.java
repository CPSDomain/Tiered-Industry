package com.robvanblerk.tieredpower.stargate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

/**
 * Open connections between two gates (each gate known by its ring's bottom-middle block). Server-side and in memory only: an open gate closes when the world is reloaded anyway.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.robvanblerk.tieredpower.TieredPower.MOD_ID)
public final class Wormholes {
	private static final Map<GlobalPos, GlobalPos> LINKS = new HashMap<>();
	/** gate -> the dialler running its connection (at either end). */
	private static final Map<GlobalPos, GlobalPos> OWNERS = new HashMap<>();

	public static void setOwner(GlobalPos a, GlobalPos b, GlobalPos dialler) {
		OWNERS.put(a, dialler);
		OWNERS.put(b, dialler);
	}

	public static @Nullable GlobalPos owner(GlobalPos gate) {
		return OWNERS.get(gate);
	}

	private Wormholes() {}

	public static void connect(GlobalPos a, GlobalPos b) {
		LINKS.put(a, b);
		LINKS.put(b, a);
	}

	public static void disconnect(GlobalPos a) {
		GlobalPos b = LINKS.remove(a);
		OWNERS.remove(a);
		if (b != null && a.equals(LINKS.get(b))) { LINKS.remove(b); OWNERS.remove(b); }
	}

	/** The gate at the other end of an open connection, or null. */
	public static @Nullable GlobalPos other(GlobalPos gate) {
		return LINKS.get(gate);
	}

	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
		clear();
	}

	public static void clear() {
		LINKS.clear();
		OWNERS.clear();
	}
}
