package com.robvanblerk.tieredpower.logic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.ItemKey;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * Up to 4 rules: IF (item count in storage / power % of touching batteries / redstone in) THEN redstone out of a side.
 * Rules are checked twice a second; a side that any true rule points at gives a full-strength signal.
 */
public class LogicControllerBlockEntity extends BlockEntity implements MenuProvider {
	public static final int RULES = 4;
	public static final int MODE_OFF = 0, MODE_ITEM = 1, MODE_POWER = 2, MODE_REDSTONE = 3, MODES = 4;
	/** Output sides: 0-5 = Direction.from3DDataValue (Down, Up, North, South, West, East), 6 = all. */
	public static final int SIDE_ALL = 6, SIDES = 7;
	public static final int STATUS_FALSE = 0, STATUS_TRUE = 1, STATUS_NOT_READY = 2;

	final SimpleContainer items = new SimpleContainer(RULES) {
		@Override public void setChanged() { super.setChanged(); LogicControllerBlockEntity.this.setChanged(); }
	};
	final int[] mode = new int[RULES], side = new int[RULES], threshold = new int[RULES], status = new int[RULES];
	final boolean[] below = new boolean[RULES];
	final long[] current = new long[RULES];
	private int outputs; // bit per Direction
	private int ticks;

	public LogicControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LOGIC_CONTROLLER.get(), pos, state);
		for (int i = 0; i < RULES; i++) { below[i] = true; side[i] = SIDE_ALL; status[i] = STATUS_NOT_READY; }
	}

	/** Signal strength out of the controller's face 'face'. */
	public int signalOut(Direction face) {
		return (outputs & (1 << face.get3DDataValue())) != 0 ? 15 : 0;
	}

	public void cycleMode(int r) { mode[r] = (mode[r] + 1) % MODES; if (mode[r] == MODE_POWER) threshold[r] = Math.min(threshold[r], 100); setChanged(); }
	public void toggleCompare(int r) { below[r] = !below[r]; setChanged(); }
	public void cycleSide(int r) { side[r] = (side[r] + 1) % SIDES; setChanged(); }
	public void setThreshold(int r, int v) { threshold[r] = Math.max(0, mode[r] == MODE_POWER ? Math.min(100, v) : v); setChanged(); }

	private long itemCount(Level level, ItemStack want) {
		if (want.isEmpty()) return -1;
		var c = StorageNetwork.findController(level, worldPosition);
		if (c == null || !c.isOnline() || c.getNetwork() == null) return -1;
		return c.getNetwork().count(new ItemKey(want));
	}

	/** % full of the batteries/stores touching the controller (cables don't count), or -1 if none. */
	private long powerPercent(Level level) {
		long stored = 0, cap = 0;
		for (Direction d : Direction.values()) {
			BlockEntity n = level.getBlockEntity(worldPosition.relative(d));
			if (n == null || n instanceof com.robvanblerk.tieredpower.block.entity.CableBlockEntity) continue;
			var e = n.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).orElse(null);
			if (e == null || e.getMaxEnergyStored() <= 0) continue;
			stored += e.getEnergyStored();
			cap += e.getMaxEnergyStored();
		}
		return cap <= 0 ? -1 : stored * 100 / cap;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, LogicControllerBlockEntity be) {
		if (level.isClientSide() || ++be.ticks % 10 != 0) return;
		int out = 0;
		for (int r = 0; r < RULES; r++) {
			long value = switch (be.mode[r]) {
				case MODE_ITEM -> be.itemCount(level, be.items.getItem(r));
				case MODE_POWER -> be.powerPercent(level);
				case MODE_REDSTONE -> level.hasNeighborSignal(pos) ? 1 : 0;
				default -> -1;
			};
			be.current[r] = value;
			boolean truth;
			if (be.mode[r] == MODE_OFF || value < 0) { be.status[r] = STATUS_NOT_READY; continue; }
			truth = be.mode[r] == MODE_REDSTONE ? value > 0 : be.below[r] ? value < be.threshold[r] : value > be.threshold[r];
			be.status[r] = truth ? STATUS_TRUE : STATUS_FALSE;
			if (truth) out |= be.side[r] == SIDE_ALL ? 0b111111 : 1 << be.side[r];
		}
		if (out != be.outputs) {
			be.outputs = out;
			be.setChanged();
			level.updateNeighborsAt(pos, state.getBlock());
		}
	}

	public final ContainerData data = new ContainerData() {
		// per rule (8 values): mode, below, side, threshold lo, threshold hi, status, current lo, current hi
		@Override
		public int get(int i) {
			int r = i / 8;
			long cur = Math.max(0, Math.min(Integer.MAX_VALUE, current[r]));
			return switch (i % 8) {
				case 0 -> mode[r];
				case 1 -> below[r] ? 1 : 0;
				case 2 -> side[r];
				case 3 -> threshold[r] & 0xFFFF;
				case 4 -> (threshold[r] >>> 16) & 0xFFFF;
				case 5 -> status[r];
				case 6 -> (int) cur & 0xFFFF;
				default -> ((int) cur >>> 16) & 0xFFFF;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return RULES * 8; }
	};

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		NonNullList<ItemStack> list = NonNullList.withSize(RULES, ItemStack.EMPTY);
		for (int i = 0; i < RULES; i++) list.set(i, items.getItem(i));
		ContainerHelper.saveAllItems(tag, list);
		tag.putIntArray("mode", mode);
		tag.putIntArray("side", side);
		tag.putIntArray("threshold", threshold);
		int b = 0;
		for (int i = 0; i < RULES; i++) if (below[i]) b |= 1 << i;
		tag.putInt("below", b);
		tag.putInt("outputs", outputs);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		NonNullList<ItemStack> list = NonNullList.withSize(RULES, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, list);
		for (int i = 0; i < RULES; i++) items.setItem(i, list.get(i));
		int[] m = tag.getIntArray("mode"), s = tag.getIntArray("side"), t = tag.getIntArray("threshold");
		for (int i = 0; i < RULES; i++) {
			mode[i] = i < m.length ? Math.floorMod(m[i], MODES) : MODE_OFF;
			side[i] = i < s.length ? Math.floorMod(s[i], SIDES) : SIDE_ALL;
			threshold[i] = i < t.length ? t[i] : 0;
		}
		int b = tag.contains("below") ? tag.getInt("below") : 0b1111;
		for (int i = 0; i < RULES; i++) below[i] = (b & (1 << i)) != 0;
		outputs = tag.getInt("outputs");
	}

	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.logic_controller"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new LogicControllerMenu(id, inv, this);
	}
}
