package com.robvanblerk.tieredpower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Crushes the blocks you put in: stone -> cobblestone -> gravel -> sand, sandstone -> sand, and more. Recipes are data files. */
public class RockCrusherBlockEntity extends ProcessingMachineBlockEntity {
	public static final int ENERGY_PER_TICK = 30;

	public RockCrusherBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ROCK_CRUSHER.get(), pos, state, MachineRecipe.Kind.CRUSHING, ENERGY_PER_TICK, 0);
	}

	@Override
	protected String translationKey() {
		return "block.tieredpower.rock_crusher";
	}

	@Override
	protected MenuType<?> menuType() {
		return ModMenus.ROCK_CRUSHER.get();
	}
}
