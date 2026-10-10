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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity;
import com.robvanblerk.tieredpower.energy.ItemPipeTier;
import com.robvanblerk.tieredpower.energy.PipeSide;
import com.robvanblerk.tieredpower.item.ItemFilterItem;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Item Pipe. Connects to other Item Pipes and anything with an inventory (chests, machines, Storage Drawers, AE2/RS
 * interfaces, Sophisticated Storage...). Machines and hoppers can push items in; the network delivers them to the
 * other inventories it touches. Wrench a connection to PULL items out of that block instead.
 * Right-click a connection holding an Item Filter to install it; shift + right-click it with an empty hand to take it off.
 */
public class ItemPipeBlock extends BaseEntityBlock {
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
	private final ItemPipeTier tier;

	public ItemPipeBlock(ItemPipeTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
		BlockState state = stateDefinition.any();
		for (var p : FluidPipeBlock.SIDES.values()) state = state.setValue(p, PipeSide.NONE);
		registerDefaultState(state);
	}

	public ItemPipeTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		FluidPipeBlock.SIDES.values().forEach(builder::add);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction dir, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return state.setValue(FluidPipeBlock.SIDES.get(dir), sideFor(state, level, pos, dir));
	}

	public static BlockState withConnections(BlockState state, LevelAccessor level, BlockPos pos) {
		for (Direction dir : Direction.values()) state = state.setValue(FluidPipeBlock.SIDES.get(dir), sideFor(state, level, pos, dir));
		return state;
	}

	private static PipeSide sideFor(BlockState state, LevelAccessor level, BlockPos pos, Direction dir) {
		BlockPos next = pos.relative(dir);
		boolean connects;
		if (level.getBlockState(next).getBlock() instanceof ItemPipeBlock) {
			connects = true;
		} else {
			BlockEntity be = level.getBlockEntity(next);
			connects = be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).isPresent();
		}
		if (!connects) return PipeSide.NONE;
		PipeSide current = state.getValue(FluidPipeBlock.SIDES.get(dir));
		return current.isChoice() ? current : PipeSide.PIPE;
	}

	/** Which arm of the pipe a click landed on. */
	public static Direction clickedSide(BlockPos pos, Vec3 hit, Direction face) {
		Vec3 rel = hit.subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		double max = Math.max(Math.abs(rel.x), Math.max(Math.abs(rel.y), Math.abs(rel.z)));
		if (max <= 3.2 / 16) return face; // clicked the centre part: use the face that was clicked
		return Direction.getNearest(rel.x, rel.y, rel.z);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		boolean installing = held.getItem() instanceof ItemFilterItem;
		boolean removing = held.isEmpty() && player.isShiftKeyDown();
		boolean prioritising = held.isEmpty() && !player.isShiftKeyDown();
		if (!installing && !removing && !prioritising) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) return InteractionResult.PASS;

		Direction side = clickedSide(pos, hit.getLocation(), hit.getDirection());
		PipeSide mode = state.getValue(FluidPipeBlock.SIDES.get(side));
		if (mode == PipeSide.NONE || mode == PipeSide.DISABLED) {
			player.displayClientMessage(Component.literal("Nothing connected on the " + side.getName() + " side"), true);
			return InteractionResult.CONSUME;
		}
		if (prioritising) {
			int value = pipe.cyclePriority(side);
			player.displayClientMessage(Component.literal("Priority " + value + " on the " + side.getName()
					+ " connection (higher fills first)"), true);
			return InteractionResult.CONSUME;
		}
		ItemStack old = pipe.getFilter(side);
		if (installing) {
			pipe.setFilter(side, held.copyWithCount(1));
			if (!player.getAbilities().instabuild) held.shrink(1);
			player.displayClientMessage(Component.literal("Filter installed on the " + side.getName() + " connection"), true);
		} else {
			if (old.isEmpty()) {
				player.displayClientMessage(Component.literal("No filter on the " + side.getName() + " connection"), true);
				return InteractionResult.CONSUME;
			}
			pipe.setFilter(side, ItemStack.EMPTY);
			player.displayClientMessage(Component.literal("Filter removed from the " + side.getName() + " connection"), true);
		}
		if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
		return InteractionResult.CONSUME;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe) {
			pipe.dropFilters(level, pos);
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeCache.computeIfAbsent(state, s -> {
			VoxelShape shape = CORE;
			for (Direction dir : Direction.values()) {
				PipeSide side = s.getValue(FluidPipeBlock.SIDES.get(dir));
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
		tooltip.add(Component.literal(String.format("Pulls up to %,d items/s per pull connection", tier.getItemsPerSecond())).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Wrench a connection to pull; Item Filters limit what passes").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ItemPipeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ITEM_PIPE.get(), ItemPipeBlockEntity::tick);
	}
}
