package com.robvanblerk.tieredpower.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import com.robvanblerk.tieredpower.block.entity.GasTankBlockEntity;

/** The Gas Tank: a tall pressure cylinder. A Gas Cylinder (or anything holding gas) right-clicked on it fills or empties it. */
public class GasTankBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

	public GasTankBlock(Properties properties) {
		super(properties);
	}

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override @SuppressWarnings("deprecation") public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
	@Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GasTankBlockEntity(pos, state); }

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent() && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
			return InteractionResult.sidedSuccess(level.isClientSide());
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GasTankBlockEntity t) {
			FluidStack f = t.tank.getFluid();
			player.displayClientMessage(Component.literal(f.isEmpty() ? "Gas Tank: empty"
					: String.format("Gas Tank: %,d / %,d mB %s", f.getAmount(), GasTankBlockEntity.CAPACITY, f.getDisplayName().getString())).withStyle(ChatFormatting.AQUA), true);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		var tag = stack.getTagElement("BlockEntityTag");
		FluidStack f = tag == null ? FluidStack.EMPTY : FluidStack.loadFluidStackFromNBT(tag.getCompound("tank"));
		tooltip.add(Component.literal(f.isEmpty() ? "Empty - holds 64,000 mB of one gas" : String.format("%,d mB %s", f.getAmount(), f.getDisplayName().getString())).withStyle(ChatFormatting.AQUA));
	}
}
