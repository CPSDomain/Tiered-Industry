package com.robvanblerk.tieredpower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Washes ores and raw ores with water into 3 dusts (ore tripling). Uses 250 mB water per operation. */
public class OrePurifierBlockEntity extends ProcessingMachineBlockEntity {
	public static final int ENERGY_PER_TICK = 60;

	public OrePurifierBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ORE_PURIFIER.get(), pos, state, MachineRecipe.Kind.PURIFYING, ENERGY_PER_TICK, 250);
	}

	@Override
	protected String translationKey() {
		return "block.tieredpower.ore_purifier";
	}

	@Override
	protected MenuType<?> menuType() {
		return ModMenus.ORE_PURIFIER.get();
	}
}
