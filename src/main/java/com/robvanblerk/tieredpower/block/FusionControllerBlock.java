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

import com.robvanblerk.tieredpower.block.entity.FusionControllerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Brain of the multiblock Fusion Reactor. Place it in the centre of one side face, front facing out. */
public class FusionControllerBlock extends MachineBlock {
	public FusionControllerBlock(Properties properties) {
		super(properties);
	}

	/** Right-click re-checks the structure, tells the player what's wrong if it isn't formed, then opens the GUI. */
	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FusionControllerBlockEntity controller) {
			var message = controller.revalidate();
			if (!controller.isFormed()) player.displayClientMessage(message, false); // only speak up if something's wrong
		}
		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FusionControllerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null
				: createTickerHelper(type, ModBlockEntities.FUSION_CONTROLLER.get(), FusionControllerBlockEntity::tick);
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
		return 0.6f;
	}

	@Override
	protected float soundPitch() {
		return 0.5f;
	}
}
