package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;

import com.robvanblerk.tieredpower.item.LaserLensItem;
import com.robvanblerk.tieredpower.menu.WorkerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModMenus;
import com.robvanblerk.tieredpower.space.SatelliteData;

/**
 * The ground station of the Orbital Mining Laser: claims one of its owner's Mining Satellites, and while powered and
 * under open sky the satellite's laser drills an ore every second (2,000,000 FE each, about 100,000 FE/t). Ores come from
 * the forge:ores tag (other mods' ores too); Laser Lenses in its input slots make their ore ten times as likely.
 */
public class LaserDrillBlockEntity extends WorkerBlockEntity {
	public static final int CAPACITY = 4_000_000, MAX_INPUT = 250_000, COST = 2_000_000;
	public static final int SPECIAL_NO_SATELLITE = 1, SPECIAL_NO_SKY = 2;
	private @Nullable UUID owner;
	private int satelliteId = -1;

	public LaserDrillBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LASER_DRILL.get(), pos, state, CAPACITY, MAX_INPUT);
	}

	public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
	public int getSatelliteId() { return satelliteId; }

	@Override protected int costPerAction() { return COST; }
	@Override protected int interval() { return 20; }
	@Override public boolean isValidInput(ItemStack stack) { return stack.getItem() instanceof LaserLensItem || stack.getItem() instanceof com.robvanblerk.tieredpower.item.ItemFilterItem; }
	@Override protected MenuType<WorkerMenu> menuType() { return ModMenus.LASER_DRILL.get(); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.laser_drill"); }

	private String dim() {
		return level == null ? "" : level.dimension().location().toString();
	}

	public void release() {
		if (level instanceof ServerLevel s) SatelliteData.get(s.getServer()).release(dim(), worldPosition);
		satelliteId = -1;
	}

	@Override
	protected boolean work(ServerLevel level, BlockPos pos, Direction facing) {
		var data = SatelliteData.get(level.getServer());
		var sat = data.forDish(dim(), pos);
		if (sat == null && owner != null) sat = data.claim(dim(), pos, owner, "mining");
		satelliteId = sat == null ? -1 : sat.id;
		if (sat == null) { status = STATUS_SPECIAL; special = SPECIAL_NO_SATELLITE; return false; }
		if (!level.canSeeSky(pos.above())) { status = STATUS_SPECIAL; special = SPECIAL_NO_SKY; return false; }
		ItemStack ore = pickOre(level);
		if (ore.isEmpty()) return false;
		outputOrDrop(level, pos.above(), ore);
		return true;
	}

	/** A weighted random ore: common ores often, rare ones seldom, lens-matched ones ten times as often. */
	private ItemStack pickOre(ServerLevel level) {
		List<Item> ores = new ArrayList<>();
		List<Integer> weights = new ArrayList<>();
		int total = 0;
		for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(Tags.Items.ORES)) {
			Item item = h.value();
			if (!(item instanceof BlockItem)) continue;
			String path = BuiltInRegistries.ITEM.getKey(item).getPath();
			int w = path.contains("diamond") || path.contains("emerald") || path.contains("debris") || path.contains("uranium") ? 1
					: path.contains("gold") || path.contains("lapis") || path.contains("redstone") || path.contains("quartz") ? 3 : 6;
			ItemStack probe = new ItemStack(item);
			boolean allowed = true;
			for (int i = 0; i < INPUTS; i++) {
				ItemStack in = items.get(i);
				if (in.getItem() instanceof LaserLensItem lens && probe.is(lens.oreTag())) w *= 10;
				if (in.getItem() instanceof com.robvanblerk.tieredpower.item.ItemFilterItem && !com.robvanblerk.tieredpower.item.ItemFilterItem.passes(in, probe)) allowed = false;
			}
			if (!allowed) continue; // an Item Filter in the drill rules this ore out
			ores.add(item);
			weights.add(w);
			total += w;
		}
		if (total <= 0) return ItemStack.EMPTY;
		int r = level.random.nextInt(total);
		for (int i = 0; i < ores.size(); i++) {
			r -= weights.get(i);
			if (r < 0) return new ItemStack(ores.get(i));
		}
		return ItemStack.EMPTY;
	}

	@Override
	public AABB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB; // the beam reaches the sky
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (owner != null) tag.putUUID("owner", owner);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
	}
}
