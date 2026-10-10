package com.robvanblerk.tieredpower.block.entity;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.block.FuelAssemblyBlock;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.FissionControllerMenu;
import com.robvanblerk.tieredpower.multiblock.FissionStructure;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Multiblock Fission Reactor controller.
 *  - Fuel: Uranium Fuel Rods go into an internal store (via the Fuel Port or its GUI). One rod gives 6,000 "assembly-ticks":
 *    a reactor with 10 Fuel Assemblies burns a rod every 30 seconds. Each rod comes back as a Depleted Fuel Rod.
 *  - Power: 1,000 FE/t per assembly at full heat (from 50%), x (1 + 10% per touching neighbour, averaged).
 *  - The controller floods any air inside with water from its tank; each water block adds 2 cooling.
 *  - Heat: +1 per assembly per tick. Above 50% it cools with water: 5 base + 10 per Coolant Channel + 2 per water block heat/tick,
 *    5 mB water per heat point, which becomes 50 mB of steam (out through Coolant Ports).
 *  - At 100% heat it SCRAMs (or explodes if meltdowns are on in the config).
 * Slots: 0 fuel rods in, 1 depleted rods out.
 */
public class FissionControllerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 20_000_000, PORT_RATE = 64_000;
	public static final int OUTPUT_PER_ASSEMBLY = 1_000, ROD_TICKS = 6_000, MAX_RODS = 64;
	public static final int MAX_HEAT = 1_000, TARGET_HEAT = 500, BASE_COOLING = 5, COOLING_PER_CHANNEL = 10, COOLING_PER_WATER_BLOCK = 2;
	public static final int WATER_PER_HEAT = 5, STEAM_PER_HEAT = 50;
	public static final int WATER_CAPACITY = 64_000, STEAM_CAPACITY = 64_000;
	public static final int CHECK_INTERVAL = 20;

	private boolean formed;
	private int assemblies, channels, pairs, waterBlocks;
	private List<BlockPos> floodQueue = List.of();
	private List<BlockPos> assemblyPositions = List.of();
	private boolean assembliesLit;
	private int checkTimer = CHECK_INTERVAL;

	private int rods, depleted, fuelBuffer;
	/** MOX rods stored, and whether the fuel now burning came from one (x1.5 power and heat). */
	private int moxRods;
	private boolean burningMox;
	private int heat, generating, steam;
	private boolean scram;

	final FluidTank water = new FluidTank(WATER_CAPACITY, s -> s.getFluid().is(FluidTags.WATER)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> heat;
				case 3 -> generating & 0xFFFF;
				case 4 -> (generating >>> 16) & 0xFFFF;
				case 5 -> formed ? 1 : 0;
				case 6 -> assemblies;
				case 7 -> channels;
				case 8 -> efficiencyPercent();
				case 9 -> rods;
				case 10 -> water.getFluidAmount() / 10;
				case 11 -> scram ? 1 : 0;
				case 12 -> waterBlocks;
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return FissionControllerMenu.DATA_COUNT;
		}
	};

	public FissionControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FISSION_CONTROLLER.get(), pos, state, 2, CAPACITY, 0, PORT_RATE);
	}

	public boolean isFormed() { return formed; }
	public int getHeat() { return heat; }
	public int getGenerating() { return generating; }
	public boolean isScrammed() { return scram; }
	public int getAssemblies() { return assemblies; }

	public int efficiencyPercent() {
		if (assemblies == 0) return 100;
		double avgNeighbours = 2.0 * pairs / assemblies;
		return (int) Math.round(100 * (1 + 0.10 * avgNeighbours));
	}

	public int coolingCapacity() {
		return BASE_COOLING + COOLING_PER_CHANNEL * channels + COOLING_PER_WATER_BLOCK * waterBlocks;
	}

	public int getWaterBlocks() {
		return waterBlocks;
	}

	// ---- used by the ports ----
	public int insertWater(FluidStack stack, IFluidHandler.FluidAction action) {
		return formed ? water.fill(stack, action) : 0;
	}

	public FluidStack takeSteam(int max, IFluidHandler.FluidAction action) {
		int amount = Math.min(max, steam);
		if (!formed || amount <= 0) return FluidStack.EMPTY;
		if (action.execute()) {
			steam -= amount;
			setChanged();
		}
		return new FluidStack(ModFluids.STEAM.get(), amount);
	}

	public int getSteam() { return steam; }

	/** Loads up to 'count' rods straight into the fuel store; returns how many fitted. */
	public int loadRods(int count) {
		return loadRods(count, false);
	}

	/** Loads fuel rods (uranium, or MOX) into the store; returns how many fitted. */
	public int loadRods(int count, boolean mox) {
		int fit = Math.max(0, Math.min(count, MAX_RODS - rods - moxRods));
		if (mox) moxRods += fit; else rods += fit;
		if (fit > 0) setChanged();
		return fit;
	}

	/** Uranium and MOX rods waiting to burn. */
	public int getStoredRods() { return rods + moxRods; }

	/** Depleted rods in the output slot plus any still inside. */
	public int getWaitingDepleted() {
		return depleted + items.get(1).getCount();
	}

	/** Takes depleted rods for a Waste Port (from the output slot). */
	public ItemStack takeDepleted(int max, boolean simulate) {
		ItemStack out = items.get(1);
		if (out.isEmpty() || max <= 0) return ItemStack.EMPTY;
		int n = Math.min(max, out.getCount());
		ItemStack taken = out.copyWithCount(n);
		if (!simulate) {
			out.shrink(n);
			setChanged();
		}
		return taken;
	}
	public FluidTank getWater() { return water; }

	public Component revalidate() {
		checkTimer = 0;
		if (level == null) return Component.empty();
		FissionStructure.Result r = FissionStructure.check(level, worldPosition, getBlockState().getValue(MachineBlock.FACING));
		boolean was = formed;
		formed = r.formed();
		if (formed) {
			assemblies = r.assemblies();
			channels = r.channels();
			pairs = r.neighbourPairs();
			waterBlocks = r.waterBlocks();
			floodQueue = r.airPositions();
			if (!assemblyPositions.equals(r.assemblyPositions())) {
				setLit(false);
				assemblyPositions = r.assemblyPositions();
			}
			for (BlockPos p : r.ports()) {
				if (level.getBlockEntity(p) instanceof FissionPortBlockEntity port) port.setController(worldPosition);
				if (level.getBlockEntity(p) instanceof ReactorGaugeBlockEntity gauge) gauge.setController(worldPosition);
			}
		} else if (was) {
			setLit(false);
		}
		setChanged();
		return r.message();
	}

	private void setLit(boolean lit) {
		if (level == null) return;
		for (BlockPos p : assemblyPositions) {
			if (!level.isLoaded(p)) continue;
			BlockState s = level.getBlockState(p);
			if (s.getBlock() instanceof FuelAssemblyBlock && s.getValue(FuelAssemblyBlock.ACTIVE) != lit) {
				level.setBlock(p, s.setValue(FuelAssemblyBlock.ACTIVE, lit), Block.UPDATE_CLIENTS);
			}
		}
		assembliesLit = lit;
	}

	// ---- Neutron Reflector ----
	private boolean reflector;

	public boolean hasReflector() {
		return reflector;
	}

	public void setReflector(boolean fitted) {
		reflector = fitted;
		setChanged();
	}

	@Override
	public java.util.List<ItemStack> installedDrops() {
		return reflector ? java.util.List.of(new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.NEUTRON_REFLECTOR.get())) : java.util.List.of();
	}

	// ---- nitrogen cooling (Cryo Injector) ----
	private int cryoTicks;

	/** Is the reactor making power right now? (Tritium Breeders and Cryo Injectors need it running.) */
	public boolean isRunning() {
		return generating > 0;
	}

	/** Called each tick by a supplied Cryo Injector: nitrogen cooling lets it run harder, +50% power. */
	public boolean applyCryoBoost() {
		if (generating <= 0 && cryoTicks <= 0) return false;
		cryoTicks = 2;
		return true;
	}

	public boolean isCryoBoosted() {
		return cryoTicks > 0;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FissionControllerBlockEntity be) {
		if (++be.checkTimer >= CHECK_INTERVAL) be.revalidate();
		boolean paused = be.preTick(level, pos, state);

		// Move rods from the GUI slot into the store, and depleted rods out to the output slot.
		ItemStack in = be.items.get(0);
		while (com.robvanblerk.tieredpower.energy.FuelRods.isFuel(in) && !in.isEmpty() && be.rods + be.moxRods < MAX_RODS) {
			if (com.robvanblerk.tieredpower.energy.FuelRods.isMox(in)) be.moxRods++; else be.rods++;
			in.shrink(1);
		}
		ItemStack out = be.items.get(1);
		if (be.depleted > 0 && (out.isEmpty() || (out.is(ModBlocks.DEPLETED_FUEL_ROD.get()) && out.getCount() < out.getMaxStackSize()))) {
			int move = out.isEmpty() ? Math.min(be.depleted, 16) : Math.min(be.depleted, out.getMaxStackSize() - out.getCount());
			if (out.isEmpty()) be.items.set(1, new ItemStack(ModBlocks.DEPLETED_FUEL_ROD.get(), move));
			else out.grow(move);
			be.depleted -= move;
		}

		// Flood the inside with water (one bucket at a time, bottom up) - the water around the rods is coolant.
		if (be.formed && level.getGameTime() % 5 == 0 && be.water.getFluidAmount() >= 1_000) {
			for (BlockPos p : be.floodQueue) {
				if (!level.isLoaded(p) || !level.getBlockState(p).isAir()) continue;
				level.setBlock(p, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
				be.water.drain(1_000, IFluidHandler.FluidAction.EXECUTE);
				be.waterBlocks++;
				break;
			}
		}

		// Burn fuel.
		boolean burning = false;
		if (be.formed && !paused && !be.scram && be.assemblies > 0 && be.energy.getSpace() > 0) { // idles while its buffer is full
			if (be.fuelBuffer < be.assemblies && (be.rods > 0 || be.moxRods > 0)) {
				if (be.moxRods > 0) { // MOX first: twice the burn time, x1.5 power and heat
					be.moxRods--;
					be.fuelBuffer += ROD_TICKS * 2;
					be.burningMox = true;
				} else {
					be.rods--;
					be.fuelBuffer += ROD_TICKS;
					be.burningMox = false;
				}
				be.depleted++;
			}
			if (be.fuelBuffer >= be.assemblies) {
				if (!be.reflector || level.getGameTime() % 3 != 0) be.fuelBuffer -= be.assemblies; // Neutron Reflector: rods last 50% longer
				burning = true;
			}
		}

		// Heat and cooling.
		if (burning) be.heat += be.assemblies + (be.burningMox && level.getGameTime() % 2 == 0 ? be.assemblies : 0);
		if (be.heat > TARGET_HEAT) {
			int cool = Math.min(Math.min(be.heat - TARGET_HEAT, be.coolingCapacity()), be.water.getFluidAmount() / WATER_PER_HEAT);
			if (cool > 0) {
				be.heat -= cool;
				be.water.drain(cool * WATER_PER_HEAT, IFluidHandler.FluidAction.EXECUTE);
				be.steam = Math.min(STEAM_CAPACITY, be.steam + cool * STEAM_PER_HEAT);
			}
		}
		if (!burning && be.heat > 0) be.heat = Math.max(0, be.heat - 2);

		if (be.heat >= MAX_HEAT) {
			if (Config.get(Config.FISSION_MELTDOWN)) {
				float power = 6.0f + (float) Math.sqrt(be.assemblies) * 2;
				level.removeBlock(pos, false);
				level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);
				return;
			}
			be.scram = true;
		}
		if (be.scram && be.heat <= 100) be.scram = false;

		be.generating = burning ? (int) ((long) be.assemblies * OUTPUT_PER_ASSEMBLY * be.efficiencyPercent() / 100
				* Math.min(1.0, be.heat / (double) TARGET_HEAT)) : 0;
		if (be.burningMox) be.generating = be.generating * 3 / 2; // MOX fuel: +50%
		if (be.cryoTicks > 0) {
			be.cryoTicks--;
			be.generating = be.generating * 3 / 2; // nitrogen-cooled: +50%
		}
		if (be.generating > 0) be.energy.addInternal(com.robvanblerk.tieredpower.Config.gen(be.generating));

		if (be.formed && burning != be.assembliesLit) be.setLit(burning);
		be.setChanged();
		BlockState now = level.getBlockState(pos);
		if (now.hasProperty(MachineBlock.LIT) && now.getValue(MachineBlock.LIT) != burning) {
			level.setBlock(pos, now.setValue(MachineBlock.LIT, burning), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("moxRods", moxRods);
		tag.putBoolean("burningMox", burningMox);
		tag.putBoolean("reflector", reflector);
		tag.putInt("rods", rods);
		tag.putInt("depleted", depleted);
		tag.putInt("fuelBuffer", fuelBuffer);
		tag.putInt("heat", heat);
		tag.putInt("steam", steam);
		tag.putBoolean("scram", scram);
		tag.put("water", water.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		moxRods = tag.getInt("moxRods");
		burningMox = tag.getBoolean("burningMox");
		reflector = tag.getBoolean("reflector");
		rods = tag.getInt("rods");
		depleted = tag.getInt("depleted");
		fuelBuffer = tag.getInt("fuelBuffer");
		heat = tag.getInt("heat");
		steam = tag.getInt("steam");
		scram = tag.getBoolean("scram");
		water.readFromNBT(tag.getCompound("water"));
	}

	// Fuel Ports (and hoppers on the controller): rods in, depleted rods out.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{0, 1};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == 1;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && com.robvanblerk.tieredpower.energy.FuelRods.isFuel(stack);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.fission_controller");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FissionControllerMenu(containerId, inventory, this, data);
	}
}
