package com.robvanblerk.tieredpower.registry;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;

/**
 * Gases (fluids lighter than air): Steam, Hydrogen and Oxygen. Steam as a real gas (a fluid lighter than air), so it can travel through Gas Pipes from Boilers to Turbines.
 * It has no bucket or world block - it only exists inside machines, tanks and pipes.
 */
public final class ModFluids {
	public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, TieredPower.MOD_ID);
	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, TieredPower.MOD_ID);

	/** A gas: a fluid lighter than air, drawn with the water texture tinted this colour. No bucket or world block. */
	private static RegistryObject<FluidType> gasType(String name, int tint) {
		return TYPES.register(name, () -> new FluidType(FluidType.Properties.create().descriptionId("fluid_type.tieredpower." + name)
				.density(-1000).viscosity(200).temperature(name.equals("steam") ? 373 : 300)) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
				consumer.accept(new IClientFluidTypeExtensions() {
					private final ResourceLocation still = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
					private final ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");

					@Override
					public ResourceLocation getStillTexture() {
						return still;
					}

					@Override
					public ResourceLocation getFlowingTexture() {
						return flowing;
					}

					@Override
					public int getTintColor() {
						return tint;
					}
				});
			}
		});
	}

	/** A liquid (goes in Fluid Pipes, not Gas Pipes): the water texture tinted this colour. No bucket or world block. */
	private static RegistryObject<FluidType> liquidType(String name, int tint, int temperature) {
		return TYPES.register(name, () -> new FluidType(FluidType.Properties.create().descriptionId("fluid_type.tieredpower." + name)
				.density(1000).viscosity(800).temperature(temperature)) {
			@Override
			public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
				consumer.accept(new IClientFluidTypeExtensions() {
					private final ResourceLocation still = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_still");
					private final ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");
					@Override public ResourceLocation getStillTexture() { return still; }
					@Override public ResourceLocation getFlowingTexture() { return flowing; }
					@Override public int getTintColor() { return tint; }
				});
			}
		});
	}

	public static final RegistryObject<FluidType> LIQUID_METHANE_TYPE = liquidType("liquid_methane", 0xEE9FD07A, 111);
	public static final RegistryObject<ForgeFlowingFluid.Source> LIQUID_METHANE = FLUIDS.register("liquid_methane", () -> new ForgeFlowingFluid.Source(liquidMethane()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_LIQUID_METHANE = FLUIDS.register("flowing_liquid_methane", () -> new ForgeFlowingFluid.Flowing(liquidMethane()));
	public static final RegistryObject<FluidType> LIQUID_OXYGEN_TYPE = liquidType("liquid_oxygen", 0xEE7FB8FF, 90);
	public static final RegistryObject<ForgeFlowingFluid.Source> LIQUID_OXYGEN = FLUIDS.register("liquid_oxygen", () -> new ForgeFlowingFluid.Source(liquidOxygen()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_LIQUID_OXYGEN = FLUIDS.register("flowing_liquid_oxygen", () -> new ForgeFlowingFluid.Flowing(liquidOxygen()));
	public static final RegistryObject<FluidType> ROCKET_FUEL_TYPE = liquidType("rocket_fuel", 0xEEE8B040, 100);
	public static final RegistryObject<ForgeFlowingFluid.Source> ROCKET_FUEL = FLUIDS.register("rocket_fuel", () -> new ForgeFlowingFluid.Source(rocketFuel()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_ROCKET_FUEL = FLUIDS.register("flowing_rocket_fuel", () -> new ForgeFlowingFluid.Flowing(rocketFuel()));

	public static final RegistryObject<FluidType> CREOSOTE_TYPE = liquidType("creosote", 0xFF3A2A12, 300);
	public static final RegistryObject<ForgeFlowingFluid.Source> CREOSOTE = FLUIDS.register("creosote", () -> new ForgeFlowingFluid.Source(creosote()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_CREOSOTE = FLUIDS.register("flowing_creosote", () -> new ForgeFlowingFluid.Flowing(creosote()));

	public static final RegistryObject<FluidType> BIODIESEL_TYPE = liquidType("biodiesel", 0xEED8A020, 300);
	public static final RegistryObject<ForgeFlowingFluid.Source> BIODIESEL = FLUIDS.register("biodiesel", () -> new ForgeFlowingFluid.Source(biodiesel()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_BIODIESEL = FLUIDS.register("flowing_biodiesel", () -> new ForgeFlowingFluid.Flowing(biodiesel()));

	/** Liquid Experience: 20 mB = 1 experience point. */
	public static final int MB_PER_XP = 20;
	public static final RegistryObject<FluidType> EXPERIENCE_TYPE = liquidType("experience", 0xEE8EF04A, 300);
	public static final RegistryObject<ForgeFlowingFluid.Source> EXPERIENCE = FLUIDS.register("experience", () -> new ForgeFlowingFluid.Source(experience()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_EXPERIENCE = FLUIDS.register("flowing_experience", () -> new ForgeFlowingFluid.Flowing(experience()));

	private static ForgeFlowingFluid.Properties experience() {
		return new ForgeFlowingFluid.Properties(EXPERIENCE_TYPE, EXPERIENCE, FLOWING_EXPERIENCE)
				.bucket(com.robvanblerk.tieredpower.registry.ModBlocks.EXPERIENCE_BUCKET).block(com.robvanblerk.tieredpower.registry.ModBlocks.EXPERIENCE_BLOCK);
	}

	private static ForgeFlowingFluid.Properties biodiesel() {
		return new ForgeFlowingFluid.Properties(BIODIESEL_TYPE, BIODIESEL, FLOWING_BIODIESEL)
				.bucket(com.robvanblerk.tieredpower.registry.ModBlocks.BIODIESEL_BUCKET).block(com.robvanblerk.tieredpower.registry.ModBlocks.BIODIESEL_BLOCK);
	}

	private static ForgeFlowingFluid.Properties creosote() {
		return new ForgeFlowingFluid.Properties(CREOSOTE_TYPE, CREOSOTE, FLOWING_CREOSOTE)
				.bucket(com.robvanblerk.tieredpower.registry.ModBlocks.CREOSOTE_BUCKET).block(com.robvanblerk.tieredpower.registry.ModBlocks.CREOSOTE_BLOCK);
	}

	private static ForgeFlowingFluid.Properties liquidMethane() {
		return new ForgeFlowingFluid.Properties(LIQUID_METHANE_TYPE, LIQUID_METHANE, FLOWING_LIQUID_METHANE)
				.bucket(com.robvanblerk.tieredpower.registry.ModBlocks.LIQUID_METHANE_BUCKET).block(com.robvanblerk.tieredpower.registry.ModBlocks.LIQUID_METHANE_BLOCK);
	}
	private static ForgeFlowingFluid.Properties liquidOxygen() {
		return new ForgeFlowingFluid.Properties(LIQUID_OXYGEN_TYPE, LIQUID_OXYGEN, FLOWING_LIQUID_OXYGEN)
				.bucket(com.robvanblerk.tieredpower.registry.ModBlocks.LIQUID_OXYGEN_BUCKET).block(com.robvanblerk.tieredpower.registry.ModBlocks.LIQUID_OXYGEN_BLOCK);
	}
	private static ForgeFlowingFluid.Properties rocketFuel() {
		return new ForgeFlowingFluid.Properties(ROCKET_FUEL_TYPE, ROCKET_FUEL, FLOWING_ROCKET_FUEL)
				.bucket(com.robvanblerk.tieredpower.registry.ModBlocks.ROCKET_FUEL_BUCKET).block(com.robvanblerk.tieredpower.registry.ModBlocks.ROCKET_FUEL_BLOCK);
	}

	public static final RegistryObject<FluidType> STEAM_TYPE = gasType("steam", 0xCCE8E8EC);
	public static final RegistryObject<ForgeFlowingFluid.Source> STEAM = FLUIDS.register("steam", () -> new ForgeFlowingFluid.Source(steam()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_STEAM = FLUIDS.register("flowing_steam", () -> new ForgeFlowingFluid.Flowing(steam()));

	public static final RegistryObject<FluidType> HYDROGEN_TYPE = gasType("hydrogen", 0xCCF0F4FF);
	public static final RegistryObject<ForgeFlowingFluid.Source> HYDROGEN = FLUIDS.register("hydrogen", () -> new ForgeFlowingFluid.Source(hydrogen()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_HYDROGEN = FLUIDS.register("flowing_hydrogen", () -> new ForgeFlowingFluid.Flowing(hydrogen()));

	public static final RegistryObject<FluidType> OXYGEN_TYPE = gasType("oxygen", 0xCC9FD8FF);
	public static final RegistryObject<ForgeFlowingFluid.Source> OXYGEN = FLUIDS.register("oxygen", () -> new ForgeFlowingFluid.Source(oxygen()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_OXYGEN = FLUIDS.register("flowing_oxygen", () -> new ForgeFlowingFluid.Flowing(oxygen()));

	public static final RegistryObject<FluidType> DEUTERIUM_TYPE = gasType("deuterium", 0xCCC8E0FF);
	public static final RegistryObject<ForgeFlowingFluid.Source> DEUTERIUM = FLUIDS.register("deuterium", () -> new ForgeFlowingFluid.Source(deuterium()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_DEUTERIUM = FLUIDS.register("flowing_deuterium", () -> new ForgeFlowingFluid.Flowing(deuterium()));

	public static final RegistryObject<FluidType> TRITIUM_TYPE = gasType("tritium", 0xCCB8FFC8);
	public static final RegistryObject<ForgeFlowingFluid.Source> TRITIUM = FLUIDS.register("tritium", () -> new ForgeFlowingFluid.Source(tritium()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_TRITIUM = FLUIDS.register("flowing_tritium", () -> new ForgeFlowingFluid.Flowing(tritium()));

	public static final RegistryObject<FluidType> NITROGEN_TYPE = gasType("nitrogen", 0xCCDCE4F0);
	public static final RegistryObject<ForgeFlowingFluid.Source> NITROGEN = FLUIDS.register("nitrogen", () -> new ForgeFlowingFluid.Source(nitrogen()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_NITROGEN = FLUIDS.register("flowing_nitrogen", () -> new ForgeFlowingFluid.Flowing(nitrogen()));

	private static ForgeFlowingFluid.Properties deuterium() {
		return new ForgeFlowingFluid.Properties(DEUTERIUM_TYPE, DEUTERIUM, FLOWING_DEUTERIUM);
	}

	private static ForgeFlowingFluid.Properties tritium() {
		return new ForgeFlowingFluid.Properties(TRITIUM_TYPE, TRITIUM, FLOWING_TRITIUM);
	}

	private static ForgeFlowingFluid.Properties nitrogen() {
		return new ForgeFlowingFluid.Properties(NITROGEN_TYPE, NITROGEN, FLOWING_NITROGEN);
	}

	public static final RegistryObject<FluidType> METHANE_TYPE = gasType("methane", 0xCCB8D890);
	public static final RegistryObject<ForgeFlowingFluid.Source> METHANE = FLUIDS.register("methane", () -> new ForgeFlowingFluid.Source(methane()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_METHANE = FLUIDS.register("flowing_methane", () -> new ForgeFlowingFluid.Flowing(methane()));

	private static ForgeFlowingFluid.Properties methane() {
		return new ForgeFlowingFluid.Properties(METHANE_TYPE, METHANE, FLOWING_METHANE);
	}

	public static final RegistryObject<FluidType> CHLORINE_TYPE = gasType("chlorine", 0xCCD8E870);
	public static final RegistryObject<ForgeFlowingFluid.Source> CHLORINE = FLUIDS.register("chlorine", () -> new ForgeFlowingFluid.Source(chlorine()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_CHLORINE = FLUIDS.register("flowing_chlorine", () -> new ForgeFlowingFluid.Flowing(chlorine()));

	private static ForgeFlowingFluid.Properties chlorine() {
		return new ForgeFlowingFluid.Properties(CHLORINE_TYPE, CHLORINE, FLOWING_CHLORINE);
	}

	public static final RegistryObject<FluidType> ANTIMATTER_TYPE = gasType("antimatter", 0xCCFF60D0);
	public static final RegistryObject<ForgeFlowingFluid.Source> ANTIMATTER = FLUIDS.register("antimatter", () -> new ForgeFlowingFluid.Source(antimatter()));
	public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING_ANTIMATTER = FLUIDS.register("flowing_antimatter", () -> new ForgeFlowingFluid.Flowing(antimatter()));

	private static ForgeFlowingFluid.Properties antimatter() {
		return new ForgeFlowingFluid.Properties(ANTIMATTER_TYPE, ANTIMATTER, FLOWING_ANTIMATTER);
	}

	private static ForgeFlowingFluid.Properties steam() {
		return new ForgeFlowingFluid.Properties(STEAM_TYPE, STEAM, FLOWING_STEAM);
	}

	private static ForgeFlowingFluid.Properties hydrogen() {
		return new ForgeFlowingFluid.Properties(HYDROGEN_TYPE, HYDROGEN, FLOWING_HYDROGEN);
	}

	private static ForgeFlowingFluid.Properties oxygen() {
		return new ForgeFlowingFluid.Properties(OXYGEN_TYPE, OXYGEN, FLOWING_OXYGEN);
	}

	private ModFluids() {}
}
