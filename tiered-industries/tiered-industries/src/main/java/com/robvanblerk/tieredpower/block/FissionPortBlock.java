package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.FissionPortBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * A wall port of the multiblock Fission Reactor: Fuel (rods in - right-click with rods, hoppers or pipes), Waste (pushes depleted
 * rods out into pipes/chests), Coolant (water in / steam out) or Power (power out).
 */
public class FissionPortBlock extends BaseEntityBlock {
	public enum Kind { FUEL, WASTE, COOLANT, POWER }

	private final Kind kind;

	public FissionPortBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind getKind() {
		return kind;
	}

	/** Fuel Port: right-click with fuel rods to load them; with an empty hand to see what's loaded. */
	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (kind != Kind.FUEL) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		FissionControllerBlockEntity controller = level.getBlockEntity(pos) instanceof FissionPortBlockEntity port ? port.getController() : null;
		if (controller == null) {
			player.displayClientMessage(Component.literal("The reactor isn't formed - right-click the Fission Controller to see why"), true);
			return InteractionResult.CONSUME;
		}
		ItemStack held = player.getItemInHand(hand);
		if (com.robvanblerk.tieredpower.energy.FuelRods.isFuel(held)) {
			int loaded = controller.loadRods(held.getCount(), com.robvanblerk.tieredpower.energy.FuelRods.isMox(held));
			if (!player.getAbilities().instabuild) held.shrink(loaded);
			player.displayClientMessage(Component.literal(loaded > 0 ? "Loaded " + loaded + " fuel rod" + (loaded == 1 ? "" : "s") + " (" + controller.getStoredRods() + " in the reactor)"
					: "The reactor is full of fuel (" + FissionControllerBlockEntity.MAX_RODS + " rods)"), true);
		} else {
			player.displayClientMessage(Component.literal(controller.getStoredRods() + " fuel rods loaded, " + controller.getWaitingDepleted() + " depleted rods waiting"), true);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FissionPortBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || kind == Kind.FUEL ? null : createTickerHelper(type, ModBlockEntities.FISSION_PORT.get(), FissionPortBlockEntity::tick);
	}
}
