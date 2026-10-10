package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.menu.SteamTurbineMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Sits on top of a Boiler. Spins up while it's getting steam and spins down when it isn't.
 * Output is proportional to rotor speed: at full speed it uses 100 mB steam/t and makes 200 FE/t.
 */
public class SteamTurbineBlockEntity extends BlockEntity implements MenuProvider {
	public static final int STEAM_CAPACITY = 1_000;
	public static final int MAX_STEAM_PER_TICK = 100;
	public static final int FE_PER_STEAM = 2;
	public static final int ENERGY_CAPACITY = 100_000;
	public static final int MAX_PUSH = 8_000;
	public static final int MAX_SPEED = 100;

	public final ModEnergyStorage energy = new ModEnergyStorage(ENERGY_CAPACITY, 0, MAX_PUSH, this::setChanged);
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

	/** Steam input for Gas Pipes (the Boiler underneath feeds it directly too). */
	private final net.minecraftforge.fluids.capability.IFluidHandler steamInput = new net.minecraftforge.fluids.capability.IFluidHandler() {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
			return steam > 0 ? new net.minecraftforge.fluids.FluidStack(com.robvanblerk.tieredpower.registry.ModFluids.STEAM.get(), steam)
					: net.minecraftforge.fluids.FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return STEAM_CAPACITY;
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
			return stack.getFluid() == com.robvanblerk.tieredpower.registry.ModFluids.STEAM.get();
		}

		@Override
		public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
			if (!isFluidValid(0, resource)) return 0;
			int accepted = Math.min(resource.getAmount(), STEAM_CAPACITY - steam);
			if (action.execute() && accepted > 0) {
				steam += accepted;
				setChanged();
			}
			return accepted;
		}

		@Override
		public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
			return net.minecraftforge.fluids.FluidStack.EMPTY;
		}

		@Override
		public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
			return net.minecraftforge.fluids.FluidStack.EMPTY;
		}
	};
	private LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> steamCap = LazyOptional.of(() -> steamInput);

	private int steam;
	private int speed;          // 0-100 %
	private int lastGenerated;  // FE/t, for the GUI

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> speed;
				case 3 -> steam;
				case 4 -> lastGenerated;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return SteamTurbineMenu.DATA_COUNT;
		}
	};

	public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STEAM_TURBINE.get(), pos, state);
	}

	/** Called by the Boiler underneath. Returns how much steam was accepted. */
	public int acceptSteam(int amount) {
		int accepted = Math.min(amount, STEAM_CAPACITY - steam);
		steam += accepted;
		return accepted;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SteamTurbineBlockEntity be) {
		// Spin up while there's enough steam and room to store power; otherwise spin down.
		boolean supplied = be.steam > 0 && be.energy.getSpace() > 0;
		be.speed = supplied ? Math.min(MAX_SPEED, be.speed + 1) : Math.max(0, be.speed - 2);

		int steamUse = Math.min(be.steam, MAX_STEAM_PER_TICK * be.speed / MAX_SPEED);
		be.steam -= steamUse;
		be.lastGenerated = steamUse * com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.TURBINE_FE_PER_STEAM);
		if (be.lastGenerated > 0) be.energy.addInternal(com.robvanblerk.tieredpower.Config.gen(be.lastGenerated));

		// Push power out of every side except down (that's the Boiler).
		for (Direction dir : Direction.values()) {
			if (dir == Direction.DOWN || be.energy.getEnergyStored() <= 0) continue;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), MAX_PUSH);
		}

		boolean running = be.speed > 0;
		if (state.getValue(MachineBlock.LIT) != running) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, running), Block.UPDATE_ALL);
		}
		be.setChanged();
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY && side != Direction.DOWN) return energyCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER) return steamCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energyCap.invalidate();
		steamCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		energyCap = LazyOptional.of(() -> energy);
		steamCap = LazyOptional.of(() -> steamInput);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
		tag.putInt("steam", steam);
		tag.putInt("speed", speed);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		steam = tag.getInt("steam");
		speed = tag.getInt("speed");
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.steam_turbine");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new SteamTurbineMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
	}
}
