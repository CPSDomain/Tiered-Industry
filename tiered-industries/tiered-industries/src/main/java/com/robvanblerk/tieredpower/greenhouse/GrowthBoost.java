package com.robvanblerk.tieredpower.greenhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;

/** Extra growth for the plants under a Sprinkler or Grow Lamp, and keeping farmland wet. */
public final class GrowthBoost {
	public static final int RADIUS = 4, DEPTH = 6;

	private static boolean isPlant(BlockState s) {
		var b = s.getBlock();
		if (b instanceof GrassBlock) return false; // don't spread grass about
		return b instanceof IPlantable || b instanceof BonemealableBlock && s.isRandomlyTicking();
	}

	/** Gives each plant within RADIUS around and up to DEPTH below 'from' a 'chance' of an extra growth tick. Returns plants seen. */
	public static int boost(ServerLevel level, BlockPos from, float chance) {
		int plants = 0;
		for (int dx = -RADIUS; dx <= RADIUS; dx++)
			for (int dz = -RADIUS; dz <= RADIUS; dz++)
				for (int dy = 1; dy <= DEPTH; dy++) {
					BlockPos p = from.offset(dx, -dy, dz);
					BlockState s = level.getBlockState(p);
					if (!isPlant(s) || !s.isRandomlyTicking()) continue;
					plants++;
					if (level.random.nextFloat() < chance) s.randomTick(level, p, level.random);
				}
		return plants;
	}

	/** Wets every farmland block in the area. */
	public static void hydrate(ServerLevel level, BlockPos from) {
		for (int dx = -RADIUS; dx <= RADIUS; dx++)
			for (int dz = -RADIUS; dz <= RADIUS; dz++)
				for (int dy = 1; dy <= DEPTH; dy++) {
					BlockPos p = from.offset(dx, -dy, dz);
					BlockState s = level.getBlockState(p);
					if (s.getBlock() instanceof FarmBlock && s.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE)
						level.setBlock(p, s.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), 2);
				}
	}

	private GrowthBoost() {}
}
