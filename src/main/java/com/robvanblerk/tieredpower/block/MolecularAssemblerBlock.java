package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

public class MolecularAssemblerBlock extends MachineBlock implements StorageNetworkBlock {
	public MolecularAssemblerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MolecularAssemblerBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
			net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
			net.minecraft.world.phys.BlockHitResult hit) {
		if (!level.isClientSide() && !com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, level, pos)) return net.minecraft.world.InteractionResult.CONSUME;
		return super.use(state, level, pos, player, hand, hit);
	}

	// Glass case: let light through and don't darken neighbours.
	@Override
	@SuppressWarnings("deprecation")
	public float getShadeBrightness(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
		return 1.0f;
	}

	@Override
	public boolean propagatesSkylightDown(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
		return true;
	}

	@Override
	public <T extends BlockEntity> @Nullable net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,
			BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return level.isClientSide() ? null
				: createTickerHelper(type, com.robvanblerk.tieredpower.registry.ModBlockEntities.MOLECULAR_ASSEMBLER.get(), MolecularAssemblerBlockEntity::tick);
	}
}
