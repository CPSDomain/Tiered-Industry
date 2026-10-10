package com.robvanblerk.tieredpower.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Machine Status Display: every 2 seconds it checks every machine within 24 blocks and sorts them into Working, Idle,
 * Blocked (has things to do but isn't running - output full or a missing ingredient), No power and Off (stopped by
 * redstone). Its screen lists them, its face lights green / amber / red, and a comparator reads how many need attention.
 */
public class MachineStatusDisplayBlockEntity extends BlockEntity implements MenuProvider {
	public static final int RANGE = 24;
	public enum Status {
		NO_POWER("No power", 0xFF5050), BLOCKED("Blocked", 0xFFB040), OFF("Off (redstone)", 0xA0A0A0), IDLE("Idle", 0xC8C8C8), WORKING("Working", 0x60E060);
		public final String label;
		public final int colour;
		Status(String label, int colour) { this.label = label; this.colour = colour; }
	}

	public record Entry(Status status, String name, BlockPos pos, String tier) {
		/** status|name|x|y|z|tier, for the screen. */
		public String encode() {
			return status.ordinal() + "|" + name + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ() + "|" + tier;
		}
	}

	private List<Entry> entries = List.of();
	private int problems;
	private int ticks;

	public MachineStatusDisplayBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MACHINE_STATUS_DISPLAY.get(), pos, state);
	}

	public List<Entry> entries() { return entries; }
	public int problems() { return problems; }

	public static void tick(Level level, BlockPos pos, BlockState state, MachineStatusDisplayBlockEntity be) {
		if (be.ticks++ % 40 != 0) return;
		be.scan(level);
		int light = be.entries.stream().anyMatch(e -> e.status() == Status.NO_POWER) ? 2 : be.entries.stream().anyMatch(e -> e.status() == Status.BLOCKED) ? 1 : 0;
		if (state.getValue(MachineStatusDisplayBlock.STATUS) != light) level.setBlock(pos, state.setValue(MachineStatusDisplayBlock.STATUS, light), Block.UPDATE_ALL);
		level.updateNeighbourForOutputSignal(pos, state.getBlock());
	}

	private void scan(Level level) {
		List<Entry> out = new ArrayList<>();
		ChunkPos c0 = new ChunkPos(worldPosition.offset(-RANGE, 0, -RANGE)), c1 = new ChunkPos(worldPosition.offset(RANGE, 0, RANGE));
		for (int cx = c0.x; cx <= c1.x; cx++) for (int cz = c0.z; cz <= c1.z; cz++) {
			if (!level.hasChunk(cx, cz)) continue;
			for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
				if (!(be instanceof MachineBlockEntity m)) continue;
				BlockPos p = be.getBlockPos();
				if (Math.abs(p.getX() - worldPosition.getX()) > RANGE || Math.abs(p.getY() - worldPosition.getY()) > RANGE || Math.abs(p.getZ() - worldPosition.getZ()) > RANGE) continue;
				out.add(new Entry(status(level, m), m.getDisplayName().getString(), p.immutable(), m.supportsTiers() ? MachineBlockEntity.TIER_NAMES[m.getTier()] : ""));
			}
		}
		out.sort(Comparator.<Entry>comparingInt(e -> e.status().ordinal()).thenComparing(Entry::name));
		entries = out;
		problems = (int) out.stream().filter(e -> e.status() == Status.NO_POWER || e.status() == Status.BLOCKED).count();
	}

	private static Status status(Level level, MachineBlockEntity m) {
		BlockState st = m.getBlockState();
		if (st.hasProperty(MachineBlock.LIT) && st.getValue(MachineBlock.LIT)) return Status.WORKING;
		if (!m.getRedstoneMode().allows(level.hasNeighborSignal(m.getBlockPos()))) return Status.OFF;
		boolean hasWork = false;
		for (int i = 0; i < m.getContainerSize(); i++) {
			ItemStack s = m.getItem(i);
			if (s.isEmpty()) continue;
			String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
			if (id.contains("upgrade") || id.contains("installer") || id.equals("muffler")) continue;
			hasWork = true;
			break;
		}
		int cap = m.energy.getMaxEnergyStored();
		if (cap > 0 && m.energy.getEnergyStored() < Math.max(1, cap / 200)) return hasWork ? Status.NO_POWER : Status.IDLE;
		return hasWork ? Status.BLOCKED : Status.IDLE;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.machine_status_display");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new MachineStatusMenu(id, inventory, this, player);
	}
}
