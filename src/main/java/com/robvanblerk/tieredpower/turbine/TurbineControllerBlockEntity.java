package com.robvanblerk.tieredpower.turbine;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Industrial Turbine controller. Steam piped into any Turbine Valve fills the turbine (2,000 mB per block of inside
 * space); each Turbine Rotor lets 200 mB/t through, and more rotors also make each mB worth more (x1.3 with one rotor
 * up to x2 with 15; at most 128 rotors count). The steam condenses back into water (1 mB per 10 mB), which the valves push out for the boilers.
 * Power comes out of every valve into whatever touches it.
 */
public class TurbineControllerBlockEntity extends BlockEntity implements MenuProvider {
	public static final int STEAM_PER_BLOCK = 2_000, WATER_PER_BLOCK = 200, FLOW_PER_ROTOR = 200, STEAM_PER_WATER = 10;
	public static final long BASE_ENERGY = 1_000_000;

	private boolean formed;
	private final List<BlockPos> rotors = new ArrayList<>();
	private final List<BlockPos> valves = new ArrayList<>();
	private int volume;
	private String size = "";
	private int steam, water;
	private long energy;
	private int flow, lastGenerated;
	private float speed; // 0..1, how fast the rotors spin
	private int checkTimer, syncTimer;
	private boolean lastFormedSent;

	/** Client: the blades' current angle, advanced by the renderer. */
	public float clientAngle;
	public long clientLastFrame;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> formed ? 1 : 0;
				case 1 -> rotors.size();
				case 2 -> steam & 0xFFFF;
				case 3 -> steam >>> 16;
				case 4 -> steamCapacity() & 0xFFFF;
				case 5 -> steamCapacity() >>> 16;
				case 6 -> flow & 0xFFFF;
				case 7 -> lastGenerated & 0xFFFF;
				case 8 -> lastGenerated >>> 16;
				case 9 -> (int) (energy * 1000 / Math.max(1, energyCapacity()));
				case 10 -> Math.round(efficiency() * 100);
				case 11 -> water & 0xFFFF;
				case 12 -> water >>> 16;
				case 13 -> flow >>> 16;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return TurbineControllerMenu.DATA_COUNT; }
	};

	public TurbineControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TURBINE_CONTROLLER.get(), pos, state);
	}

	public boolean isFormed() { return formed; }
	public int steamCapacity() { return volume * STEAM_PER_BLOCK; }
	public int waterCapacity() { return volume * WATER_PER_BLOCK; }
	public long energyCapacity() { return BASE_ENERGY + 250_000L * rotors.size(); }
	/** Rotors beyond this many add nothing (keeps one turbine from outdoing an antimatter reactor). */
	public static final int MAX_USEFUL_ROTORS = 128;

	public int usefulRotors() { return Math.min(MAX_USEFUL_ROTORS, rotors.size()); }
	public int maxFlow() { return usefulRotors() * FLOW_PER_ROTOR; }
	public float efficiency() { return Math.min(2.0f, 1.25f + 0.05f * usefulRotors()); }
	public float getSpeed() { return speed; }
	public List<BlockPos> rotors() { return rotors; }
	public int getSteam() { return steam; }
	public int getWater() { return water; }
	public int getLastGenerated() { return lastGenerated; }

	/** Called by valves. */
	public int fillSteam(int amount, boolean simulate) {
		if (!formed) return 0;
		int n = Math.max(0, Math.min(amount, steamCapacity() - steam));
		if (!simulate && n > 0) { steam += n; setChanged(); }
		return n;
	}

	public int drainWater(int amount, boolean simulate) {
		int n = Math.max(0, Math.min(amount, water));
		if (!simulate && n > 0) { water -= n; setChanged(); }
		return n;
	}

	public int extractEnergy(int amount, boolean simulate) {
		int n = (int) Math.max(0, Math.min(amount, energy));
		if (!simulate && n > 0) { energy -= n; setChanged(); }
		return n;
	}

	public int getEnergyStoredInt() { return (int) Math.min(Integer.MAX_VALUE, energy); }
	public int getEnergyCapacityInt() { return (int) Math.min(Integer.MAX_VALUE, energyCapacity()); }

	public Component revalidate() {
		checkTimer = 0;
		if (level == null) return Component.empty();
		TurbineStructure.Result r = TurbineStructure.check(level, worldPosition, getBlockState().getValue(MachineBlock.FACING));
		boolean was = formed;
		int rotorsBefore = rotors.size();
		formed = r.formed();
		rotors.clear();
		valves.clear();
		if (formed) {
			rotors.addAll(r.rotors());
			valves.addAll(r.valves());
			volume = r.volume();
			size = r.size();
			steam = Math.min(steam, steamCapacity());
			water = Math.min(water, waterCapacity());
			energy = Math.min(energy, energyCapacity());
			for (BlockPos v : valves) if (level.getBlockEntity(v) instanceof TurbineValveBlockEntity valve) valve.setController(worldPosition);
		}
		if (was != formed || rotorsBefore != rotors.size()) sync();
		setChanged();
		return r.message();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, TurbineControllerBlockEntity be) {
		if (++be.checkTimer >= 20) be.revalidate();
		if (!be.formed) {
			be.flow = 0;
			be.lastGenerated = 0;
			be.speed = Math.max(0, be.speed - 0.02f);
		} else {
			double fePer = Config.get(Config.TURBINE_FE_PER_STEAM) * be.efficiency();
			int room = (int) Math.min(Integer.MAX_VALUE, (be.energyCapacity() - be.energy) / Math.max(1, fePer));
			int use = Math.max(0, Math.min(Math.min(be.steam, be.maxFlow()), room));
			be.steam -= use;
			be.flow = use;
			int gen = Config.gen((int) Math.round(use * fePer));
			be.energy += gen;
			be.lastGenerated = gen;
			be.water = Math.min(be.waterCapacity(), be.water + use / STEAM_PER_WATER);
			float target = be.maxFlow() == 0 ? 0 : (float) use / be.maxFlow();
			be.speed += (target - be.speed) * 0.05f;
			be.pushOut(level);
		}
		boolean lit = be.formed && be.flow > 0;
		if (state.getValue(MachineBlock.LIT) != lit) level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
		if (++be.syncTimer >= 20) { be.syncTimer = 0; be.sync(); }
		be.setChanged();
	}

	/** Power and condensed water out of every valve into whatever touches it (not other turbine parts). */
	private void pushOut(Level level) {
		for (BlockPos v : valves) {
			for (Direction d : Direction.values()) {
				BlockPos n = v.relative(d);
				if (TurbineStructure.isPart(level.getBlockState(n).getBlock())) continue;
				BlockEntity be = level.getBlockEntity(n);
				if (be == null) continue;
				if (energy > 0) be.getCapability(ForgeCapabilities.ENERGY, d.getOpposite()).ifPresent(e -> {
					if (e.canReceive()) energy -= e.receiveEnergy((int) Math.min(Integer.MAX_VALUE, energy), false);
				});
				if (water > 0) be.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).ifPresent(f -> {
					water -= f.fill(new net.minecraftforge.fluids.FluidStack(Fluids.WATER, water), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
				});
			}
		}
	}

	private void sync() {
		if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	// ---- client sync (rotor positions and speed for the spinning blades) ----

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		tag.putBoolean("formed", formed);
		tag.putFloat("speed", speed);
		ListTag list = new ListTag();
		for (BlockPos r : rotors) list.add(LongTag.valueOf(r.asLong()));
		tag.put("rotors", list);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		formed = tag.getBoolean("formed");
		speed = tag.getFloat("speed");
		rotors.clear();
		ListTag list = tag.getList("rotors", Tag.TAG_LONG);
		for (int i = 0; i < list.size(); i++) rotors.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
		if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
	}

	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(TurbineStructure.MAX_INTERIOR + 2);
	}

	// ---- saving ----

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("steam", steam);
		tag.putInt("water", water);
		tag.putLong("energy", energy);
		tag.putInt("volume", volume);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		steam = tag.getInt("steam");
		water = tag.getInt("water");
		energy = tag.getLong("energy");
		volume = tag.getInt("volume");
		if (tag.contains("rotors")) handleUpdateTag(tag);
	}

	/** Lines for Jade. */
	public List<String> info() {
		List<String> out = new ArrayList<>();
		if (!formed) { out.add("!Not formed - right-click for details"); return out; }
		out.add(size + ", " + rotors.size() + " rotors");
		out.add(String.format("%,d FE/t from %,d mB/t steam", lastGenerated, flow));
		out.add(String.format("Steam %,d / %,d mB", steam, steamCapacity()));
		return out;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.turbine_controller");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new TurbineControllerMenu(id, inventory, data, ContainerLevelAccess.create(level, worldPosition));
	}
}
