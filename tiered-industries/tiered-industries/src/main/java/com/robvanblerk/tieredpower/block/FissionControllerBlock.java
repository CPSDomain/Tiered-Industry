package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Brain of the multiblock Fission Reactor. Build it into a side wall, front facing out. */
public class FissionControllerBlock extends MachineBlock {
	public FissionControllerBlock(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FissionControllerBlockEntity controller) {
			var message = controller.revalidate();
			if (!controller.isFormed()) player.displayClientMessage(message, false);
		}
		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FissionControllerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FISSION_CONTROLLER.get(), FissionControllerBlockEntity::tick);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent runningSound() {
		return net.minecraft.sounds.SoundEvents.BEACON_AMBIENT;
	}

	@Override
	protected float soundChance() {
		return 0.08f;
	}

	@Override
	protected float soundVolume() {
		return 0.5f;
	}

	@Override
	protected float soundPitch() {
		return 0.7f;
	}

	/** Comparator: reactor heat, 0-15 (SCRAM happens at 15). */
	@Override
	@SuppressWarnings("deprecation")
	public boolean hasAnalogOutputSignal(net.minecraft.world.level.block.state.BlockState state) {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity r)) return 0;
		return Math.min(15, r.getHeat() * 15 / com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity.MAX_HEAT);
	}
}
