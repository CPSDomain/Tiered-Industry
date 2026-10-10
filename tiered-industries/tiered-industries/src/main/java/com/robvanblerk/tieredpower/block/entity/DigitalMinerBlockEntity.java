package com.robvanblerk.tieredpower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import com.robvanblerk.tieredpower.item.ItemFilterItem;
import com.robvanblerk.tieredpower.menu.WorkerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModMenus;

/**
 * Mines only the ores you ask for. Item Filters in its slots choose them (none = every ore); an Enchanted Book of Silk
 * Touch or Fortune in a slot sets how they're mined (the book isn't used up). It scans a 33x33 area under itself layer by
 * layer, from just below down to the bottom of the world, and leaves everything that isn't a wanted ore alone.
 * 800 FE per ore; changing the filters or book starts the scan again from the top.
 */
public class DigitalMinerBlockEntity extends WorkerBlockEntity {
	public static final int RADIUS = 16, COST = 800, SCAN_PER_JOB = 2_048;
	public static final int SPECIAL_DONE = 1;
	private int cx, cy = Integer.MIN_VALUE, cz;
	private int mined, configHash;

	public DigitalMinerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DIGITAL_MINER.get(), pos, state);
	}

	public int getMined() { return mined; }
	public int getScanY() { return cy; }

	@Override protected int costPerAction() { return COST; }
	@Override protected int interval() { return 5; }
	@Override protected MenuType<WorkerMenu> menuType() { return ModMenus.DIGITAL_MINER.get(); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.digital_miner"); }

	@Override
	public boolean isValidInput(ItemStack stack) {
		return stack.getItem() instanceof ItemFilterItem || stack.getItem() instanceof EnchantedBookItem;
	}

	/** A diamond pickaxe carrying the Silk Touch or best Fortune from the books in the slots. */
	private ItemStack tool() {
		ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
		int fortune = 0;
		boolean silk = false;
		for (int i = 0; i < INPUTS; i++) {
			ItemStack s = items.get(i);
			if (!(s.getItem() instanceof EnchantedBookItem)) continue;
			var enchants = EnchantmentHelper.getEnchantments(s);
			if (enchants.containsKey(Enchantments.SILK_TOUCH)) silk = true;
			fortune = Math.max(fortune, enchants.getOrDefault(Enchantments.BLOCK_FORTUNE, 0));
		}
		if (silk) pick.enchant(Enchantments.SILK_TOUCH, 1);
		else if (fortune > 0) pick.enchant(Enchantments.BLOCK_FORTUNE, fortune);
		return pick;
	}

	private boolean wanted(BlockState state) {
		if (!state.is(Tags.Blocks.ORES)) return false;
		ItemStack probe = new ItemStack(state.getBlock().asItem());
		for (int i = 0; i < INPUTS; i++) {
			ItemStack s = items.get(i);
			if (s.getItem() instanceof ItemFilterItem && !ItemFilterItem.passes(s, probe)) return false;
		}
		return true;
	}

	private int configHash() {
		int h = 1;
		for (int i = 0; i < INPUTS; i++) h = h * 31 + (items.get(i).isEmpty() ? 0 : java.util.Objects.hash(items.get(i).getItem(), items.get(i).getTag()));
		return h;
	}

	private void restart() {
		cy = worldPosition.getY() - 1;
		cx = -RADIUS;
		cz = -RADIUS;
	}

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, Direction facing) {
		int hash = configHash();
		if (hash != configHash || cy == Integer.MIN_VALUE) {
			configHash = hash;
			restart();
		}
		ItemStack tool = null;
		for (int n = 0; n < SCAN_PER_JOB; n++) {
			if (cy < level.getMinBuildHeight()) {
				status = STATUS_SPECIAL;
				special = SPECIAL_DONE;
				return false;
			}
			BlockPos p = new BlockPos(pos.getX() + cx, cy, pos.getZ() + cz);
			if (++cx > RADIUS) {
				cx = -RADIUS;
				if (++cz > RADIUS) { cz = -RADIUS; cy--; }
			}
			if (!level.isLoaded(p)) continue;
			BlockState state = level.getBlockState(p);
			if (!wanted(state) || state.getDestroySpeed(level, p) < 0) continue;
			if (tool == null) tool = tool();
			for (ItemStack drop : Block.getDrops(state, level, p, level.getBlockEntity(p), fakePlayer(level), tool)) outputOrDrop(level, pos.above(), drop);
			level.levelEvent(2001, p, Block.getId(state));
			level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			mined++;
			setChanged();
			return true;
		}
		return false; // nothing in this stretch; carry on next time
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("cx", cx);
		tag.putInt("cy", cy);
		tag.putInt("cz", cz);
		tag.putInt("mined", mined);
		tag.putInt("configHash", configHash);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		cx = tag.getInt("cx");
		cy = tag.contains("cy") ? tag.getInt("cy") : Integer.MIN_VALUE;
		cz = tag.getInt("cz");
		mined = tag.getInt("mined");
		configHash = tag.getInt("configHash");
	}
}
