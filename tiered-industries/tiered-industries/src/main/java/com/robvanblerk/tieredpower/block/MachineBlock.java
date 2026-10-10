package com.robvanblerk.tieredpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import com.robvanblerk.tieredpower.Config;

/**
 * Shared behaviour for furnace-style machines: they face the player when placed, have a "lit" state
 * while running, open their GUI on right-click, and drop their items when broken.
 */
public abstract class MachineBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	protected MachineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** Sound played now and then while the machine is running (null = silent). */
	protected @Nullable SoundEvent runningSound() {
		return null;
	}

	/** Chance per tick of playing the running sound. */
	protected float soundChance() {
		return 0.08f;
	}

	protected float soundVolume() {
		return 0.35f;
	}

	protected float soundPitch() {
		return 1.0f;
	}

	/** Generators that burn fuel show smoke and flames. */
	protected boolean burnsFuel() {
		return false;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
		SoundEvent sound = runningSound();
		boolean muffled = level.getBlockEntity(pos) instanceof com.robvanblerk.tieredpower.block.entity.MachineBlockEntity m && m.isMuffled();
		if (sound != null && !muffled && Config.get(Config.MACHINE_SOUNDS) && random.nextFloat() < soundChance()) {
			level.playLocalSound(x, y + 0.5, z, sound, SoundSource.BLOCKS, soundVolume(),
					soundPitch() * (0.9f + random.nextFloat() * 0.2f), false);
		}
		if (burnsFuel() && Config.get(Config.MACHINE_PARTICLES)) {
			Direction front = state.getValue(FACING);
			double side = random.nextDouble() * 0.6 - 0.3;
			double px = x + front.getStepX() * 0.52 + (front.getAxis() == Direction.Axis.X ? 0 : side);
			double pz = z + front.getStepZ() * 0.52 + (front.getAxis() == Direction.Axis.Z ? 0 : side);
			double py = y + random.nextDouble() * 6.0 / 16.0 + 0.1;
			level.addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0, 0);
			level.addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0, 0);
		}
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL; // BaseEntityBlock defaults to invisible
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
			player.openMenu(provider);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof Container container) {
				Containers.dropContents(level, pos, container);
				level.updateNeighbourForOutputSignal(pos, this);
			}
			// Tier Installers and the Muffler come back out, so upgrading is never lost.
			if (level.getBlockEntity(pos) instanceof com.robvanblerk.tieredpower.block.entity.MachineBlockEntity m) {
				for (int t = 1; t <= m.getTier(); t++)
					Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
							new net.minecraft.world.item.ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.allInstallers().get(t - 1)));
				if (m.isMuffled()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
						new net.minecraft.world.item.ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.MUFFLER.get()));
				for (var extra : m.installedDrops()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, extra.copy());
			}
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
