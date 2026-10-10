package com.robvanblerk.tieredpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/**
 * Machine Connector: a flat panel stuck onto a machine's face that joins that machine to the storage network, so the
 * Assembly Matrix can run its patterns there (up to 4 operations per cycle). No slots, no screen. Connect it to the
 * network with Storage Cable (touching any part of the panel's block space).
 */
public class MachineConnectorBlock extends DirectionalBlock implements StorageNetworkBlock {
	public MachineConnectorBlock(Properties properties) {
		super(properties);
		registerDefaultState(PanelCables.none(stateDefinition.any().setValue(FACING, Direction.NORTH)
				.setValue(com.robvanblerk.tieredpower.storage.CraftingTier.TIER, 0)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, com.robvanblerk.tieredpower.storage.CraftingTier.TIER);
		PanelCables.addProperties(builder);
	}

	/** The screen faces out from the surface you clicked. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return PanelCables.update(defaultBlockState().setValue(FACING, ctx.getClickedFace()), ctx.getLevel(), ctx.getClickedPos());
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return switch (state.getValue(FACING)) {
			case NORTH -> Block.box(1, 1, 13, 15, 15, 16);
			case SOUTH -> Block.box(1, 1, 0, 15, 15, 3);
			case WEST -> Block.box(13, 1, 1, 16, 15, 15);
			case EAST -> Block.box(0, 1, 1, 3, 15, 15);
			case UP -> Block.box(1, 0, 1, 15, 3, 15);
			case DOWN -> Block.box(1, 13, 1, 15, 16, 15);
		};
	}


	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return PanelCables.update(state, dir, neighbour);
	}

	/** Right-click with an empty hand: says whether this connector is on a network and which machine it serves. */
	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		var controller = com.robvanblerk.tieredpower.storage.StorageNetwork.findController(level, pos);
		var machines = com.robvanblerk.tieredpower.block.entity.MachineConnectorBlockEntityHelper.touching(level, pos);
		StringBuilder msg = new StringBuilder();
		int t = com.robvanblerk.tieredpower.storage.CraftingTier.of(state);
		msg.append(com.robvanblerk.tieredpower.storage.CraftingTier.NAMES[t]).append(" (")
				.append(com.robvanblerk.tieredpower.storage.CraftingTier.CONNECTOR_OPS[t]).append(" operations per cycle). ");
		if (controller == null) msg.append("\u00a7cNot linked to a Storage Controller\u00a7r - run Storage Cable to this panel.");
		else msg.append("\u00a7aLinked\u00a7r to the network.");
		msg.append(" Machine: ");
		if (machines.isEmpty()) msg.append("\u00a7cnone touching\u00a7r");
		else {
			for (int i = 0; i < machines.size(); i++) {
				if (i > 0) msg.append(", ");
				msg.append(level.getBlockState(machines.get(i)).getBlock().getName().getString());
			}
		}
		player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg.toString()), false);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable BlockGetter level,
			java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
		int t = com.robvanblerk.tieredpower.storage.CraftingTier.of(stack);
		tooltip.add(net.minecraft.network.chat.Component.literal(com.robvanblerk.tieredpower.storage.CraftingTier.NAMES[t] + ": "
				+ com.robvanblerk.tieredpower.storage.CraftingTier.CONNECTOR_OPS[t] + " operations per cycle")
				.withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(com.robvanblerk.tieredpower.storage.CraftingTier.COLOUR[t])));
	}
}
