package com.robvanblerk.tieredpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** Opens a searchable view of everything stored in the network. */
public class StorageTerminalBlock extends HorizontalDirectionalBlock implements StorageNetworkBlock {
	private final boolean crafting;

	public StorageTerminalBlock(Properties properties) {
		this(properties, false);
	}

	/** crafting = true makes a Crafting Terminal. */
	public StorageTerminalBlock(Properties properties, boolean crafting) {
		super(properties);
		this.crafting = crafting;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide()) open(player, pos, crafting);
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	/** Opens a (Crafting) Storage Terminal for the network at pos. Shared with the terminal panels. */
	public static void open(Player player, BlockPos pos, boolean crafting) {
		if (!com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, player.level(), pos)) return;
		if (crafting) player.openMenu(new SimpleMenuProvider((id, inv, p) -> new com.robvanblerk.tieredpower.menu.CraftingTerminalMenu(id, inv, pos),
				Component.translatable("block.tieredpower.crafting_terminal")));
		else player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StorageTerminalMenu(id, inv, pos), Component.translatable("block.tieredpower.storage_terminal")));
	}
}
