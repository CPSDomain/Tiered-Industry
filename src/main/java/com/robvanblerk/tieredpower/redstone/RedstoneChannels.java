package com.robvanblerk.tieredpower.redstone;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.GlobalPos;

/**
 * Wireless redstone: what each loaded transmitter is sending on each channel. Transmitters report every couple of
 * ticks; a report older than half a second no longer counts (the transmitter was broken or its chunk unloaded).
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.robvanblerk.tieredpower.TieredPower.MOD_ID)
public final class RedstoneChannels {
	public static final int CHANNELS = 64;
	private static final int STALE = 10;

	private record Report(int strength, long time) {}

	private static final Map<Integer, Map<GlobalPos, Report>> CH = new HashMap<>();

	private RedstoneChannels() {}

	public static void report(int channel, GlobalPos from, int strength, long now) {
		CH.computeIfAbsent(channel, k -> new HashMap<>()).put(from, new Report(strength, now));
	}

	public static void remove(int channel, GlobalPos from) {
		Map<GlobalPos, Report> m = CH.get(channel);
		if (m != null) m.remove(from);
	}

	/** The strongest signal on a channel right now (0-15). */
	public static int strength(int channel, long now) {
		Map<GlobalPos, Report> m = CH.get(channel);
		if (m == null) return 0;
		int best = 0;
		var it = m.values().iterator();
		while (it.hasNext()) {
			Report r = it.next();
			if (now - r.time() > STALE) { it.remove(); continue; }
			best = Math.max(best, r.strength());
		}
		return best;
	}

	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
		clear();
	}

	public static void clear() {
		CH.clear();
	}
}
