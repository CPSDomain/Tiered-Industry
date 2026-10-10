package com.robvanblerk.tieredpower.greenhouse;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sugar Cane Seeds: plant them like sugar cane - on sand, dirt or grass with water right next to that block. Made from
 * sugar cane (one cane makes two), and the Crop Farmer plants them too.
 */
public class SugarCaneSeedsItem extends Item {
	public SugarCaneSeedsItem(Properties properties) {
		super(properties);
	}

	/** Where a seed would go when used on 'clicked' (on its top face), or null if sugar cane can't grow there. */
	public static boolean canPlantAt(Level level, BlockPos pos) {
		BlockState cane = Blocks.SUGAR_CANE.defaultBlockState();
		return level.getBlockState(pos).canBeReplaced() && cane.canSurvive(level, pos);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
		if (!canPlantAt(level, pos)) return InteractionResult.FAIL;
		if (!level.isClientSide()) {
			level.setBlock(pos, Blocks.SUGAR_CANE.defaultBlockState(), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1f, 1f);
			if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}
}
