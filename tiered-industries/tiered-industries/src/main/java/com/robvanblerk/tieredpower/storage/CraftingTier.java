package com.robvanblerk.tieredpower.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Tiers for the Crafting CPU, Crafting Accelerator and Machine Connector (Basic, Advanced, Elite, Quantum), raised in place with
 * Crafting Upgrades. The tier is a block state, so it is kept when the block is broken (the loot table copies it).
 */
public final class CraftingTier {
	public static final int MAX = 3;
	public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, MAX);
	public static final String[] NAMES = {"Basic", "Advanced", "Elite", "Quantum"};
	/** Crafting jobs one CPU runs at the same time. */
	public static final int[] CPU_JOBS = {1, 2, 4, 8};
	/** Crafts per cycle one Accelerator adds to its Assembly Matrix. */
	public static final int[] ACCELERATOR_CRAFTS = {2, 4, 8, 16};
	/** Operations per crafting cycle a Machine Connector sends into its machine. */
	public static final int[] CONNECTOR_OPS = {4, 8, 16, 32};
	/** Light-strip colour per tier (Basic has none). */
	public static final int[] COLOUR = {0x9AA0A8, 0x3FA9F5, 0xB15CFF, 0xFF4FD8};
	/** The most crafts per cycle an Assembly Matrix can do. */
	public static final int MATRIX_MAX_CRAFTS = 256;

	private CraftingTier() {}

	public static int of(BlockState state) {
		return state.hasProperty(TIER) ? state.getValue(TIER) : 0;
	}

	/** The tier a CPU or Accelerator item carries (from its BlockStateTag). */
	public static int of(ItemStack stack) {
		var tag = stack.getTag();
		if (tag == null || !tag.contains("BlockStateTag")) return 0;
		try {
			return Math.max(0, Math.min(MAX, Integer.parseInt(tag.getCompound("BlockStateTag").getString("tier"))));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
