package com.robvanblerk.tieredpower.stargate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Stargate Dialler (a DHD): dials the Stargate ring next to it (within 8 blocks). From home, opening a gate to a planet
 * costs 50 billion FE, and keeping it open costs 1 million FE per tick (feed it from an Energy Core's Output Pylons). On a
 * planet the dialler is free: it can go home or on to any other planet. A gate dialled by hand stays open for 30 seconds;
 * one dialled by redstone stays open as long as the signal is on.
 */
public class StargateDialerBlockEntity extends BlockEntity implements MenuProvider {
	public static final long CAPACITY = 100_000_000_000L, DIAL_COST = 50_000_000_000L, UPKEEP = 1_000_000L;
	public static final int OPEN_TICKS = 600, SEARCH = 8;
	/** Redstone dialling: OFF, or -1 (home) / a Planet ordinal. */
	public static final int REDSTONE_OFF = -2;

	private long energy;
	private boolean ringFound;
	private int ticks;
	private int redstoneTarget = REDSTONE_OFF;
	private int redstoneCooldown;
	private boolean incomingCache;

	// The connection this dialler is running (not saved: an open gate closes on a world reload).
	private @Nullable StargateRing.Shape ring;
	private @Nullable ResourceKey<Level> target;
	private BlockPos targetPos = BlockPos.ZERO;
	private @Nullable GlobalPos remote;
	/** The far gate's square, so its surface can be opened too (you can walk back through either end). */
	private @Nullable StargateRing.Shape remoteShape;
	private BlockPos backPos = BlockPos.ZERO;
	private int remoteSide = 1;
	private boolean active, byRedstone, payUpkeep;
	/** Ticks of dialling left before the surface opens; then ticks left open (ignored when held open by redstone). */
	private int pendingDelay, openTicks;
	private String destName = "";

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> (int) (energy * 1000 / CAPACITY);
				case 1 -> ringFound ? 1 : 0;
				case 2 -> level != null && Planet.of(level) != null ? 1 : 0;
				case 3 -> !active ? 0 : byRedstone ? -1 : Math.max(1, Math.min(30_000, openTicks + pendingDelay));
				case 4 -> redstoneTarget;
				case 7 -> incomingCache ? 1 : 0;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return StargateDialerMenu.DATA_COUNT; }
	};

	public StargateDialerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STARGATE_DIALER.get(), pos, state);
	}

	public long getEnergy() { return energy; }
	public boolean isGateOpen() { return active; }

	public static void tick(Level level, BlockPos pos, BlockState state, StargateDialerBlockEntity be) {
		if (++be.ticks % 40 == 0) be.ringFound = be.findRing() != null;
		if (be.ticks % 20 == 0) be.incomingCache = be.hasIncoming();
		boolean powered = level.hasNeighborSignal(pos);
		if (be.redstoneCooldown > 0) be.redstoneCooldown--;
		if (be.active) be.runGate((ServerLevel) level, powered);
		else if (powered && be.redstoneTarget != REDSTONE_OFF && be.redstoneCooldown == 0) {
			if (!be.dial(null, be.redstoneTarget, true)) be.redstoneCooldown = 100;
		}
		boolean lit = be.active;
		if (state.getValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT) != lit)
			level.setBlock(pos, state.setValue(com.robvanblerk.tieredpower.block.MachineBlock.LIT, lit), Block.UPDATE_ALL);
	}

	/** Every tick while connected: upkeep, keeping both rings and the surface alive, and deciding when to shut. */
	private void runGate(ServerLevel level, boolean powered) {
		StargateRing.Shape r = ring;
		if (r == null) { close(); return; }
		if (byRedstone && !powered) { close(); return; }
		if (pendingDelay > 0) {
			if (--pendingDelay == 0) openSurface(r);
		} else {
			if (payUpkeep) {
				if (energy < UPKEEP) { close(); return; }
				energy -= UPKEEP;
				setChanged();
			}
			if (!byRedstone && --openTicks <= 0) { close(); return; }
			if (ticks % 10 == 0) {
				int alive = 0;
				for (BlockPos p : r.inside()) if (level.getBlockEntity(p) instanceof EventHorizonBlockEntity h) { h.keepAlive(40); alive++; }
				if (alive == 0) { close(); return; } // the surface was broken up (blocks placed in it)
			}
		}
		if (level.getBlockEntity(r.master()) instanceof StargateRingBlockEntity g) g.keepAlive();
		ServerLevel there = remoteLevel();
		if (there != null && remote != null) {
			if (ticks % 100 == 0 || ticks % 100 == 1) loadRemote(there);
			if (there.getBlockEntity(remote.pos()) instanceof StargateRingBlockEntity g) g.keepAlive();
			if (pendingDelay == 0 && remoteShape != null && ticks % 10 == 0)
				for (BlockPos p : remoteShape.inside()) if (there.getBlockEntity(p) instanceof EventHorizonBlockEntity h) h.keepAlive(40);
		}
	}

	private @Nullable ServerLevel remoteLevel() {
		return remote == null || level == null || level.getServer() == null ? null : level.getServer().getLevel(remote.dimension());
	}

	/** Keeps the far gate's chunks loaded (a short-lived ticket, renewed while the gate is open, never saved). */
	private void loadRemote(ServerLevel there) {
		if (remote == null) return;
		there.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL, new net.minecraft.world.level.ChunkPos(remote.pos()), 3, remote.pos());
	}

	// ---- the ring ----

	/** The ring next to this dialler (a complete 7x7 square of frame), or null. */
	public @Nullable StargateRing.Shape findRing() {
		return level == null ? null : StargateRing.near(level, worldPosition, SEARCH);
	}

	private void openSurface(StargateRing.Shape r) {
		if (level == null || target == null) return;
		fill(level, r, target, targetPos);
		ServerLevel there = remoteLevel();
		if (there != null && remoteShape != null) fill(there, remoteShape, level.dimension(), backPos);
	}

	/** Puts an event horizon in a ring, leading to 'dest' / 'at'. */
	private static void fill(Level lvl, StargateRing.Shape r, ResourceKey<Level> dest, BlockPos at) {
		boolean hidden = StargateRing.isFormed(lvl, r);
		for (BlockPos p : r.inside()) {
			if (!lvl.getBlockState(p).isAir() && !lvl.getBlockState(p).is(ModBlocks.EVENT_HORIZON.get())) continue;
			lvl.setBlock(p, ModBlocks.EVENT_HORIZON.get().defaultBlockState().setValue(EventHorizonBlock.AXIS, r.axis()).setValue(EventHorizonBlock.HIDDEN, hidden), Block.UPDATE_ALL);
			if (lvl.getBlockEntity(p) instanceof EventHorizonBlockEntity h) h.open(dest, at, 40);
		}
		lvl.playSound(null, r.inside().get(r.inside().size() / 2), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6f, 1.2f);
	}

	private static void clear(Level lvl, StargateRing.Shape r) {
		for (BlockPos p : r.inside()) if (lvl.getBlockState(p).is(ModBlocks.EVENT_HORIZON.get())) lvl.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
	}

	/** True when this dialler's gate is the far end of a connection opened by another dialler. */
	public boolean hasIncoming() {
		if (active || level == null) return false;
		StargateRing.Shape r = findRing();
		return r != null && Wormholes.other(GlobalPos.of(level.dimension(), r.master())) != null;
	}

	/** Close gate pressed on the far end of a connection: shut it from the dialler that opened it. */
	public void closeIncoming(@Nullable ServerPlayer player) {
		if (level == null || level.getServer() == null) return;
		StargateRing.Shape r = findRing();
		if (r == null) return;
		GlobalPos owner = Wormholes.owner(GlobalPos.of(level.dimension(), r.master()));
		ServerLevel ol = owner == null ? null : level.getServer().getLevel(owner.dimension());
		if (ol != null && ol.isLoaded(owner.pos()) && ol.getBlockEntity(owner.pos()) instanceof StargateDialerBlockEntity d && d.active) {
			d.close();
			if (d.byRedstone) d.redstoneCooldown = 200;
			tell(player, "Connection closed", ChatFormatting.AQUA, true);
		} else {
			// the dialler is gone or unloaded: just let go of this end
			Wormholes.disconnect(GlobalPos.of(level.dimension(), r.master()));
			if (level.getBlockEntity(r.master()) instanceof StargateRingBlockEntity g) g.close();
			for (BlockPos p : r.inside()) if (level.getBlockState(p).is(ModBlocks.EVENT_HORIZON.get())) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			tell(player, "Connection closed", ChatFormatting.AQUA, true);
		}
	}

	/** Shuts the gate: the surface goes, both rings power down and the connection ends. */
	public void close() {
		if (level == null) return;
		StargateRing.Shape r = ring;
		if (r != null) {
			clear(level, r);
			if (level.getBlockEntity(r.master()) instanceof StargateRingBlockEntity g) g.close();
			Wormholes.disconnect(GlobalPos.of(level.dimension(), r.master()));
			if (active) level.playSound(null, r.master().above(3), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5f, 0.7f);
		}
		ServerLevel there = remoteLevel();
		if (there != null && remote != null && there.isLoaded(remote.pos())) {
			if (there.getBlockEntity(remote.pos()) instanceof StargateRingBlockEntity g) g.close();
			if (remoteShape != null) clear(there, remoteShape);
		}
		active = false;
		byRedstone = false;
		ring = null;
		remote = null;
		remoteShape = null;
		pendingDelay = 0;
		openTicks = 0;
		setChanged();
	}

	// ---- dialling ----

	public int getRedstoneTarget() { return redstoneTarget; }

	/** Cycles what a redstone signal dials: off, home (planets only), then each planet other than this one. */
	public void cycleRedstoneTarget(Player player) {
		Planet here = level == null ? null : Planet.of(level);
		StargateData known = level == null || level.getServer() == null ? null : StargateData.get(level.getServer());
		int n = Planet.values().length;
		int t = redstoneTarget;
		for (int i = 0; i < n + 3; i++) {
			t = t + 1 >= n ? REDSTONE_OFF : t + 1;
			if (t == REDSTONE_OFF) break;
			if (t == -1 && here == null) continue;
			if (t >= 0 && Planet.values()[t] == here) continue;
			if (t >= 0 && known != null && !known.knows(player.getUUID(), Planet.values()[t])) continue;
			break;
		}
		redstoneTarget = t;
		setChanged();
	}

	private static String name(int choice) {
		return choice < 0 ? "home" : Planet.values()[choice].title;
	}

	private void tell(@Nullable ServerPlayer player, String msg, ChatFormatting colour, boolean actionBar) {
		if (player != null) player.displayClientMessage(Component.literal(msg).withStyle(colour), actionBar);
	}

	/**
	 * 'choice': -1 = home, otherwise a Planet ordinal. 'player' is who pressed the dome (null when dialled by redstone,
	 * which holds the gate open while the signal stays on). Returns true if the gate is dialling.
	 */
	public boolean dial(@Nullable ServerPlayer player, int choice, boolean redstone) {
		if (!(level instanceof ServerLevel server)) return false;
		if (active) { tell(player, "The gate is already open", ChatFormatting.YELLOW, true); return false; }
		StargateRing.Shape r = findRing();
		if (r == null) {
			tell(player, "No complete Stargate ring within " + SEARCH + " blocks (a 7x7 square of Stargate Frame, empty inside)", ChatFormatting.RED, false);
			return false;
		}
		GlobalPos myGate = GlobalPos.of(level.dimension(), r.master());
		GlobalPos incoming = Wormholes.other(myGate);
		if (incoming == null && level.getBlockEntity(r.master()) instanceof StargateRingBlockEntity g && g.isActive()) {
			tell(player, "That gate is busy - wait a moment", ChatFormatting.YELLOW, true);
			return false;
		}
		Planet here = Planet.of(level);
		StargateData data = StargateData.get(server.getServer());
		ResourceKey<Level> dest;
		BlockPos destPos;
		GlobalPos far = null;
		if (choice < 0) {
			if (here == null) { tell(player, "You're already home", ChatFormatting.YELLOW, true); return false; }
			GlobalPos home = data.homeGate(here);
			if (player != null && player.getPersistentData().contains("tpGateHomeDim")) {
				var tag = player.getPersistentData();
				ResourceLocation dim = ResourceLocation.tryParse(tag.getString("tpGateHomeDim"));
				dest = ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.location() : dim);
				destPos = tag.contains("tpGateHomePos") ? BlockPos.of(tag.getLong("tpGateHomePos")) : server.getServer().overworld().getSharedSpawnPos();
			} else if (home != null) {
				dest = home.dimension();
				BlockPos spot = data.homeSpot(here);
				destPos = spot != null ? spot : home.pos();
			} else {
				tell(player, "No gate at home has dialled " + here.title + " yet", ChatFormatting.RED, false);
				return false;
			}
			ServerLevel homeLevel = server.getServer().getLevel(dest);
			if (homeLevel != null) {
				// the gate standing by where you'll come out (or failing that, the one that last dialled here)
				homeLevel.getChunk(destPos);
				StargateRing.Shape h = StargateRing.near(homeLevel, destPos, 10);
				if (h != null) far = GlobalPos.of(dest, h.master());
				else if (home != null && home.dimension().equals(dest)) far = home;
			}
		} else {
			Planet p = Planet.values()[Math.max(0, Math.min(Planet.values().length - 1, choice))];
			if (p == here) { tell(player, "You're already on " + p.title, ChatFormatting.YELLOW, true); return false; }
			dest = p.level;
			destPos = BlockPos.ZERO; // the planet's arrival platform
		}
		ServerLevel there = server.getServer().getLevel(dest);
		if (there == null) {
			tell(player, "That world isn't available on this server", ChatFormatting.RED, false);
			return false;
		}
		if (choice >= 0) {
			// make sure the planet's gate exists, and connect to it
			BlockPos arrival = StargateTravel.arrival(there);
			StargateRing.Shape a = StargateRing.near(there, arrival, 6);
			if (a != null) far = GlobalPos.of(dest, a.master());
		}
		boolean fromHome = here == null;
		if (fromHome && energy < DIAL_COST + UPKEEP * 20) {
			tell(player, String.format("Needs 50 billion FE to open a gate, plus 1 million FE/t to keep it open (it has %,d)", energy), ChatFormatting.RED, false);
			return false;
		}
		if (incoming != null) {
			// this gate is the far end of a connection another dialler opened (e.g. one held open from a planet)
			if (incoming.equals(far)) {
				tell(player, "Already connected to " + name(choice) + " - the gate is open, walk through", ChatFormatting.AQUA, true);
			} else {
				tell(player, "That gate is busy with another connection - press Close gate (or the dome) to shut it first", ChatFormatting.YELLOW, false);
			}
			return false;
		}
		if (far != null && there.getBlockEntity(far.pos()) instanceof StargateRingBlockEntity fg && fg.isActive()
				&& !myGate.equals(Wormholes.other(far))) {
			tell(player, "The gate at the other end is busy", ChatFormatting.YELLOW, true);
			return false;
		}
		if (fromHome) energy -= DIAL_COST;
		if (player != null && choice >= 0 && !Planet.values()[choice].known && !data.knows(player.getUUID(), Planet.values()[choice])) {
			data.learn(player.getUUID(), Planet.values()[choice]);
			tell(player, "New address: " + Planet.values()[choice].title + " - added to your address list", ChatFormatting.GOLD, false);
		}

		// A home gate dialling a planet becomes that planet's way home.
		int mySide = sideOf(r, worldPosition);
		if (fromHome && choice >= 0) data.setHomeGate(Planet.values()[choice], GlobalPos.of(level.dimension(), r.master()), StargateRing.front(r, mySide, 3));

		ring = r;
		target = dest;
		targetPos = destPos;
		remote = far;
		remoteShape = far == null ? null : StargateRing.near(there, far.pos(), 1);
		backPos = StargateRing.front(r, mySide, 3);
		active = true;
		byRedstone = redstone;
		payUpkeep = fromHome;
		openTicks = OPEN_TICKS;
		destName = name(choice);
		Wormholes.disconnect(GlobalPos.of(level.dimension(), r.master()));
		if (far != null) {
			Wormholes.connect(GlobalPos.of(level.dimension(), r.master()), far);
			Wormholes.setOwner(GlobalPos.of(level.dimension(), r.master()), far, GlobalPos.of(level.dimension(), worldPosition));
			loadRemote(there);
			there.getChunk(far.pos());
			if (there.getBlockEntity(far.pos()) instanceof StargateRingBlockEntity fg) {
				// the gate at the other end lights up too, its vortex bursting out over the arrival spot
				BlockPos landing = choice >= 0 ? StargateTravel.arrival(there) : destPos;
				fg.dial(StargateRingBlockEntity.HELD, remoteShape == null ? 1 : sideOf(remoteShape, landing));
			}
		}
		if (StargateRing.isFormed(level, r) && level.getBlockEntity(r.master()) instanceof StargateRingBlockEntity gate) {
			// A formed gate dials first (chevrons lock, the inner ring spins), then the vortex bursts out on the DHD's side.
			gate.dial(StargateRingBlockEntity.HELD, mySide);
			pendingDelay = StargateRingBlockEntity.DIAL_TICKS;
			level.playSound(null, r.master().above(3), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 0.6f);
		} else {
			pendingDelay = 0;
			openSurface(r);
			level.playSound(null, r.inside().get(r.inside().size() / 2), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 0.6f);
		}
		tell(player, "Gate open to " + destName + (redstone ? " while the redstone signal is on" : " for 30 seconds - walk through")
				+ (fromHome ? " (1 million FE/t to keep open)" : ""), ChatFormatting.AQUA, true);
		setChanged();
		return true;
	}

	/** +1 or -1: which side of the ring (along its normal) a position is on. */
	private static int sideOf(StargateRing.Shape s, BlockPos p) {
		int off = s.axis() == Direction.Axis.X ? p.getZ() - s.master().getZ() : p.getX() - s.master().getX();
		return off < 0 ? -1 : 1;
	}


	// ---- power ----

	private final IEnergyStorage power = new IEnergyStorage() {
		@Override
		public int receiveEnergy(int max, boolean simulate) {
			int n = (int) Math.max(0, Math.min(max, CAPACITY - energy));
			if (!simulate && n > 0) { energy += n; setChanged(); }
			return n;
		}
		@Override public int extractEnergy(int max, boolean simulate) { return 0; }
		@Override public int getEnergyStored() { return (int) Math.min(Integer.MAX_VALUE, energy); }
		@Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
		@Override public boolean canExtract() { return false; }
		@Override public boolean canReceive() { return true; }
	};
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> power);

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return cap == ForgeCapabilities.ENERGY ? energyCap.cast() : super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); energyCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); energyCap = LazyOptional.of(() -> power); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putLong("energy", energy);
		tag.putInt("redstoneTarget", redstoneTarget);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy = tag.getLong("energy");
		redstoneTarget = tag.contains("redstoneTarget") ? tag.getInt("redstoneTarget") : REDSTONE_OFF;
	}

	/** Lines for Jade. */
	public List<String> info() {
		List<String> out = new ArrayList<>();
		if (level != null && Planet.of(level) != null) out.add("On " + Planet.of(level).title + " - dialling is free");
		else out.add(String.format("%,d / %,d FE (50 billion per gate, 1M FE/t open)", energy, CAPACITY));
		if (!ringFound) out.add("!No Stargate ring found nearby");
		if (active) out.add(byRedstone ? "Gate open to " + destName + " (redstone)" : "Gate open to " + destName + ": " + (openTicks + pendingDelay) / 20 + " s");
		if (redstoneTarget != REDSTONE_OFF) out.add("Redstone dials " + name(redstoneTarget));
		return out;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.stargate_dialer");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		// the same data, plus which hidden worlds this player has found the address of
		ContainerData mine = new ContainerData() {
			@Override
			public int get(int i) {
				if (i != 5 && i != 6) return data.get(i);
				int mask = player.getServer() == null ? 0 : StargateData.get(player.getServer()).knownMask(player.getUUID());
				return i == 5 ? (short) (mask & 0xFFFF) : (short) ((mask >>> 16) & 0xFFFF);
			}
			@Override public void set(int i, int v) {}
			@Override public int getCount() { return StargateDialerMenu.DATA_COUNT; }
		};
		return new StargateDialerMenu(id, inventory, mine, ContainerLevelAccess.create(level, worldPosition));
	}
}
