package com.robvanblerk.tieredpower.elevator;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Elevator: stand on it and jump to go up to the next Elevator of the same colour above (up to 32 blocks), or sneak
 * to go down. Right-click with a dye to colour it - elevators only link to their own colour.
 */
public class ElevatorBlock extends Block {
	public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
	public static final int RANGE = 32;

	public ElevatorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.WHITE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COLOR);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		if (!(held.getItem() instanceof DyeItem dye)) return InteractionResult.PASS;
		if (state.getValue(COLOR) == dye.getDyeColor()) return InteractionResult.PASS;
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(COLOR, dye.getDyeColor()), Block.UPDATE_ALL);
			if (!player.getAbilities().instabuild) held.shrink(1);
			level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	/** The elevator a player is standing on, or null. */
	public static @Nullable BlockPos under(Player player) {
		BlockPos feet = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
		return player.level().getBlockState(feet).getBlock() instanceof ElevatorBlock ? feet : null;
	}

	/** Moves the player to the next matching elevator up or down, if there is one with room to stand on. */
	public static void travel(ServerPlayer player, boolean up) {
		BlockPos from = under(player);
		if (from == null) return;
		Level level = player.level();
		DyeColor colour = level.getBlockState(from).getValue(COLOR);
		for (int i = 1; i <= RANGE; i++) {
			BlockPos p = from.offset(0, up ? i : -i, 0);
			if (level.isOutsideBuildHeight(p)) break;
			BlockState s = level.getBlockState(p);
			if (!(s.getBlock() instanceof ElevatorBlock) || s.getValue(COLOR) != colour) continue;
			if (!roomAbove(level, p)) continue;
			player.teleportTo(player.getX(), p.getY() + 1, player.getZ());
			player.fallDistance = 0;
			level.playSound(null, p.above(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4f, up ? 1.4f : 1.1f);
			return;
		}
		player.displayClientMessage(Component.literal("No " + colour.getName().replace('_', ' ') + " elevator " + (up ? "above" : "below")
				+ " within " + RANGE + " blocks").withStyle(ChatFormatting.GRAY), true);
	}

	private static boolean roomAbove(Level level, BlockPos p) {
		return level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
				&& level.getBlockState(p.above(2)).getCollisionShape(level, p.above(2)).isEmpty();
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Jump to go up, sneak to go down").withStyle(ChatFormatting.GRAY));
	}
}
