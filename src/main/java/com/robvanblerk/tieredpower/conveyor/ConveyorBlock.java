package com.robvanblerk.tieredpower.conveyor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/**
 * Conveyor Belt: carries dropped items (and mobs and players) in the direction it faces. Items stay as real item
 * entities, so you can see everything moving. At the end of a belt, items go into any inventory in front of it.
 * Hoppers, pipes and Export Buses can put items onto a belt. Sneak + right-click with an empty hand to make it a
 * slope going up or down a block.
 */
public class ConveyorBlock extends Block implements EntityBlock {
	public enum Slope implements StringRepresentable {
		FLAT, UP, DOWN;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);

	/** Belt speeds in blocks per tick: Basic, Fast, Express. */
	public static final double[] SPEED = {1 / 16.0, 2 / 16.0, 4 / 16.0};
	public static final String[] TIER_NAMES = {"Basic", "Fast", "Express"};
	private static final String TAG = "tpBelt";

	private final int tier;

	public ConveyorBlock(int tier, Properties properties) {
		super(properties);
		this.tier = tier;
		BlockState d = stateDefinition.any().setValue(FACING, Direction.NORTH);
		if (d.hasProperty(SLOPE)) d = d.setValue(SLOPE, Slope.FLAT);
		registerDefaultState(d);
	}

	public int tier() {
		return tier;
	}

	public double speed() {
		return SPEED[tier];
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SLOPE);
	}

	public static Slope slope(BlockState state) {
		return state.hasProperty(SLOPE) ? state.getValue(SLOPE) : Slope.FLAT;
	}

	/** Items move away from you. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
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

	// ---- shapes ----

	private static final VoxelShape FLAT_SHAPE = Block.box(0, 0, 0, 16, 2, 16);
	private static final VoxelShape FLAT_OUTLINE = Block.box(0, 0, 0, 16, 3, 16);
	private static final java.util.Map<Direction, VoxelShape> RISING = new java.util.EnumMap<>(Direction.class);

	static {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			VoxelShape s = Shapes.empty();
			for (int i = 0; i < 4; i++) s = Shapes.or(s, rotated(d, 0, 0, 16 - (i + 1) * 4, 16, Math.max(2, i * 4), 16 - i * 4));
			RISING.put(d, s);
		}
	}

	/** A box given for a belt facing north, turned to face 'd'. */
	private static VoxelShape rotated(Direction d, double x1, double y1, double z1, double x2, double y2, double z2) {
		return switch (d) {
			case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
			case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
			case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
			default -> Block.box(x1, y1, z1, x2, y2, z2);
		};
	}

	private static VoxelShape shapeOf(BlockState state, boolean outline) {
		return switch (slope(state)) {
			case UP -> RISING.get(state.getValue(FACING));
			case DOWN -> RISING.get(state.getValue(FACING).getOpposite());
			default -> outline ? FLAT_OUTLINE : FLAT_SHAPE;
		};
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeOf(state, true);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapeOf(state, false);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return false;
	}

	// ---- moving things ----

	/**
	 * Which way something at this point of the belt travels, or null to leave it alone. 't' is how far along the belt
	 * it is (0 = where items come on, 1 = the far end).
	 */
	protected @Nullable Direction route(BlockState state, Level level, BlockPos pos, Entity entity, double t) {
		return state.getValue(FACING);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (entity.isSpectator() || entity instanceof Player p && (p.isShiftKeyDown() || p.getAbilities().flying)) return;
		Direction facing = state.getValue(FACING);
		double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
		double t = (entity.getX() - cx) * facing.getStepX() + (entity.getZ() - cz) * facing.getStepZ() + 0.5;
		Slope slope = slope(state);

		// Slopes: keep things on the incline.
		if (slope != Slope.FLAT) {
			double k = Math.max(0, Math.min(1, t));
			double targetY = pos.getY() + (slope == Slope.UP ? k : 1 - k) + 0.125;
			if (entity.getY() < targetY + (slope == Slope.UP ? 0.3 : 0.6) && entity.getY() > targetY - 0.6) {
				if (entity instanceof ItemEntity) {
					entity.setPos(entity.getX(), targetY, entity.getZ());
					Vec3 v = entity.getDeltaMovement();
					entity.setDeltaMovement(v.x, 0, v.z);
					entity.setOnGround(true);
				} else if (slope == Slope.UP && entity.getY() < targetY) {
					entity.setPos(entity.getX(), targetY, entity.getZ());
				}
			}
		} else if (entity.getY() > pos.getY() + 0.5) {
			return; // flying over, not on the belt
		}

		Direction travel = route(state, level, pos, entity, t);
		if (travel == null) return;
		double speed = speed();
		Vec3 v = entity.getDeltaMovement();
		if (entity instanceof ItemEntity item) {
			// Item friction is applied after we set the speed, so allow for it.
			float friction = entity.onGround() ? level.getBlockState(BlockPos.containing(entity.getX(), entity.getY() - 0.5000001, entity.getZ()))
					.getFriction(level, pos, entity) * 0.98f : 0.98f;
			double boost = speed / Math.max(0.3, friction);
			// Pull towards the middle of the belt (across the direction of travel).
			double perp = travel.getAxis() == Direction.Axis.X ? cz - entity.getZ() : cx - entity.getX();
			double centre = Math.max(-0.08, Math.min(0.08, perp * 0.25)) / Math.max(0.3, friction);
			double vx = travel.getStepX() * boost + (travel.getAxis() == Direction.Axis.Z ? centre : 0);
			double vz = travel.getStepZ() * boost + (travel.getAxis() == Direction.Axis.X ? centre : 0);
			entity.setDeltaMovement(vx, v.y, vz);
			if (!level.isClientSide()) {
				if (item.getAge() > item.lifespan - 200) item.lifespan = item.getAge() + 6000; // don't despawn on a belt
				// Splitters and Filter Belts decide on the server: keep clients told while they steer.
				if (travel != facing || (t >= 0.5 && (this instanceof SplitterConveyorBlock || this instanceof FilterConveyorBlock))) entity.hasImpulse = true;
				double along = (entity.getX() - cx) * travel.getStepX() + (entity.getZ() - cz) * travel.getStepZ() + 0.5;
				if (along > 0.8) deliver(level, pos, state, item, travel);
			}
		} else {
			double along = v.x * travel.getStepX() + v.z * travel.getStepZ();
			double want = speed * 1.6;
			if (along < want) {
				double add = (want - along) * 0.5;
				entity.setDeltaMovement(v.x + travel.getStepX() * add, v.y, v.z + travel.getStepZ() * add);
			}
		}
	}

	/** At the end of the belt: into the inventory in front, if there is one. */
	private static void deliver(Level level, BlockPos pos, BlockState state, ItemEntity item, Direction travel) {
		BlockPos target = pos.relative(travel);
		if (travel == state.getValue(FACING) && slope(state) == Slope.UP) target = target.above();
		if (level.getBlockState(target).getBlock() instanceof ConveyorBlock) return;
		BlockEntity be = level.getBlockEntity(target);
		if (be == null) return;
		be.getCapability(ForgeCapabilities.ITEM_HANDLER, travel.getOpposite()).ifPresent(inv -> {
			ItemStack left = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(inv, item.getItem().copy(), false);
			if (left.isEmpty()) item.discard();
			else item.setItem(left);
		});
	}

	/** True if something next to this belt on 'side' can take items: another belt or an inventory. */
	protected static boolean canOutput(Level level, BlockPos pos, Direction side) {
		BlockPos n = pos.relative(side);
		if (level.getBlockState(n).getBlock() instanceof ConveyorBlock) return true;
		BlockEntity be = level.getBlockEntity(n);
		return be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).isPresent();
	}

	/** Per-item memory of a decision made on this block (which side a splitter or filter sends it). */
	protected static int decision(Entity entity, BlockPos pos) {
		CompoundTag t = entity.getPersistentData().getCompound(TAG);
		return t.getLong("pos") == pos.asLong() && t.contains("dir") ? t.getInt("dir") : -1;
	}

	protected static void decide(Entity entity, BlockPos pos, Direction dir) {
		CompoundTag t = new CompoundTag();
		t.putLong("pos", pos.asLong());
		t.putInt("dir", dir.get3DDataValue());
		entity.getPersistentData().put(TAG, t);
	}

	// ---- interaction ----

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!state.hasProperty(SLOPE) || !player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
		if (!level.isClientSide()) {
			Slope next = Slope.values()[(slope(state).ordinal() + 1) % 3];
			level.setBlock(pos, state.setValue(SLOPE, next), 3);
			player.displayClientMessage(Component.literal(switch (next) {
				case UP -> "Slope: going up a block (towards where it points)";
				case DOWN -> "Slope: going down a block";
				default -> "Flat";
			}).withStyle(ChatFormatting.YELLOW), true);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(String.format("%s: %.2f blocks per second", TIER_NAMES[tier], speed() * 20)).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConveyorBlockEntity(pos, state);
	}
}
