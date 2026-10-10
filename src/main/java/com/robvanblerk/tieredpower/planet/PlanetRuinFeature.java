package com.robvanblerk.tieredpower.planet;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.stargate.Planet;

/**
 * Planet bases: old ruins and outposts built in each planet's own style. Ruins (7x7, broken walls) hold a loot chest;
 * outposts (11x11, two rooms and a partial roof) hold two richer chests and a Guardian Altar - right-click it to call
 * the planet's guardian, an optional boss fight. Placed by the planet_ruin feature on planets only.
 */
public class PlanetRuinFeature extends Feature<NoneFeatureConfiguration> {
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, TieredPower.MOD_ID);
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> PLANET_RUIN = FEATURES.register("planet_ruin", () -> new PlanetRuinFeature(NoneFeatureConfiguration.CODEC));

	/** Floor, wall, accent (cracked / decorative) and light for each planet. */
	private record Palette(BlockState floor, BlockState wall, BlockState accent, BlockState light) {}

	public PlanetRuinFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	private static Palette palette(Planet p) {
		return switch (p) {
			case FROST -> new Palette(Blocks.PACKED_ICE.defaultBlockState(), Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.BLUE_ICE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState());
			case DUNE -> new Palette(Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CHISELED_RED_SANDSTONE.defaultBlockState(), Blocks.LANTERN.defaultBlockState());
			case VERDANT -> new Palette(Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), Blocks.LANTERN.defaultBlockState());
			case INFERNO -> new Palette(Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.RED_NETHER_BRICKS.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState());
			case ABYSS -> new Palette(Blocks.DARK_PRISMARINE.defaultBlockState(), Blocks.PRISMARINE_BRICKS.defaultBlockState(), Blocks.PRISMARINE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState());
			case SKYLANDS -> new Palette(Blocks.SMOOTH_QUARTZ.defaultBlockState(), Blocks.QUARTZ_BRICKS.defaultBlockState(), Blocks.CHERRY_PLANKS.defaultBlockState(), Blocks.END_ROD.defaultBlockState());
			case MYCELIA -> new Palette(Blocks.PACKED_MUD.defaultBlockState(), Blocks.MUD_BRICKS.defaultBlockState(), Blocks.MUSHROOM_STEM.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState());
			case CRYSTAL -> new Palette(Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(), Blocks.AMETHYST_BLOCK.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState());
			case EDEN -> new Palette(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), Blocks.LANTERN.defaultBlockState());
			case KAROO -> new Palette(Blocks.TERRACOTTA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(), Blocks.ACACIA_PLANKS.defaultBlockState(), Blocks.LANTERN.defaultBlockState());
			case REDWOOD -> new Palette(Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Blocks.LANTERN.defaultBlockState());
			case MURK -> new Palette(Blocks.MUD_BRICKS.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.MANGROVE_ROOTS.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState());
			case TITAN -> new Palette(Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Blocks.LANTERN.defaultBlockState());
			case WRAITH -> new Palette(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState());
			case UMBRA -> new Palette(Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState());
			case VOID_REACH -> new Palette(Blocks.END_STONE_BRICKS.defaultBlockState(), Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.PURPUR_PILLAR.defaultBlockState(), Blocks.END_ROD.defaultBlockState());
		};
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
		WorldGenLevel level = ctx.level();
		Planet planet = Planet.of(level.getLevel());
		if (planet == null) return false;
		RandomSource rnd = ctx.random();
		BlockPos o = ctx.origin();
		BlockPos base = planet.cave ? caveFloor(level, o, rnd) : surface(level, o);
		if (base == null) return false;
		boolean outpost = rnd.nextInt(4) == 0;
		build(level, base, palette(planet), planet, outpost, rnd);
		return true;
	}

	private static BlockPos surface(WorldGenLevel level, BlockPos o) {
		int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX(), o.getZ());
		if (y <= level.getMinBuildHeight() + 3) return null; // over the void
		return new BlockPos(o.getX(), y, o.getZ());
	}

	/** Cave worlds: look for a floor with headroom, from a random height downwards. */
	private static BlockPos caveFloor(WorldGenLevel level, BlockPos o, RandomSource rnd) {
		int min = level.getMinBuildHeight() + 6, top = Math.min(level.getMinBuildHeight() + level.getHeight() - 10, min + 110);
		int start = min + rnd.nextInt(Math.max(1, top - min));
		for (int y = start; y > min; y--) {
			BlockPos p = new BlockPos(o.getX(), y, o.getZ());
			if (!level.getBlockState(p.below()).isSolid()) continue;
			boolean room = true;
			for (int h = 0; h < 6 && room; h++) room = level.getBlockState(p.above(h)).isAir();
			if (room) return p;
		}
		return null;
	}

	private static void set(WorldGenLevel level, BlockPos p, BlockState s) {
		level.setBlock(p, s, Block.UPDATE_CLIENTS);
	}

	/** Air where there's no fluid; underwater ruins stay flooded. */
	private static void clear(WorldGenLevel level, BlockPos p) {
		BlockState here = level.getBlockState(p);
		if (here.getFluidState().isEmpty() && !here.isAir()) set(level, p, Blocks.AIR.defaultBlockState());
	}

	private static void build(WorldGenLevel level, BlockPos base, Palette pal, Planet planet, boolean outpost, RandomSource rnd) {
		int half = outpost ? 5 : 3, height = outpost ? 4 : 3;
		for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
			BlockPos f = base.offset(dx, -1, dz);
			set(level, f, rnd.nextInt(6) == 0 ? pal.accent() : pal.floor());
			// a foundation down to the ground so nothing floats
			for (int d = 1; d <= 6; d++) {
				BlockState below = level.getBlockState(f.below(d));
				if (below.isSolid()) break;
				set(level, f.below(d), pal.wall());
			}
			for (int h = 0; h <= height + 1; h++) clear(level, base.offset(dx, h, dz));
			boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
			boolean door = dz == -half && Math.abs(dx) <= (outpost ? 1 : 0);
			boolean inner = outpost && dx == 0 && Math.abs(dz) < half && Math.abs(dz) > 1;
			if ((edge && !door) || inner) {
				int h = outpost ? height : rnd.nextInt(height + 1); // ruins are broken down
				boolean corner = Math.abs(dx) == half && Math.abs(dz) == half;
				if (corner) h = height;
				for (int y = 0; y < h; y++) set(level, base.offset(dx, y, dz), rnd.nextInt(5) == 0 ? pal.accent() : pal.wall());
				if (corner) set(level, base.offset(dx, h, dz), pal.light());
			}
			// outposts have a partly fallen-in roof
			if (outpost && rnd.nextInt(10) < 6) set(level, base.offset(dx, height, dz), pal.wall());
			if (!edge && !inner && rnd.nextInt(30) == 0) set(level, base.offset(dx, 0, dz), Blocks.COBWEB.defaultBlockState());
		}
		String table = outpost ? "chests/planet_outpost_" + planet.id : "chests/planet_ruin_" + planet.id;
		if (outpost) {
			chest(level, base.offset(-half + 1, 0, half - 1), table, rnd);
			chest(level, base.offset(half - 1, 0, half - 1), table, rnd);
			set(level, base.offset(-2, 0, 0), ModBlocks.GUARDIAN_ALTAR.get().defaultBlockState());
			set(level, base.offset(2, 0, 0), pal.light());
		} else {
			chest(level, base.offset(rnd.nextInt(3) - 1, 0, rnd.nextInt(2)), table, rnd);
		}
	}

	private static void chest(WorldGenLevel level, BlockPos p, String table, RandomSource rnd) {
		boolean wet = !level.getFluidState(p).isEmpty() && level.getFluidState(p).is(Fluids.WATER);
		set(level, p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(rnd))
				.setValue(ChestBlock.WATERLOGGED, wet));
		RandomizableContainerBlockEntity.setLootTable(level, rnd, p, ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, table));
	}
}
