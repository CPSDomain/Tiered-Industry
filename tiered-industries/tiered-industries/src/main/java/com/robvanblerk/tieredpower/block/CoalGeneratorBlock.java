package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.CoalGeneratorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class CoalGeneratorBlock extends MachineBlock {
	public CoalGeneratorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CoalGeneratorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		// Only tick on the server; the client just shows what the server tells it.
		return level.isClientSide() ? null
				: createTickerHelper(type, ModBlockEntities.COAL_GENERATOR.get(), CoalGeneratorBlockEntity::tick);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent runningSound() {
		return net.minecraft.sounds.SoundEvents.FURNACE_FIRE_CRACKLE;
	}

	@Override
	protected boolean burnsFuel() {
		return true;
	}

	/** Smoke rising from the chimney (back-right corner, turned with the block) while it burns. */
	@Override
	public void animateTick(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
		super.animateTick(state, level, pos, random);
		if (!state.getValue(LIT) || !com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.MACHINE_PARTICLES)) return;
		// Chimney centre for a north-facing block is (+0.28, +0.28) from the middle; rotate that to the block's facing.
		double dx = 0.28, dz = 0.28;
		switch (state.getValue(FACING)) {
			case EAST -> { double t = dx; dx = -dz; dz = t; }
			case SOUTH -> { dx = -dx; dz = -dz; }
			case WEST -> { double t = dx; dx = dz; dz = -t; }
			default -> { }
		}
		if (random.nextFloat() < 0.6f) {
			level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
					pos.getX() + 0.5 + dx, pos.getY() + 1.5, pos.getZ() + 0.5 + dz, 0, 0.04, 0);
		}
	}
}
