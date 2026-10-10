package com.robvanblerk.tieredpower.item.powered;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * Powered drill: mines anything a pickaxe or shovel can. Uses energy instead of durability; with no energy it barely
 * scratches blocks. The Advanced Drill can mine 3x3 (shift + right-click in the air to switch).
 */
public class ElectricDrillItem extends DiggerItem {
	private final int capacity;
	private final int costPerBlock;
	private final boolean canMineArea;
	private static boolean breakingArea; // stops the 3x3 from triggering itself

	public ElectricDrillItem(Tier tier, TagKey<Block> mineable, int capacity, int costPerBlock, boolean canMineArea, Properties properties) {
		super(1.0f, -2.8f, tier, mineable, properties.stacksTo(1));
		this.capacity = capacity;
		this.costPerBlock = costPerBlock;
		this.canMineArea = canMineArea;
	}

	public int getCapacity() {
		return capacity;
	}

	protected int cost() {
		return costPerBlock;
	}

	public static boolean isAreaMode(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.getBoolean("Area");
	}

	/** How far the area reaches from the centre block: 0 = single block, 1 = 3x3, 2 = 5x5... */
	protected int areaRadius(ItemStack stack) {
		return canMineArea && isAreaMode(stack) ? 1 : 0;
	}

	protected String modeName(ItemStack stack) {
		int r = areaRadius(stack);
		return r == 0 ? "single block" : (2 * r + 1) + "x" + (2 * r + 1);
	}

	/** Shift + right-click: next mining mode. */
	protected void cycleMode(ItemStack stack) {
		stack.getOrCreateTag().putBoolean("Area", !isAreaMode(stack));
	}

	@Override
	public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return ItemEnergy.provider(stack, capacity, capacity / 50, false);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		if (ItemEnergy.get(stack) < cost()) return 0.5f;
		return super.getDestroySpeed(stack, state);
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return ItemEnergy.get(stack) >= cost() && super.isCorrectToolForDrops(stack, state);
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
		if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0) ItemEnergy.use(stack, cost());
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		ItemEnergy.use(stack, cost());
		return true;
	}

	/** Advanced Drill 3x3: also break the 8 blocks around the one being mined, on the face the player is looking at. */
	@Override
	public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
		int r = areaRadius(stack);
		if (r <= 0 || breakingArea || !(player instanceof ServerPlayer server)) return false;
		Level level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1f).scale(8));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK) return false;
		Direction face = hit.getDirection();
		float centreHardness = level.getBlockState(pos).getDestroySpeed(level, pos);

		breakingArea = true;
		try {
			for (int a = -r; a <= r; a++) {
				for (int b = -r; b <= r; b++) {
					if (a == 0 && b == 0) continue;
					BlockPos p = switch (face.getAxis()) {
						case X -> pos.offset(0, a, b);
						case Y -> pos.offset(a, 0, b);
						case Z -> pos.offset(a, b, 0);
					};
					BlockState s = level.getBlockState(p);
					float hardness = s.getDestroySpeed(level, p);
					if (s.isAir() || hardness < 0 || hardness > centreHardness * 4 + 1) continue;
					if (!super.isCorrectToolForDrops(stack, s) || ItemEnergy.get(stack) < cost()) continue;
					server.gameMode.destroyBlock(p);
				}
			}
		} finally {
			breakingArea = false;
		}
		return false;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (canMineArea && player.isShiftKeyDown()) {
			if (!level.isClientSide()) {
				cycleMode(stack);
				player.displayClientMessage(Component.literal("Mining: " + modeName(stack)), true);
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		return InteractionResultHolder.pass(stack);
	}

	// Energy changing every block shouldn't restart the mining animation or re-equip the item.
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
	}

	@Override
	public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
		return !ItemStack.isSameItem(oldStack, newStack);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return ItemEnergy.barWidth(stack, capacity);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return ItemEnergy.BAR_COLOUR;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		ItemEnergy.tooltip(stack, capacity, tooltip);
		tooltip.add(Component.literal(String.format("%,d FE per block", cost())).withStyle(ChatFormatting.GRAY));
		if (canMineArea) {
			tooltip.add(Component.literal("Mode: " + modeName(stack) + " (shift + right-click to switch)").withStyle(ChatFormatting.GRAY));
		}
	}
}
