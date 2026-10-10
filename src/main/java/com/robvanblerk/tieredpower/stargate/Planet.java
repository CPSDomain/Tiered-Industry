package com.robvanblerk.tieredpower.stargate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.TieredPower;

/** The worlds a Stargate can reach. Each is a dimension (data/tieredpower/dimension) that generates as you explore. */
public enum Planet {
	FROST("frost", "Frost", 0, 0x9FD8FF, "Ice plains, ice spikes and frozen peaks. Only here: Cryonite (glowing ice crystals)."),
	DUNE("dune", "Dune", 0, 0xE6C27A, "Endless desert and red badlands. Only here: Solarite Sand, which smelts into Solar Glass."),
	VERDANT("verdant", "Verdant", 0, 0x55C24A, "Jungle, bamboo forest and mangrove swamp. Only here: Livingwood trees."),
	INFERNO("inferno", "Inferno", 64, 0xE0502A, "A volcanic underworld of lava seas and basalt. Only here: Naquadah ore."),
	ABYSS("abyss", "Abyss", 0, 0x2A7FD0, "A planet-wide warm ocean with reefs, wrecks and monuments. Only here: Abyssal Pearls on the sea bed."),
	SKYLANDS("skylands", "Skylands", 0, 0xF0A8D8, "Floating islands of cherry groves and meadows. Only here: Aether Crystals."),
	MYCELIA("mycelia", "Mycelia", 0, 0xB07AA8, "Mushroom fields and dark forests. Only here: glowing Sporecaps."),
	CRYSTAL("crystal", "Crystal Depths", 40, 0x9C6CFF, "A sealed cave world of lush caves and dripstone. Only here: Resonance Crystals."),
	// Hidden worlds: their addresses have to be found (Stargate Address tablets in chests around the world).
	EDEN("eden", "Eden", 0, 0x8FD46A, "Gentle plains, flower meadows and birch woods, dotted with villages. A peaceful place for a second home. Only here: Lifebloom flowers.", false, 0),
	KAROO("karoo", "Karoo", 0, 0xC9A14A, "Dry savanna and high plateaus under a huge sky, with savanna villages and wandering herds. Only here: Sunstone ore."),
	REDWOOD("redwood", "Redwood", 0, 0x6E8F4A, "Giant old-growth spruce and pine forests - endless timber, mossy boulders and berry bushes. Only here: Amber ore."),
	MURK("murk", "Murk", 0, 0x4F6B4A, "Swamps, mangroves and dark woods under an eternal dusk. Witch huts, slimes and frogs - and the night never ends. Only here: Witchroot."),
	TITAN("titan", "Titan", 0, 0xA9B4C2, "Towering amplified mountains and deep valleys, with peaks far above the clouds. Rich in emerald and iron. Only here: Gravitite ore."),
	WRAITH("wraith", "Wraith", 64, 0x4FC9C2, "A haunted underworld of soul sand valleys, warped forests and basalt - fortresses and bastions included. Only here: Soul Crystal ore."),
	UMBRA("umbra", "Umbra", 40, 0x1F3A4A, "A world of the deep dark: sculk-choked caves and ancient cities. Tread quietly - the Warden listens. Only here: Umbral ore."),
	VOID_REACH("void_reach", "Void Reach", 0, 0xD9D3A0, "The outer End islands: chorus forests, end cities and shulkers. Bring blocks to bridge the void - and come back with an elytra. Only here: Voidstone ore.", false, 1500);

	public final String id, title, description;
	/** Cave worlds have a roof: arrivals are carved into the rock at this height (0 = on the surface). */
	public final int caveY;
	public final boolean cave;
	/** The first eight are known from the start; the rest need their address found first. */
	public final boolean known;
	/** Where the landing platform goes (X; Z is always 0). */
	public final int originX;
	public final int colour;
	public final ResourceKey<Level> level;

	Planet(String id, String title, int caveY, int colour, String description) {
		this(id, title, caveY, colour, description, null, 0);
	}

	Planet(String id, String title, int caveY, int colour, String description, Boolean known, int originX) {
		this.id = id;
		this.title = title;
		this.caveY = caveY;
		this.cave = caveY != 0;
		this.colour = colour;
		this.description = description;
		this.known = known != null ? known : ordinal() < 8;
		this.originX = originX;
		this.level = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, id));
	}

	/** The planets whose addresses must be found. */
	public static java.util.List<Planet> hidden() {
		java.util.List<Planet> out = new java.util.ArrayList<>();
		for (Planet p : values()) if (!p.known) out.add(p);
		return out;
	}

	public static @Nullable Planet byId(String id) {
		for (Planet p : values()) if (p.id.equals(id)) return p;
		return null;
	}

	public static @Nullable Planet of(Level level) {
		for (Planet p : values()) if (p.level.equals(level.dimension())) return p;
		return null;
	}
}
