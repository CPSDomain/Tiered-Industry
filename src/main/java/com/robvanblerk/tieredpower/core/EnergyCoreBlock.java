package com.robvanblerk.tieredpower.core;

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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.energy.PowerInfo;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Energy Core (pylon = false) or an Energy Pylon (pylon = true; input or output is decided by the block). */
public class EnergyCoreBlock extends BaseEntityBlock {
	private final boolean pylon;

	public EnergyCoreBlock(boolean pylon, Properties properties) {
		super(properties);
		this.pylon = pylon;
	}

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return pylon ? new EnergyPylonBlockEntity(pos, state) : new EnergyCoreBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) return null;
		return pylon ? createTickerHelper(type, ModBlockEntities.ENERGY_PYLON.get(), EnergyPylonBlockEntity::tick)
				: createTickerHelper(type, ModBlockEntities.ENERGY_CORE.get(), EnergyCoreBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		var be = level.getBlockEntity(pos);
		EnergyCoreBlockEntity core = be instanceof EnergyCoreBlockEntity c ? c : be instanceof EnergyPylonBlockEntity p ? p.core() : null;
		if (be instanceof EnergyPylonBlockEntity && core == null) {
			player.displayClientMessage(Component.literal("No Energy Core within 8 blocks").withStyle(ChatFormatting.RED), true);
		} else if (core != null) {
			player.displayClientMessage(Component.literal(String.format("Energy Core tier %d: %s / %s FE (%.1f%%)  in %s FE/t, out %s FE/t",
					core.getTier(), PowerInfo.shortFe(core.getStored()), PowerInfo.shortFe(core.getCapacity()), core.fill() * 100,
					PowerInfo.shortFe(core.getRateIn()), PowerInfo.shortFe(core.getRateOut()))).withStyle(ChatFormatting.AQUA), true);
		}
		return InteractionResult.CONSUME;
	}

	@Override @SuppressWarnings("deprecation") public boolean hasAnalogOutputSignal(BlockState state) { return !pylon; }

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof EnergyCoreBlockEntity c ? c.getComparatorSignal() : 0;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		if (pylon) return;
		var tag = stack.getTagElement("BlockEntityTag");
		int tier = tag == null || !tag.contains("tier") ? 1 : tag.getInt("tier");
		long stored = tag == null ? 0 : tag.getLong("stored");
		tooltip.add(Component.literal(String.format("Tier %d: %s / %s FE", tier, PowerInfo.shortFe(stored), PowerInfo.shortFe(EnergyCoreBlockEntity.CAPACITY[Math.max(0, Math.min(4, tier - 1))])))
				.withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.literal("Keeps its energy when broken. Energy Pylons within 8 blocks move power in and out").withStyle(ChatFormatting.GRAY));
	}
}
