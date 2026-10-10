package com.robvanblerk.tieredpower.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity;
import com.robvanblerk.tieredpower.storage.CraftingJob;
import com.robvanblerk.tieredpower.storage.StorageNetwork;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/**
 * Lets the storage network run crafting jobs - 1, 2, 4 or 8 at a time depending on the CPU's tier. Right-click to see the network's jobs;
 * sneak + right-click to cancel them all.
 */
public class CraftingCpuBlock extends Block implements StorageNetworkBlock {
	public CraftingCpuBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(com.robvanblerk.tieredpower.storage.CraftingTier.TIER, 0));
	}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(com.robvanblerk.tieredpower.storage.CraftingTier.TIER);
	}

	@Override
	public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
			java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
		int t = com.robvanblerk.tieredpower.storage.CraftingTier.of(stack);
		int jobs = com.robvanblerk.tieredpower.storage.CraftingTier.CPU_JOBS[t];
		tooltip.add(Component.literal(com.robvanblerk.tieredpower.storage.CraftingTier.NAMES[t] + ": " + jobs + " job" + (jobs > 1 ? "s" : "") + " at once")
				.withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(com.robvanblerk.tieredpower.storage.CraftingTier.COLOUR[t])));
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && !com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, level, pos)) return InteractionResult.CONSUME;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		StorageControllerBlockEntity c = StorageNetwork.findController(level, pos);
		if (c == null) {
			player.sendSystemMessage(Component.literal("Not connected to a Storage Controller").withStyle(ChatFormatting.RED));
			return InteractionResult.CONSUME;
		}
		if (player.isShiftKeyDown()) {
			int n = c.cancelJobs();
			player.sendSystemMessage(Component.literal(n == 0 ? "No crafting jobs to cancel" : "Cancelled " + n + " crafting job(s). Items already made stay in storage.")
					.withStyle(ChatFormatting.YELLOW));
			return InteractionResult.CONSUME;
		}
		int t = com.robvanblerk.tieredpower.storage.CraftingTier.of(state);
		player.sendSystemMessage(Component.literal("-- Crafting jobs --  (this CPU: " + com.robvanblerk.tieredpower.storage.CraftingTier.NAMES[t] + ", "
				+ com.robvanblerk.tieredpower.storage.CraftingTier.CPU_JOBS[t] + " at once; network: " + c.jobSlots() + " at once)").withStyle(ChatFormatting.GOLD));
		if (c.getJobs().isEmpty()) player.sendSystemMessage(Component.literal("None running").withStyle(ChatFormatting.GRAY));
		for (CraftingJob job : c.getJobs()) {
			player.sendSystemMessage(Component.literal(job.amount + " x " + job.target.getHoverName().getString() + ": " + job.status).withStyle(ChatFormatting.AQUA));
		}
		player.sendSystemMessage(Component.literal("Sneak + right-click to cancel all").withStyle(ChatFormatting.DARK_GRAY));
		return InteractionResult.CONSUME;
	}
}
