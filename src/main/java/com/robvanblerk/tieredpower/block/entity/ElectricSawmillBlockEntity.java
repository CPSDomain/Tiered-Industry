package com.robvanblerk.tieredpower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Cuts logs into 6 planks plus sawdust, and planks into sticks. */
public class ElectricSawmillBlockEntity extends ProcessingMachineBlockEntity {
	public static final int ENERGY_PER_TICK = 30;

	public ElectricSawmillBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ELECTRIC_SAWMILL.get(), pos, state, MachineRecipe.Kind.SAWING, ENERGY_PER_TICK, 0);
	}

	@Override
	protected String translationKey() {
		return "block.tieredpower.electric_sawmill";
	}

	@Override
	protected MenuType<?> menuType() {
		return ModMenus.ELECTRIC_SAWMILL.get();
	}
}
