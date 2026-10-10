package com.robvanblerk.tieredpower.stargate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Builds each planet's arrival platform (a gate, a dialling device and a floor) the first time someone arrives. */
public final class StargateTravel {
	private StargateTravel() {}

	/** Where travellers land on this planet. */
	public static BlockPos arrival(ServerLevel level) {
		Planet planet = Planet.of(level);
		StargateData data = StargateData.get(level.getServer());
		if (planet != null && data.arrival(planet) != null) return data.arrival(planet);
		int ox = planet == null ? 0 : planet.originX;
		level.getChunk(ox >> 4, 0);
		int y;
		if (planet != null && planet.cave) y = planet.caveY;
		else {
			y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox, 0);
			if (y <= level.getMinBuildHeight() + 2) y = 100; // over the void (Skylands): build a floating platform
		}
		y = Math.max(level.getMinBuildHeight() + 6, Math.min(y, level.getMinBuildHeight() + level.getLogicalHeight() - 12));
		build(level, new BlockPos(ox, y, 0), planet);
		BlockPos arrival = new BlockPos(ox, y, 2);
		if (planet != null) data.setArrival(planet, arrival);
		return arrival;
	}

	private static void build(ServerLevel level, BlockPos base, Planet planet) {
		BlockState air = Blocks.AIR.defaultBlockState(), floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState(), edge = Blocks.DEEPSLATE_TILES.defaultBlockState();
		for (int x = -5; x <= 5; x++) for (int z = -4; z <= 4; z++) {
			for (int dy = 0; dy <= 8; dy++) level.setBlock(base.offset(x, dy, z), air, Block.UPDATE_CLIENTS);
			level.setBlock(base.offset(x, -1, z), Math.abs(x) == 5 || Math.abs(z) == 4 ? edge : floor, Block.UPDATE_CLIENTS);
		}
		// The ring: 7 wide, 7 tall, standing on the floor, facing along Z.
		BlockState frame = ModBlocks.STARGATE_FRAME.get().defaultBlockState();
		for (int x = -3; x <= 3; x++) for (int dy = 0; dy <= 6; dy++)
			if (Math.abs(x) == 3 || dy == 0 || dy == 6) level.setBlock(base.offset(x, dy, 0), frame, Block.UPDATE_ALL);
		// Shape it into a round gate straight away.
		StargateRing.Shape shape = StargateRing.check(level, base.offset(-3, 0, 0), net.minecraft.core.Direction.Axis.X);
		if (shape != null) StargateRing.form(level, shape, true);
		// A dialling device beside the landing spot (free to use on a planet).
		level.setBlock(base.offset(2, 0, 3), ModBlocks.STARGATE_DIALER.get().defaultBlockState(), Block.UPDATE_ALL);
		// A supply chest - with the address of a hidden world, so every planet leads on to another.
		BlockPos chest = base.offset(-2, 0, 3);
		level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.NORTH), Block.UPDATE_ALL);
		if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity c) {
			var rnd = level.getRandom();
			java.util.List<Planet> hidden = Planet.hidden();
			hidden.remove(planet);
			if (!hidden.isEmpty()) c.setItem(13, AddressTabletItem.of(hidden.get(rnd.nextInt(hidden.size()))));
			c.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH, 16));
			c.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 8));
			c.setItem(2, new net.minecraft.world.item.ItemStack(Blocks.COBBLESTONE, 64));
		}
		// Light.
		for (int x : new int[] {-5, 5}) for (int z : new int[] {-4, 4}) level.setBlock(base.offset(x, 0, z), Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
	}
}
