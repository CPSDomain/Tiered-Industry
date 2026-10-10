package com.robvanblerk.tieredpower.planet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.stargate.Planet;

/**
 * Guardian Altar: found in planet outposts. Right-click it to call the planet's guardian - an optional boss. Beat it
 * for a Guardian Heart and a pile of loot; the altar then goes dark. Nothing in the mod needs a guardian to be beaten.
 */
public class GuardianAltarBlock extends Block {
	/** Lit while unused; dark once its guardian has been called. */
	public static final BooleanProperty SPENT = BooleanProperty.create("spent");

	public GuardianAltarBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SPENT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SPENT);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (state.getValue(SPENT)) {
			player.displayClientMessage(Component.literal("The altar is dark - its guardian has already been called").withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		Planet planet = Planet.of(level);
		if (planet == null) {
			player.displayClientMessage(Component.literal("Nothing answers here - guardians only live on Stargate planets").withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		if (!player.isShiftKeyDown()) {
			player.displayClientMessage(Component.literal("Sneak + right-click to call the " + planet.title + " Guardian (a boss fight - be ready)").withStyle(ChatFormatting.GOLD), true);
			return InteractionResult.CONSUME;
		}
		if (Guardians.summon((ServerLevel) level, pos, planet)) level.setBlock(pos, state.setValue(SPENT, true), Block.UPDATE_ALL);
		return InteractionResult.CONSUME;
	}
}
