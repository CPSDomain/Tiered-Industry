package com.robvanblerk.tieredpower.block;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.block.entity.BatteryBoxBlockEntity;
import com.robvanblerk.tieredpower.energy.BatteryTier;
import com.robvanblerk.tieredpower.energy.SideMode;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Tiered energy storage. Each face is Input, Output or Disabled (shown on the block).
 * When placed, the face pointing at you becomes the Input and the other five faces are Outputs.
 * Shift + right-click a face with an empty hand to cycle it. Keeps its energy when mined.
 */
public class BatteryBoxBlock extends BaseEntityBlock {
	public static final Map<Direction, EnumProperty<SideMode>> SIDES = new EnumMap<>(Direction.class);

	static {
		for (Direction dir : Direction.values()) {
			SIDES.put(dir, EnumProperty.create(dir.getSerializedName(), SideMode.class));
		}
	}

	private final BatteryTier tier;

	public BatteryBoxBlock(BatteryTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
		BlockState state = stateDefinition.any();
		for (EnumProperty<SideMode> property : SIDES.values()) state = state.setValue(property, SideMode.OUTPUT);
		registerDefaultState(state);
	}

	public BatteryTier getTier() {
		return tier;
	}

	public static SideMode getMode(BlockState state, Direction side) {
		return state.getValue(SIDES.get(side));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		SIDES.values().forEach(builder::add);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction toPlayer = ctx.getNearestLookingDirection().getOpposite();
		return defaultBlockState().setValue(SIDES.get(toPlayer), SideMode.INPUT);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			if (!level.isClientSide()) {
				Direction face = hit.getDirection();
				SideMode mode = getMode(state, face).next();
				level.setBlock(pos, state.setValue(SIDES.get(face), mode), Block.UPDATE_ALL);
				player.displayClientMessage(Component.literal(capitalise(face.getName()) + " face: " + mode.displayName()), true);
			}
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BatteryBoxBlockEntity battery) {
			player.openMenu(battery);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	private static String capitalise(String s) {
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag data = BlockItem.getBlockEntityData(stack);
		int stored = data == null ? 0 : data.getInt("energy");
		tooltip.add(Component.literal(String.format("Stored: %,d / %,d FE", stored, tier.getCapacity())).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal(String.format("Transfer: %,d FE/t per face", tier.getRate())).withStyle(ChatFormatting.DARK_GRAY));
		tooltip.add(Component.literal("Shift + right-click a face (empty hand) to change it").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** Comparators read 0-15 depending on how full the battery is. */
	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof BatteryBoxBlockEntity battery ? battery.getComparatorSignal() : 0;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BatteryBoxBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null
				: createTickerHelper(type, ModBlockEntities.BATTERY_BOX.get(), BatteryBoxBlockEntity::tick);
	}
}
