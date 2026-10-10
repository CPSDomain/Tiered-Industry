package com.robvanblerk.tieredpower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Presses items: ingots into plates, coal blocks into diamonds, and more. Recipes are data files. */
public class CompressorBlockEntity extends ProcessingMachineBlockEntity {
	public static final int ENERGY_PER_TICK = 50;

	public CompressorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.COMPRESSOR.get(), pos, state, MachineRecipe.Kind.COMPRESSING, ENERGY_PER_TICK, 0);
	}

	@Override
	protected String translationKey() {
		return "block.tieredpower.compressor";
	}

	@Override
	protected MenuType<?> menuType() {
		return ModMenus.COMPRESSOR.get();
	}
}
