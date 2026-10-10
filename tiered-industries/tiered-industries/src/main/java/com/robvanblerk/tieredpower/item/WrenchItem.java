package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.BankPortBlock;
import com.robvanblerk.tieredpower.block.BatteryBoxBlock;
import com.robvanblerk.tieredpower.block.FluidPipeBlock;
import com.robvanblerk.tieredpower.energy.PipeSide;
import net.minecraft.world.phys.Vec3;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.SideMode;

/**
 * Wrench, for Tiered Industries blocks only:
 *  - Right-click a machine: rotate it.
 *  - Right-click a Battery Box face: cycle Input / Output / Disabled.
 *  - Right-click a Bank Port: switch Input / Output.
 *  - Shift + right-click: pick the block up, keeping its items, energy, fluids and settings.
 */
public class WrenchItem extends Item {
	public WrenchItem(Properties properties) {
		super(properties);
	}

	/**
	 * Forge calls this BEFORE the block's own right-click action (which would open the machine's GUI),
	 * so the wrench gets first go on Tiered Industries blocks.
	 */
	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = ctx.getPlayer();
		var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
		if (id == null || !TieredPower.MOD_ID.equals(id.getNamespace())) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;

		if (player != null && player.isShiftKeyDown()) {
			pickUp(level, pos, state, player);
			return InteractionResult.CONSUME;
		}

		Block block = state.getBlock();
		if (block instanceof FluidPipeBlock || block instanceof com.robvanblerk.tieredpower.block.ItemPipeBlock) {
			// Work out which connection was clicked (the arm, or the face of the centre part).
			Direction side = com.robvanblerk.tieredpower.block.ItemPipeBlock.clickedSide(pos, ctx.getClickLocation(), ctx.getClickedFace());
			var property = FluidPipeBlock.SIDES.get(side);
			PipeSide current = state.getValue(property);
			if (current == PipeSide.NONE) {
				message(player, "Nothing to connect to on the " + side.getName() + " side");
				return InteractionResult.CONSUME;
			}
			BlockPos otherPos = pos.relative(side);
			BlockState other = level.getBlockState(otherPos);
			if (other.getBlock() == block || (other.getBlock() instanceof FluidPipeBlock && block instanceof FluidPipeBlock)
					|| (other.getBlock() instanceof com.robvanblerk.tieredpower.block.ItemPipeBlock && block instanceof com.robvanblerk.tieredpower.block.ItemPipeBlock)) {
				// Pipe to pipe: just connected / disconnected, on both pipes.
				PipeSide next = current == PipeSide.DISABLED ? PipeSide.PIPE : PipeSide.DISABLED;
				level.setBlock(pos, state.setValue(property, next), Block.UPDATE_ALL);
				var back = FluidPipeBlock.SIDES.get(side.getOpposite());
				if (other.hasProperty(back) && other.getValue(back) != PipeSide.NONE) level.setBlock(otherPos, other.setValue(back, next), Block.UPDATE_ALL);
				// The two pipes may now belong to different networks: make both rebuild.
				for (BlockPos p : new BlockPos[]{pos, otherPos}) {
					if (level.getBlockEntity(p) instanceof com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity ip && ip.getNetwork() != null) ip.getNetwork().invalidate();
					if (level.getBlockEntity(p) instanceof com.robvanblerk.tieredpower.block.entity.FluidPipeBlockEntity fp && fp.getNetwork() != null) fp.getNetwork().invalidate();
				}
				message(player, capitalise(side.getName()) + ": " + (next == PipeSide.DISABLED ? "disconnected from the next pipe" : "connected to the next pipe"));
			} else {
				PipeSide next = current.nextMode();
				level.setBlock(pos, state.setValue(property, next), Block.UPDATE_ALL);
				message(player, capitalise(side.getName()) + ": " + next.describe());
			}
		} else if (block instanceof BatteryBoxBlock) {
			Direction face = ctx.getClickedFace();
			var property = BatteryBoxBlock.SIDES.get(face);
			SideMode mode = state.getValue(property).next();
			level.setBlock(pos, state.setValue(property, mode), Block.UPDATE_ALL);
			message(player, capitalise(face.getName()) + " face: " + mode.displayName());
		} else if (block instanceof BankPortBlock) {
			SideMode mode = state.getValue(BankPortBlock.MODE) == SideMode.INPUT ? SideMode.OUTPUT : SideMode.INPUT;
			level.setBlock(pos, state.setValue(BankPortBlock.MODE, mode), Block.UPDATE_ALL);
			message(player, "Bank Port: " + mode.displayName());
		} else if (state.hasProperty(MachineBlock.FACING)) { // four-way: turn clockwise
			level.setBlock(pos, state.setValue(MachineBlock.FACING, state.getValue(MachineBlock.FACING).getClockWise()), Block.UPDATE_ALL);
		} else if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) { // six-way (buses, panels): next direction
			var facing = net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
			net.minecraft.core.Direction next = net.minecraft.core.Direction.from3DDataValue(state.getValue(facing).get3DDataValue() + 1);
			level.setBlock(pos, state.setValue(facing, next), Block.UPDATE_ALL);
			message(player, "Facing " + next.getName());
		} else {
			return InteractionResult.PASS;
		}
		level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
		return InteractionResult.CONSUME;
	}

	/** Removes the block and gives it to the player with its block entity data (items, energy, fluids) saved on the item. */
	private static void pickUp(Level level, BlockPos pos, BlockState state, Player player) {
		ItemStack drop = new ItemStack(state.getBlock());
		BlockEntity be = level.getBlockEntity(pos);
		if (be != null) {
			CompoundTag data = be.saveWithoutMetadata();
			if (!data.isEmpty()) BlockItem.setBlockEntityData(drop, be.getType(), data);
			if (be instanceof Container container) container.clearContent(); // contents travel on the item instead of spilling
		}
		level.removeBlock(pos, false);
		level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1.0f);
		if (!player.getInventory().add(drop)) player.drop(drop, false);
	}

	private static void message(@Nullable Player player, String text) {
		if (player != null) player.displayClientMessage(Component.literal(text), true);
	}

	private static String capitalise(String s) {
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click: rotate machines, buses and panels; set battery faces and bank ports").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Right-click a pipe connection: Push / Pull / Push + Pull / Disabled").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Shift + right-click: pick up with contents and energy").withStyle(ChatFormatting.GRAY));
	}
}
