package com.robvanblerk.tieredpower.stargate;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;

/** Placement filter: only places a feature on a Stargate planet (so planet ore veins don't change the Overworld or Nether). */
public class PlanetOnlyFilter extends PlacementFilter {
	public static final PlanetOnlyFilter INSTANCE = new PlanetOnlyFilter("");
	/** {"type": "tieredpower:planet_only"} for any planet, or with "planet": "inferno" for just one. */
	public static final Codec<PlanetOnlyFilter> CODEC = Codec.STRING.optionalFieldOf("planet", "").xmap(PlanetOnlyFilter::new, f -> f.planet).codec();

	private final String planet;

	public PlanetOnlyFilter(String planet) {
		this.planet = planet;
	}

	public static final DeferredRegister<PlacementModifierType<?>> TYPES = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, TieredPower.MOD_ID);
	public static final RegistryObject<PlacementModifierType<PlanetOnlyFilter>> TYPE = TYPES.register("planet_only", () -> () -> CODEC);

	@Override
	protected boolean shouldPlace(PlacementContext ctx, RandomSource random, BlockPos pos) {
		Planet p = Planet.of(ctx.getLevel().getLevel());
		return p != null && (planet.isEmpty() || p.id.equals(planet));
	}

	@Override
	public PlacementModifierType<?> type() {
		return TYPE.get();
	}
}
