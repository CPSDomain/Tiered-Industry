package com.robvanblerk.tieredpower.greenhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Irrigated Sand: sand for greenhouses. Sugar cane grows on it without a water block beside it as long as a Sprinkler
 * with water in it covers it (within 4 blocks sideways, up to 8 above) - or with water beside it, like normal sand.
 * Cactus grows on it too. It doesn't fall like sand.
 */
public class IrrigatedSandBlock extends Block {
	public static final int REACH = 4, HEIGHT = 8;

	public IrrigatedSandBlock(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, IPlantable plantable) {
		PlantType type = plantable.getPlantType(level, pos.relative(facing));
		if (type == PlantType.DESERT) return true;
		if (type == PlantType.BEACH) return watered(level, pos);
		return super.canSustainPlant(state, level, pos, facing, plantable);
	}

	/** Water right beside it, or a running Sprinkler overhead. */
	public static boolean watered(BlockGetter level, BlockPos pos) {
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) return true;
		for (int dy = 1; dy <= HEIGHT; dy++)
			for (int dx = -REACH; dx <= REACH; dx++)
				for (int dz = -REACH; dz <= REACH; dz++) {
					BlockPos p = pos.offset(dx, dy, dz);
					if (level.getBlockState(p).is(ModBlocks.SPRINKLER.get())
							&& (!(level.getBlockEntity(p) instanceof GreenhouseBlockEntity g) || g.hasWater())) return true;
				}
		return false;
	}
}
