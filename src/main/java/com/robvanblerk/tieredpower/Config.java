package com.robvanblerk.tieredpower;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * config/tieredpower-common.toml - edit it to rebalance the mod without rebuilding.
 * Changes apply after restarting the world (some, like cable and battery sizes, only to newly placed blocks).
 */
public final class Config {
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.BooleanValue MACHINE_SOUNDS;
	public static final ForgeConfigSpec.BooleanValue MACHINE_PARTICLES;
	public static final ForgeConfigSpec.BooleanValue PIPE_ITEM_VISUALS;

	public static final ForgeConfigSpec.DoubleValue GENERATION_MULTIPLIER;
	public static final ForgeConfigSpec.DoubleValue MACHINE_ENERGY_MULTIPLIER;
	public static final ForgeConfigSpec.IntValue COAL_GENERATOR_OUTPUT;
	public static final ForgeConfigSpec.IntValue TURBINE_FE_PER_STEAM;
	public static final ForgeConfigSpec.IntValue COMPACT_FUSION_OUTPUT;
	public static final ForgeConfigSpec.IntValue FUSION_BASE_OUTPUT;
	public static final ForgeConfigSpec.IntValue FUSION_OUTPUT_PER_COIL;

	public static final ForgeConfigSpec.IntValue CABLE_COPPER, CABLE_GOLD, CABLE_DIAMOND, CABLE_NETHERITE, CABLE_QUANTUM;

	public static final ForgeConfigSpec.IntValue BATTERY_BASIC_CAPACITY, BATTERY_BASIC_RATE;
	public static final ForgeConfigSpec.IntValue BATTERY_ADVANCED_CAPACITY, BATTERY_ADVANCED_RATE;
	public static final ForgeConfigSpec.IntValue BATTERY_ELITE_CAPACITY, BATTERY_ELITE_RATE;
	public static final ForgeConfigSpec.IntValue BATTERY_ULTIMATE_CAPACITY, BATTERY_ULTIMATE_RATE;
	public static final ForgeConfigSpec.IntValue BATTERY_QUANTUM_CAPACITY, BATTERY_QUANTUM_RATE;

	public static final ForgeConfigSpec.IntValue SINK_WATER_PER_TICK;
	public static final ForgeConfigSpec.IntValue QUARRY_RADIUS;
	public static final ForgeConfigSpec.IntValue FARMER_RADIUS;
	public static final ForgeConfigSpec.IntValue CHUNK_LOADER_FE_PER_CHUNK;
	public static final ForgeConfigSpec.BooleanValue FISSION_MELTDOWN;
	public static final ForgeConfigSpec.BooleanValue POWER_FROM_BOTTOM, RADIATION_ENABLED;
	public static final ForgeConfigSpec.IntValue RADIATION_STRENGTH;
	public static final ForgeConfigSpec.IntValue ANTIMATTER_FE_PER_MB, COLLIDER_FE_PER_MB, TRANSMITTER_RATE, QUANTUM_DRILL_FE_PER_BLOCK, LIGHTNING_FE_PER_STRIKE, SATELLITE_OUTPUT;

	static {
		ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

		b.comment("Sounds and visual effects").push("effects");
		MACHINE_SOUNDS = b.comment("Machines make quiet ambient sounds while running").define("machineSounds", true);
		MACHINE_PARTICLES = b.comment("Generators show smoke/flame particles while running").define("machineParticles", true);
		PIPE_ITEM_VISUALS = b.comment("Show items travelling through Item Pipes (turn off for very large networks)").define("pipeItemVisuals", true);
		b.pop();

		b.comment("Global balance knobs - scale the whole mod up or down with one number").push("balance");
		GENERATION_MULTIPLIER = b.comment("Multiplies what every Tiered Industries generator makes (1.0 = normal, 0.5 = half, 2.0 = double)")
				.defineInRange("generationMultiplier", 1.0, 0.01, 100.0);
		MACHINE_ENERGY_MULTIPLIER = b.comment("Multiplies how much power every Tiered Industries machine uses (1.0 = normal)")
				.defineInRange("machineEnergyMultiplier", 1.0, 0.0, 100.0);
		b.pop();

		b.comment("Power generation (FE per tick)").push("generation");
		COAL_GENERATOR_OUTPUT = b.defineInRange("coalGeneratorOutput", 40, 1, 1_000_000);
		TURBINE_FE_PER_STEAM = b.comment("FE made per mB of steam (turbine uses up to 100 mB/t)").defineInRange("turbineFePerSteam", 2, 1, 1_000);
		COMPACT_FUSION_OUTPUT = b.defineInRange("compactFusionOutput", 8_000, 1, 10_000_000);
		FUSION_BASE_OUTPUT = b.comment("Multiblock reactor output with no coils").defineInRange("fusionBaseOutput", 4_000, 0, 10_000_000);
		FUSION_OUTPUT_PER_COIL = b.comment("Extra multiblock reactor output per Magnet Coil").defineInRange("fusionOutputPerCoil", 1_500, 0, 10_000_000);
		b.pop();

		b.comment("Cable transfer rates (FE per tick per connection). Applies to newly placed cables.").push("cables");
		CABLE_COPPER = b.defineInRange("copper", 1_000, 1, Integer.MAX_VALUE);
		CABLE_GOLD = b.defineInRange("gold", 8_000, 1, Integer.MAX_VALUE);
		CABLE_DIAMOND = b.defineInRange("diamond", 32_000, 1, Integer.MAX_VALUE);
		CABLE_NETHERITE = b.defineInRange("netherite", 128_000, 1, Integer.MAX_VALUE);
		CABLE_QUANTUM = b.defineInRange("quantum", 2_048_000, 1, Integer.MAX_VALUE);
		b.pop();

		b.comment("Battery Boxes: capacity (FE) and transfer rate (FE per tick per face). Applies to newly placed batteries.").push("batteries");
		BATTERY_BASIC_CAPACITY = b.defineInRange("basicCapacity", 1_000_000, 1, Integer.MAX_VALUE);
		BATTERY_BASIC_RATE = b.defineInRange("basicRate", 8_000, 1, Integer.MAX_VALUE);
		BATTERY_ADVANCED_CAPACITY = b.defineInRange("advancedCapacity", 8_000_000, 1, Integer.MAX_VALUE);
		BATTERY_ADVANCED_RATE = b.defineInRange("advancedRate", 32_000, 1, Integer.MAX_VALUE);
		BATTERY_ELITE_CAPACITY = b.defineInRange("eliteCapacity", 32_000_000, 1, Integer.MAX_VALUE);
		BATTERY_ELITE_RATE = b.defineInRange("eliteRate", 128_000, 1, Integer.MAX_VALUE);
		BATTERY_ULTIMATE_CAPACITY = b.defineInRange("ultimateCapacity", 256_000_000, 1, Integer.MAX_VALUE);
		BATTERY_ULTIMATE_RATE = b.defineInRange("ultimateRate", 512_000, 1, Integer.MAX_VALUE);
		BATTERY_QUANTUM_CAPACITY = b.defineInRange("quantumCapacity", 2_000_000_000, 1, Integer.MAX_VALUE);
		BATTERY_QUANTUM_RATE = b.defineInRange("quantumRate", 2_048_000, 1, Integer.MAX_VALUE);
		b.pop();

		b.push("sink");
		SINK_WATER_PER_TICK = b.comment("mB of water the Sink pushes into each neighbouring tank/machine per tick").defineInRange("waterPerTick", 1_000, 1, 1_000_000);
		b.pop();

		b.comment("Machines").push("machines");
		POWER_FROM_BOTTOM = b.comment("Powered machines take power only through their bottom face, leaving the other faces for items.",
				"Set to false to let them take power from any side again (the Electric Pump and Quarry always do).").define("powerFromBottomOnly", true);
		b.pop();

		b.comment("Automation").push("automation");
		QUARRY_RADIUS = b.comment("Quarry mines a square this many blocks out from it in each direction (8 = 17x17)").defineInRange("quarryRadius", 8, 1, 32);
		FARMER_RADIUS = b.comment("Crop Farmer works this many blocks out from it (4 = 9x9)").defineInRange("farmerRadius", 4, 1, 16);
		CHUNK_LOADER_FE_PER_CHUNK = b.comment("FE per tick for each chunk a Chunk Loader keeps loaded").defineInRange("chunkLoaderFePerChunk", 20, 0, 100_000);
		b.pop();

		b.comment("Fission Reactor").push("fission");
		FISSION_MELTDOWN = b.comment("If true, an overheating Fission Reactor explodes. If false (default) it shuts itself down (SCRAM) instead.")
				.define("meltdownExplodes", false);
		b.pop();

		b.comment("End-game and newer systems").push("endgame");
		ANTIMATTER_FE_PER_MB = b.comment("FE an Antimatter Reactor makes from 1 mB of antimatter").defineInRange("antimatterFePerMb", 50_000, 1, 10_000_000);
		COLLIDER_FE_PER_MB = b.comment("FE a Particle Collider spends to make 1 mB of antimatter").defineInRange("colliderFePerMb", 20_000, 0, 10_000_000);
		TRANSMITTER_RATE = b.comment("FE/t a Power Transmitter can hand out, shared by its receivers").defineInRange("powerTransmitterRate", 64_000, 1, Integer.MAX_VALUE);
		QUANTUM_DRILL_FE_PER_BLOCK = b.comment("FE the Quantum Drill uses per block").defineInRange("quantumDrillFePerBlock", 400, 0, 1_000_000);
		LIGHTNING_FE_PER_STRIKE = b.comment("FE a Lightning Collector gets from one strike").defineInRange("lightningFePerStrike", 2_500_000, 1, 25_000_000);
		SATELLITE_OUTPUT = b.comment("FE/t a Receiver Dish gets from its Solar Satellite").defineInRange("satelliteOutput", 100_000, 1, 10_000_000);
		b.pop();

		b.comment("Radiation (off by default): reactors and nuclear materials irradiate players; Hazmat Suits and the Quantum Suit's shielding module protect").push("radiation");
		RADIATION_ENABLED = b.comment("Turn radiation on").define("enabled", false);
		RADIATION_STRENGTH = b.comment("How strong radiation is, in percent (100 = normal)").defineInRange("strength", 100, 1, 1000);
		b.pop();

		SPEC = b.build();
	}

	/** Applies the global generation multiplier. */
	public static int gen(int amount) {
		if (amount <= 0) return amount;
		double m;
		try {
			m = GENERATION_MULTIPLIER.get();
		} catch (IllegalStateException e) {
			m = 1.0;
		}
		return (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(amount * m)));
	}

	/** Applies the global machine energy multiplier. */
	public static int use(int amount) {
		double m;
		try {
			m = MACHINE_ENERGY_MULTIPLIER.get();
		} catch (IllegalStateException e) {
			m = 1.0;
		}
		return (int) Math.min(Integer.MAX_VALUE, Math.round(amount * m));
	}

	/** Reads a config value, falling back to the default if the config isn't loaded yet (e.g. very early startup). */
	public static int get(ForgeConfigSpec.IntValue value) {
		try {
			return value.get();
		} catch (IllegalStateException e) {
			return value.getDefault();
		}
	}

	public static boolean get(ForgeConfigSpec.BooleanValue value) {
		try {
			return value.get();
		} catch (IllegalStateException e) {
			return value.getDefault();
		}
	}

	private Config() {}
}
