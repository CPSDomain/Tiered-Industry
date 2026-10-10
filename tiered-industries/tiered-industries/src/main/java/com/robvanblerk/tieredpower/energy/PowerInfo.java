package com.robvanblerk.tieredpower.energy;

import net.minecraft.world.level.block.entity.BlockEntity;

import com.robvanblerk.tieredpower.block.entity.*;

/** What a generator is making right now, for the Multimeter. -1 if the block doesn't report it. */
public final class PowerInfo {
	public static int generating(BlockEntity be) {
		if (be instanceof SolarPanelBlockEntity s) return s.getGenerating();
		if (be instanceof GeothermalGeneratorBlockEntity g) return g.getGenerating();
		if (be instanceof AntimatterReactorBlockEntity a) return a.getGenerating();
		if (be instanceof com.robvanblerk.tieredpower.industry.DieselGeneratorBlockEntity dg) return dg.getGenerating();
		if (be instanceof ReceiverDishBlockEntity d) return d.getGenerating();
		if (be instanceof WaterWheelBlockEntity w) return w.getGenerating();
		if (be instanceof WindTurbineBlockEntity w) return w.getGenerating();
		if (be instanceof GasBurnerGeneratorBlockEntity g) return g.getGenerating();
		if (be instanceof SteamEngineBlockEntity e) return e.getGenerating();
		if (be instanceof FissionReactorBlockEntity f) return f.getGenerating();
		if (be instanceof FissionControllerBlockEntity c) return c.isFormed() ? c.getGenerating() : 0;
		if (be instanceof FusionControllerBlockEntity u) return u.getGenerated();
		return -1;
	}

	/** 1234 -> "1,234", 12345 -> "12.3k", 4500000 -> "4.50M". */
	public static String shortFe(long fe) {
		if (fe < 10_000) return String.format("%,d", fe);
		if (fe < 1_000_000) return String.format("%.1fk", fe / 1_000.0);
		if (fe < 1_000_000_000) return String.format("%.2fM", fe / 1_000_000.0);
		if (fe < 1_000_000_000_000L) return String.format("%.2fG", fe / 1_000_000_000.0);
		if (fe < 1_000_000_000_000_000L) return String.format("%.2fT", fe / 1_000_000_000_000.0);
		return "\u221E"; // infinity
	}

	private PowerInfo() {}
}
