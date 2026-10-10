package com.robvanblerk.tieredpower.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import com.robvanblerk.tieredpower.block.entity.FluidTankBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Holds one fluid. Buckets and pipes work on any side. Keeps its contents when mined. */
public class FluidTankBlock extends BaseEntityBlock {
	private final int capacity;

	public FluidTankBlock(int capacity, Properties properties) {
		super(properties);
		this.capacity = capacity;
	}

	public int getCapacity() {
		return capacity;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
			FluidStack fluid = tank.getFluid();
			player.displayClientMessage(fluid.isEmpty() ? Component.literal("Empty")
					: Component.literal(String.format("%,d / %,d mB ", fluid.getAmount(), capacity)).append(fluid.getDisplayName()), true);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)) return 0;
		int amount = tank.getFluid().getAmount();
		return amount <= 0 ? 0 : 1 + (int) ((long) amount * 14 / capacity);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag data = BlockItem.getBlockEntityData(stack);
		FluidStack fluid = data != null && data.contains("fluid") ? FluidStack.loadFluidStackFromNBT(data.getCompound("fluid")) : FluidStack.EMPTY;
		if (fluid.isEmpty()) {
			tooltip.add(Component.literal(String.format("Empty (holds %,d mB)", capacity)).withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(Component.literal(String.format("%,d / %,d mB ", fluid.getAmount(), capacity)).append(fluid.getDisplayName()).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FluidTankBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FLUID_TANK.get(), FluidTankBlockEntity::tick);
	}
}
