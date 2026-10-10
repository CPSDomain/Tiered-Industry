package com.robvanblerk.tieredpower.block;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import com.robvanblerk.tieredpower.block.entity.CableBlockEntity;
import com.robvanblerk.tieredpower.energy.CableTier;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * A cable that connects to other cables and to ANY block exposing Forge Energy on that side,
 * whether it's from this mod or another one. The tier decides how much FE per tick it can carry.
 */
public class CableBlock extends BaseEntityBlock {
	public static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);
	/** Lit while power is flowing through the cable's network (set by CableNetwork about once a second). */
	public static final BooleanProperty POWERED = BooleanProperty.create("powered");

	static {
		CONNECTIONS.put(Direction.NORTH, BlockStateProperties.NORTH);
		CONNECTIONS.put(Direction.EAST, BlockStateProperties.EAST);
		CONNECTIONS.put(Direction.SOUTH, BlockStateProperties.SOUTH);
		CONNECTIONS.put(Direction.WEST, BlockStateProperties.WEST);
		CONNECTIONS.put(Direction.UP, BlockStateProperties.UP);
		CONNECTIONS.put(Direction.DOWN, BlockStateProperties.DOWN);
	}

	private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
	private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

	static {
		ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6));
		ARMS.put(Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16));
		ARMS.put(Direction.WEST, Block.box(0, 6, 6, 6, 10, 10));
		ARMS.put(Direction.EAST, Block.box(10, 6, 6, 16, 10, 10));
		ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10));
		ARMS.put(Direction.UP, Block.box(6, 10, 6, 10, 16, 10));
	}

	private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();
	private final CableTier tier;

	public CableBlock(CableTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
		BlockState state = stateDefinition.any();
		for (BooleanProperty property : CONNECTIONS.values()) state = state.setValue(property, false);
		state = state.setValue(POWERED, false);
		registerDefaultState(state);
	}

	public CableTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CONNECTIONS.values().toArray(new BooleanProperty[0]));
		builder.add(POWERED);
	}

	// ---- Connections ----

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
	}

	/** Called when a neighbouring block changes, so arms appear/disappear straight away. */
	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction dir, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return state.setValue(CONNECTIONS.get(dir), connectsTo(level, pos, dir));
	}

	public static BlockState withConnections(BlockState state, LevelAccessor level, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			state = state.setValue(CONNECTIONS.get(dir), connectsTo(level, pos, dir));
		}
		return state;
	}

	private static boolean connectsTo(LevelAccessor level, BlockPos pos, Direction dir) {
		BlockEntity be = level.getBlockEntity(pos.relative(dir));
		if (be == null) return false;
		if (be instanceof CableBlockEntity || be instanceof com.robvanblerk.tieredpower.block.entity.PowerMonitorBlockEntity) return true;
		return be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).isPresent();
	}

	// ---- Shape, rendering, tooltip ----

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeCache.computeIfAbsent(state, s -> {
			VoxelShape shape = CORE;
			for (Direction dir : Direction.values()) {
				if (s.getValue(CONNECTIONS.get(dir))) shape = Shapes.or(shape, ARMS.get(dir));
			}
			return shape;
		});
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.tieredpower.cable_rate", String.format("%,d", tier.getTransferRate()))
				.withStyle(ChatFormatting.GRAY));
	}

	// ---- Block entity ----

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null
				: createTickerHelper(type, ModBlockEntities.CABLE.get(), CableBlockEntity::tick);
	}
}
