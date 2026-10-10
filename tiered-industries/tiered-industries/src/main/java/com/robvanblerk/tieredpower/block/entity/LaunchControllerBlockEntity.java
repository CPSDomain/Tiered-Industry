package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.LaunchPadBlock;
import com.robvanblerk.tieredpower.menu.LaunchControllerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;
import com.robvanblerk.tieredpower.space.RocketEntity;
import com.robvanblerk.tieredpower.space.SatelliteData;

/**
 * Runs a launch site: a 3x3 Launch Pad touching this controller, with a Launch Tower beside the pad. Put a Rocket and a
 * Solar Satellite in its slots and pipe in 16,000 mB of Rocket Fuel; the rocket stands on the middle of the pad. Press
 * Launch: a 10 second countdown, then lift-off - and the satellite reaches orbit, where a Receiver Dish can claim it.
 * Slots: 0 rocket, 1 payload.
 */
public class LaunchControllerBlockEntity extends BlockEntity implements MenuProvider {
	public static final int FUEL_NEEDED = 16_000, COUNTDOWN = 200;
	public static final int OK = 0, NO_PAD = 1, NO_TOWER = 2;
	public final SimpleContainer items = new SimpleContainer(2) {
		@Override public void setChanged() { super.setChanged(); LaunchControllerBlockEntity.this.setChanged(); }
		@Override public boolean canPlaceItem(int slot, ItemStack s) { return slot == 0 ? s.is(ModBlocks.ROCKET.get()) : s.is(ModBlocks.SOLAR_SATELLITE.get()) || s.is(ModBlocks.MINING_SATELLITE.get()); }
		@Override public int getMaxStackSize() { return 1; }
	};
	private int fuel, ticks, countdown = -1, structure = NO_PAD;
	private @Nullable BlockPos centre;
	private @Nullable UUID rocketId, launcher;
	private String launcherName = "";

	private final IFluidHandler tank = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return fuel > 0 ? new FluidStack(ModFluids.ROCKET_FUEL.get(), fuel) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return FUEL_NEEDED; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return s.getFluid() == ModFluids.ROCKET_FUEL.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (r.getFluid() != ModFluids.ROCKET_FUEL.get() || countdown >= 0) return 0;
			int n = Math.min(r.getAmount(), FUEL_NEEDED - fuel);
			if (a.execute() && n > 0) { fuel += n; setChanged(); }
			return Math.max(0, n);
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
		@Override public @NotNull FluidStack drain(int m, FluidAction a) { return FluidStack.EMPTY; }
	};
	private LazyOptional<IFluidHandler> tankCap = LazyOptional.of(() -> tank);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> fuel;
				case 1 -> structure;
				case 2 -> countdown < 0 ? -1 : (countdown + 19) / 20;
				case 3 -> level instanceof ServerLevel s ? SatelliteData.get(s.getServer()).all().size() : 0;
				case 4 -> level instanceof ServerLevel s ? SatelliteData.get(s.getServer()).claimed() : 0;
				case 5 -> skyClear() ? 1 : 0;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return LaunchControllerMenu.DATA_COUNT; }
	};

	public LaunchControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LAUNCH_CONTROLLER.get(), pos, state);
	}

	private static boolean isPad(BlockState s) {
		return s.getBlock() instanceof LaunchPadBlock p && !p.isTower();
	}

	private static boolean isTower(BlockState s) {
		return s.getBlock() instanceof LaunchPadBlock p && p.isTower();
	}

	/** Finds the 3x3 pad touching the controller and a tower beside it. */
	private void checkStructure(Level level) {
		centre = null;
		BlockPos start = null;
		for (Direction d : Direction.values()) if (isPad(level.getBlockState(worldPosition.relative(d)))) { start = worldPosition.relative(d); break; }
		if (start == null) { structure = NO_PAD; return; }
		Set<BlockPos> pads = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		while (!queue.isEmpty() && pads.size() <= 9) {
			BlockPos p = queue.poll();
			if (pads.contains(p) || !isPad(level.getBlockState(p))) continue;
			pads.add(p);
			for (Direction d : Direction.Plane.HORIZONTAL) queue.add(p.relative(d));
		}
		int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (BlockPos p : pads) { minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX()); minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ()); }
		if (pads.size() != 9 || maxX - minX != 2 || maxZ - minZ != 2) { structure = NO_PAD; return; }
		boolean tower = false;
		for (BlockPos p : pads)
			for (Direction d : Direction.Plane.HORIZONTAL)
				if (isTower(level.getBlockState(p.relative(d)))) tower = true;
		if (!tower) { structure = NO_TOWER; return; }
		structure = OK;
		centre = new BlockPos(minX + 1, start.getY(), minZ + 1);
	}

	public boolean skyClear() {
		return centre != null && level != null && level.getHeight(Heightmap.Types.MOTION_BLOCKING, centre.getX(), centre.getZ()) <= centre.getY() + 1;
	}

	private @Nullable RocketEntity rocket(ServerLevel level) {
		return rocketId != null && level.getEntity(rocketId) instanceof RocketEntity r ? r : null;
	}

	/** Press Launch: check everything, then start the countdown. */
	public void startLaunch(Player player) {
		if (!(level instanceof ServerLevel server) || countdown >= 0) return;
		String problem = structure == NO_PAD ? "Build a 3x3 Launch Pad touching the controller"
				: structure == NO_TOWER ? "Put a Launch Tower beside the pad"
				: items.getItem(0).isEmpty() ? "Put a Rocket in the controller"
				: items.getItem(1).isEmpty() ? "Put a payload (Solar or Mining Satellite) in the controller"
				: fuel < FUEL_NEEDED ? String.format("Needs %,d more mB of Rocket Fuel", FUEL_NEEDED - fuel)
				: !skyClear() ? "The sky above the pad must be clear"
				: rocket(server) == null ? "The rocket isn't on the pad yet" : null;
		if (problem != null) {
			player.displayClientMessage(Component.literal(problem).withStyle(ChatFormatting.RED), true);
			return;
		}
		launcher = player.getUUID();
		launcherName = player.getName().getString();
		countdown = COUNTDOWN;
		setChanged();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, LaunchControllerBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		if (be.ticks++ % 40 == 0) be.checkStructure(level);
		RocketEntity rocket = be.rocket(server);
		// Keep a rocket standing on the pad while there's one in the slot (and none while there isn't).
		if (be.countdown < 0) {
			boolean want = be.structure == OK && !be.items.getItem(0).isEmpty() && be.centre != null;
			if (want && (rocket == null || rocket.isLaunching())) {
				RocketEntity r = ModEntities.ROCKET.get().create(server);
				if (r != null) {
					r.moveTo(be.centre.getX() + 0.5, be.centre.getY() + 0.25, be.centre.getZ() + 0.5, 0, 0);
					server.addFreshEntity(r);
					be.rocketId = r.getUUID();
					be.setChanged();
				}
			} else if (!want && rocket != null && !rocket.isLaunching()) {
				rocket.discard();
				be.rocketId = null;
				be.setChanged();
			}
			return;
		}
		// Countdown.
		if (be.countdown % 20 == 0 && be.countdown > 0) {
			Component msg = Component.literal("Launch in " + be.countdown / 20 + "...").withStyle(ChatFormatting.GOLD);
			for (var p : server.players()) if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 96 * 96) p.displayClientMessage(msg, true);
		}
		if (--be.countdown > 0) return;
		be.countdown = -1;
		if (rocket == null || be.items.getItem(0).isEmpty()) return;
		String payload = be.items.getItem(1).is(ModBlocks.MINING_SATELLITE.get()) ? "mining" : be.items.getItem(1).is(ModBlocks.SOLAR_SATELLITE.get()) ? "solar" : "";
		be.items.setItem(0, ItemStack.EMPTY);
		be.items.setItem(1, ItemStack.EMPTY);
		be.fuel = 0;
		rocket.launch(be.launcher != null ? be.launcher : UUID.randomUUID(), be.launcherName, payload);
		be.rocketId = null;
		be.setChanged();
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return tankCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); tankCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); tankCap = LazyOptional.of(() -> tank); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		var list = net.minecraft.core.NonNullList.withSize(2, ItemStack.EMPTY);
		for (int i = 0; i < 2; i++) list.set(i, items.getItem(i));
		ContainerHelper.saveAllItems(tag, list);
		tag.putInt("fuel", fuel);
		if (rocketId != null) tag.putUUID("rocket", rocketId);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		var list = net.minecraft.core.NonNullList.withSize(2, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, list);
		for (int i = 0; i < 2; i++) items.setItem(i, list.get(i));
		fuel = tag.getInt("fuel");
		rocketId = tag.hasUUID("rocket") ? tag.getUUID("rocket") : null;
	}

	/** When the controller is broken: take the standing rocket off the pad. */
	public void removed() {
		if (level instanceof ServerLevel s) {
			RocketEntity r = rocket(s);
			if (r != null && !r.isLaunching()) r.discard();
		}
	}

	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.launch_controller"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new LaunchControllerMenu(id, inv, this, items, data);
	}
}
