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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import com.robvanblerk.tieredpower.block.entity.FluidPipeBlockEntity;
import com.robvanblerk.tieredpower.energy.PipeSide;
import com.robvanblerk.tieredpower.energy.PipeTier;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Fluid Pipe (liquids) or Gas Pipe (gases such as Steam). Connects to pipes of the same kind and to anything with a
 * fluid tank. Pipes push into what they touch; use the Wrench on a connection to make it EXTRACT (pull) instead,
 * e.g. to empty a tank into the pipe network.
 */
public class FluidPipeBlock extends BaseEntityBlock {
	public static final Map<Direction, EnumProperty<PipeSide>> SIDES = new EnumMap<>(Direction.class);

	static {
		for (Direction dir : Direction.values()) SIDES.put(dir, EnumProperty.create(dir.getSerializedName(), PipeSide.class));
	}

	private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
	private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

	static {
		ARMS.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5));
		ARMS.put(Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16));
		ARMS.put(Direction.WEST, Block.box(0, 5, 5, 5, 11, 11));
		ARMS.put(Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));
		ARMS.put(Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));
		ARMS.put(Direction.UP, Block.box(5, 11, 5, 11, 16, 11));
	}

	private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();
	private final PipeTier tier;
	private final PipeTier.Kind kind;

	public FluidPipeBlock(PipeTier tier, PipeTier.Kind kind, Properties properties) {
		super(properties);
		this.tier = tier;
		this.kind = kind;
		BlockState state = stateDefinition.any();
		for (EnumProperty<PipeSide> p : SIDES.values()) state = state.setValue(p, PipeSide.NONE);
		registerDefaultState(state);
	}

	public PipeTier getTier() {
		return tier;
	}

	public PipeTier.Kind getKind() {
		return kind;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		SIDES.values().forEach(builder::add);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction dir, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return state.setValue(SIDES.get(dir), sideFor(state, level, pos, dir));
	}

	public static BlockState withConnections(BlockState state, LevelAccessor level, BlockPos pos) {
		for (Direction dir : Direction.values()) state = state.setValue(SIDES.get(dir), sideFor(state, level, pos, dir));
		return state;
	}

	/** Keeps EXTRACT if it's still connected, otherwise PIPE or NONE. */
	private static PipeSide sideFor(BlockState state, LevelAccessor level, BlockPos pos, Direction dir) {
		if (!connectsTo(state, level, pos, dir)) return PipeSide.NONE;
		PipeSide current = state.getValue(SIDES.get(dir));
		return current.isChoice() ? current : PipeSide.PIPE;
	}

	private static boolean connectsTo(BlockState state, LevelAccessor level, BlockPos pos, Direction dir) {
		BlockPos next = pos.relative(dir);
		BlockState other = level.getBlockState(next);
		if (other.getBlock() instanceof FluidPipeBlock pipe) {
			return state.getBlock() instanceof FluidPipeBlock self && self.kind == pipe.kind;
		}
		BlockEntity be = level.getBlockEntity(next);
		return be != null && be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).isPresent();
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeCache.computeIfAbsent(state, s -> {
			VoxelShape shape = CORE;
			for (Direction dir : Direction.values()) {
				PipeSide side = s.getValue(SIDES.get(dir));
				if (side != PipeSide.NONE && side != PipeSide.DISABLED) shape = Shapes.or(shape, ARMS.get(dir));
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
		tooltip.add(Component.literal(String.format("Carries %s: %,d mB/t", kind == PipeTier.Kind.GAS ? "gases (e.g. Steam)" : "liquids",
				tier.getRate())).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Wrench a connection to pull fluid out of that block").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FluidPipeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FLUID_PIPE.get(), FluidPipeBlockEntity::tick);
	}

	/** Right-click a connection with a Fluid Filter to install it; sneak + right-click with an empty hand to remove it. */
	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		var held = player.getItemInHand(hand);
		boolean installing = held.getItem() instanceof com.robvanblerk.tieredpower.item.FluidFilterItem;
		boolean removing = held.isEmpty() && player.isShiftKeyDown();
		if (!installing && !removing) return net.minecraft.world.InteractionResult.PASS;
		if (level.isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof com.robvanblerk.tieredpower.block.entity.FluidPipeBlockEntity pipe)) return net.minecraft.world.InteractionResult.PASS;
		Direction side = ItemPipeBlock.clickedSide(pos, hit.getLocation(), hit.getDirection());
		PipeSide mode = state.getValue(SIDES.get(side));
		if (mode == PipeSide.NONE || mode == PipeSide.DISABLED
				|| level.getBlockEntity(pos.relative(side)) instanceof com.robvanblerk.tieredpower.block.entity.FluidPipeBlockEntity) { // filters go on machine/tank connections
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("No machine or tank connected on the " + side.getName() + " side"), true);
			return net.minecraft.world.InteractionResult.CONSUME;
		}
		var old = pipe.getFilter(side);
		if (installing) {
			pipe.setFilter(side, held.copyWithCount(1));
			if (!player.getAbilities().instabuild) held.shrink(1);
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("Fluid Filter installed on the " + side.getName() + " connection"), true);
		} else {
			if (old.isEmpty()) {
				player.displayClientMessage(net.minecraft.network.chat.Component.literal("No filter on the " + side.getName() + " connection"), true);
				return net.minecraft.world.InteractionResult.CONSUME;
			}
			pipe.setFilter(side, net.minecraft.world.item.ItemStack.EMPTY);
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("Filter removed from the " + side.getName() + " connection"), true);
		}
		if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
		return net.minecraft.world.InteractionResult.CONSUME;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof com.robvanblerk.tieredpower.block.entity.FluidPipeBlockEntity pipe) pipe.dropFilters(level, pos);
		super.onRemove(state, level, pos, newState, moved);
	}
}
