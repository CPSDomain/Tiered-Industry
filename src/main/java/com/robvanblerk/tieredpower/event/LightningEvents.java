package com.robvanblerk.tieredpower.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.entity.LightningCollectorBlockEntity;

/**
 * Feeds lightning into Lightning Collectors. A bolt that lands on a vanilla Lightning Rod charges the collector the rod
 * is attached to; a bolt that lands right on a collector charges it too. (Lightning Rods already attract strikes from
 * up to 128 blocks away during a thunderstorm.)
 */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LightningEvents {
	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LightningBolt bolt)) return;
		Level level = event.getLevel();
		// The block the bolt strikes: the one just under its position (rod strikes land on top of the rod).
		BlockPos hit = BlockPos.containing(bolt.position().subtract(0, 1.0E-6, 0));
		BlockState state = level.getBlockState(hit);
		BlockPos collector = null;
		if (state.getBlock() instanceof LightningRodBlock) collector = hit.relative(state.getValue(LightningRodBlock.FACING).getOpposite());
		else if (level.getBlockEntity(hit) instanceof LightningCollectorBlockEntity) collector = hit;
		if (collector != null && level.getBlockEntity(collector) instanceof LightningCollectorBlockEntity c) c.strike();
	}

	private LightningEvents() {}
}
