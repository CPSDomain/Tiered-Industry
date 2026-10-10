package com.robvanblerk.tieredpower.block.entity;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.block.StorageControllerBlock;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.ItemKey;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * The heart of an item storage network. Needs power (20 FE/t plus 5 per Drive Bay). Also acts as one big inventory, so
 * Item Pipes and hoppers can put items in and pull them out.
 */
public class StorageControllerBlockEntity extends BlockEntity {
	public static final int CAPACITY = 100_000, MAX_INPUT = 10_000, BASE_COST = 20, COST_PER_BAY = 5, COST_PER_ACCESS_POINT = 10, COST_PER_ASSEMBLER = 10, COST_PER_CPU = 10, CRAFT_INTERVAL = 8;

	public enum Problem { NONE, NO_POWER, TWO_CONTROLLERS }

	private final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, MAX_INPUT, 0, this::setChanged);
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
	private StorageNetwork network;
	private Problem problem = Problem.NO_POWER;
	private int ticks = -1;
	private boolean twoControllers;

	// Cached view for the item handler, refreshed when anything changes.
	private List<Map.Entry<ItemKey, Long>> view = List.of();
	private long viewVersion = -1;

	private final IItemHandler items = new IItemHandler() {
		private List<Map.Entry<ItemKey, Long>> view() {
			if (network == null || !isOnline()) return List.of();
			if (viewVersion != StorageNetwork.version()) {
				view = network.listing();
				viewVersion = StorageNetwork.version();
			}
			return view;
		}

		@Override public int getSlots() { return view().size() + 1; }

		@Override
		public @NotNull ItemStack getStackInSlot(int slot) {
			List<Map.Entry<ItemKey, Long>> v = view();
			if (slot < 0 || slot >= v.size()) return ItemStack.EMPTY;
			ItemKey k = v.get(slot).getKey();
			return k.toStack((int) Math.min(v.get(slot).getValue(), k.proto().getMaxStackSize()));
		}

		@Override
		public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
			if (!isOnline()) return stack;
			return network.insert(stack, simulate);
		}

		@Override
		public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
			List<Map.Entry<ItemKey, Long>> v = view();
			if (!isOnline() || slot < 0 || slot >= v.size()) return ItemStack.EMPTY;
			ItemKey k = v.get(slot).getKey();
			return network.extract(k, Math.min(amount, k.proto().getMaxStackSize()), simulate);
		}

		@Override public int getSlotLimit(int slot) { return 64; }
		@Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
	};
	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);

	private List<Map.Entry<com.robvanblerk.tieredpower.storage.FluidKey, Long>> fluidView = List.of();
	private long fluidViewVersion = -1;

	/** Fluids and gases: pipes, machines and reactors can fill it and drain it. */
	private final net.minecraftforge.fluids.capability.IFluidHandler fluids = new net.minecraftforge.fluids.capability.IFluidHandler() {
		private List<Map.Entry<com.robvanblerk.tieredpower.storage.FluidKey, Long>> view() {
			if (network == null || !isOnline()) return List.of();
			if (fluidViewVersion != StorageNetwork.version()) {
				fluidView = network.fluidListing();
				fluidViewVersion = StorageNetwork.version();
			}
			return fluidView;
		}

		@Override public int getTanks() { return view().size() + 1; }

		@Override
		public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
			var v = view();
			if (tank < 0 || tank >= v.size()) return net.minecraftforge.fluids.FluidStack.EMPTY;
			return v.get(tank).getKey().toStack((int) Math.min(Integer.MAX_VALUE, v.get(tank).getValue()));
		}

		@Override public int getTankCapacity(int tank) { return Integer.MAX_VALUE; }
		@Override public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) { return true; }

		@Override
		public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
			return isOnline() ? network.insertFluid(resource, action.simulate()) : 0;
		}

		@Override
		public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
			if (!isOnline() || resource.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
			return network.extractFluid(new com.robvanblerk.tieredpower.storage.FluidKey(resource), resource.getAmount(), action.simulate());
		}

		@Override
		public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
			var v = view();
			if (v.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
			return network.extractFluid(v.get(0).getKey(), maxDrain, action.simulate());
		}
	};
	private LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	public IItemHandler itemHandler() {
		return items;
	}

	public net.minecraftforge.fluids.capability.IFluidHandler fluidHandler() {
		return fluids;
	}

	public StorageControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STORAGE_CONTROLLER.get(), pos, state);
	}

	// ---- autocrafting ----

	private final List<com.robvanblerk.tieredpower.storage.CraftingJob> jobs = new java.util.ArrayList<>();
	/** A crafting grid used to run patterns (never shown to anyone). */
	private static final net.minecraft.world.inventory.CraftingContainer GRID = new net.minecraft.world.inventory.TransientCraftingContainer(
			new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
				@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int i) { return ItemStack.EMPTY; }
				@Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return false; }
			}, 3, 3);

	public List<com.robvanblerk.tieredpower.storage.CraftingJob> getJobs() {
		return jobs;
	}

	/** Works out how to make 'amount' of an item right now (nothing is reserved or started). */
	public com.robvanblerk.tieredpower.storage.CraftingPlanner.Plan plan(ItemKey item, long amount) {
		if (!isOnline()) return com.robvanblerk.tieredpower.storage.CraftingPlanner.Plan.impossible(item, amount);
		Level lvl = level;
		java.util.function.Function<ItemKey, com.robvanblerk.tieredpower.storage.CraftingPattern> smelting = null;
		if (lvl != null && (hasSmelter(lvl, assemblerEntities(lvl)) || network.connectors().stream().anyMatch(c -> nextToFurnace(lvl, c)))) {
			Map<ItemKey, Long> stock = network.stock();
			smelting = key -> smeltingFor(lvl, key, stock);
		}
		return com.robvanblerk.tieredpower.storage.CraftingPlanner.plan(network.stock(), network.fluidStock(), network.patterns(), smelting, item, amount);
	}

	/** Is any Molecular Assembler touching an Electric Furnace? Then the network can smelt without a pattern. */
	private static boolean hasSmelter(Level level, List<MolecularAssemblerBlockEntity> assemblers) {
		for (MolecularAssemblerBlockEntity a : assemblers) if (nextToFurnace(level, a.getBlockPos())) return true;
		return false;
	}

	private static boolean nextToFurnace(Level level, BlockPos pos) {
		for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values())
			if (level.getBlockEntity(pos.relative(d)) instanceof ElectricFurnaceBlockEntity) return true;
		return false;
	}

	/**
	 * A built-in smelting step that makes this item, from the furnace recipes: e.g. Iron Ingot from raw iron, iron ore or
	 * iron dust - preferring the recipe whose inputs are most plentiful in storage. Null if nothing smelts into it.
	 */
	private static @Nullable com.robvanblerk.tieredpower.storage.CraftingPattern smeltingFor(Level level, ItemKey want, Map<ItemKey, Long> stock) {
		com.robvanblerk.tieredpower.storage.CraftingPattern best = null;
		long bestStock = -1;
		for (var r : level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
			ItemStack out = r.getResultItem(level.registryAccess());
			if (out.isEmpty() || !want.matches(out.copyWithCount(1)) || r.getIngredients().isEmpty()) continue;
			List<ItemStack> options = new java.util.ArrayList<>();
			for (ItemStack o : r.getIngredients().get(0).getItems()) if (!o.isEmpty() && !want.matches(o.copyWithCount(1))) options.add(o.copyWithCount(1));
			if (options.isEmpty()) continue;
			long have = 0;
			for (ItemStack o : options) have += stock.getOrDefault(new ItemKey(o), 0L);
			if (have > bestStock) {
				bestStock = have;
				best = com.robvanblerk.tieredpower.storage.CraftingPattern.builtInSmelting(options, out);
			}
		}
		return best;
	}

	/** Plans and, if everything is available, queues the job. Returns a message for the player. */
	public String startJob(ItemKey item, long amount, @Nullable java.util.UUID requester) {
		if (!isOnline()) return "The storage network is offline";
		if (network.cpus().isEmpty()) return "Add a Crafting CPU to the network to run crafting jobs";
		if (network.assemblers().isEmpty() && network.matrices().isEmpty())
			return "Add an Assembly Matrix (or a Molecular Assembler) with patterns to the network";
		var plan = plan(item, amount);
		if (!plan.possible()) return "Can't craft that: some ingredients are missing";
		if (plan.steps().isEmpty()) return "Nothing to craft";
		jobs.add(new com.robvanblerk.tieredpower.storage.CraftingJob(item.toStack(1), amount, requester, new java.util.ArrayList<>(plan.steps())));
		setChanged();
		return "Crafting " + amount + " " + item.proto().getHoverName().getString()
				+ (jobs.size() > network.jobSlots() ? " (queued - all Crafting CPUs are busy)" : "");
	}

	/** How many crafting jobs this network runs at the same time (from its CPUs' tiers). */
	public int jobSlots() {
		return network == null ? 0 : network.jobSlots();
	}

	/** Is there already a job making this item? (The Stock Keeper won't start a second one.) */
	public boolean hasJobFor(ItemKey item) {
		for (var job : jobs) if (item.matches(job.target)) return true;
		return false;
	}

	/** Cancels one job (by its place in the list). Items already made stay in storage. */
	public boolean cancelJob(int index) {
		if (index < 0 || index >= jobs.size()) return false;
		jobs.remove(index);
		setChanged();
		return true;
	}

	public int cancelJobs() {
		int n = jobs.size();
		jobs.clear();
		setChanged();
		return n;
	}

	private List<MolecularAssemblerBlockEntity> assemblerEntities(Level level) {
		List<MolecularAssemblerBlockEntity> out = new java.util.ArrayList<>();
		for (BlockPos p : network.assemblers()) if (level.getBlockEntity(p) instanceof MolecularAssemblerBlockEntity a) out.add(a);
		return out;
	}

	/**
	 * One round of crafting. Crafting steps: each assembler does one craft. Processing steps: each assembler holding the
	 * pattern sends one operation's inputs to the machine next to it. Finished machine outputs are collected first.
	 */
	private void runJobs(Level level) {
		List<Iface> ifaces = interfaces(level);
		int running = Math.min(jobs.size(), network.jobSlots());
		collectMachineOutputs(level, ifaces, running);
		int craftBudget = 0;
		Map<BlockPos, Integer> machineBudget = new java.util.HashMap<>();
		for (Iface f : ifaces) {
			if (f.assembler() != null) craftBudget += f.budget(); // crafting patterns: assemblers and the Matrix craft them
			machineBudget.put(f.pos(), f.budget());
		}
		for (BlockPos m : network.matrices())
			if (level.getBlockEntity(m) instanceof MatrixControllerBlockEntity mc) craftBudget += mc.craftsPerCycle();
		// Overclock Accelerators: the machines the Matrix crafts with run faster (and use more power) while jobs run.
		int overclock = 0;
		for (BlockPos m : network.matrices())
			if (level.getBlockEntity(m) instanceof MatrixControllerBlockEntity mc) overclock += mc.overclockLevels();
		overclock = Math.min(MatrixControllerBlockEntity.MAX_OVERCLOCK, overclock);
		if (overclock > 0) {
			long until = level.getGameTime() + CRAFT_INTERVAL * 3L;
			for (Iface f : ifaces)
				for (Machine machine : machinesNextTo(level, f.pos()))
					if (machine.be() instanceof MachineBlockEntity mbe) mbe.overclock(overclock, until);
		}
		for (int j = 0; j < running; j++) {
			var job = jobs.get(j);
			boolean progressed = false;
			String reason = null;
			for (var step : job.steps) {
				if (step.pattern.processing()) {
					boolean anyInterface = false;
					for (Iface f : ifaces) {
						if (!canServe(level, f, step.pattern)) continue;
						anyInterface = true;
						while (step.crafts > 0 && machineBudget.get(f.pos()) > 0 && sendToMachine(level, f.pos(), step)) {
							step.crafts--;
							step.pending++;
							machineBudget.merge(f.pos(), -1, Integer::sum);
							progressed = true;
						}
					}
					if (reason == null && step.crafts > 0 && !anyInterface) reason = noInterfaceReason(step.pattern);
					if (reason == null && step.crafts > 0 && lastFailure != null) reason = lastFailure;
					if (reason == null && step.pending > 0 && step.machine != null)
						reason = "Waiting on " + step.machine + " to finish " + outputName(step.pattern);
				} else {
					while (step.crafts > 0 && craftBudget > 0 && craftOnce(level, step.pattern)) {
						step.crafts--;
						craftBudget--;
						progressed = true;
					}
					if (reason == null && step.crafts > 0) reason = missingFor(step.pattern, 1);
				}
			}
			if (job.steps.removeIf(com.robvanblerk.tieredpower.storage.CraftingPlanner.Step::done)) progressed = true;
			if (progressed) {
				job.idleCycles = 0;
				job.status = "Working (" + job.craftsLeft() + " crafts/operations left)";
			} else {
				job.idleCycles++;
				if (job.idleCycles >= 3) { // after a couple of seconds, say exactly what it's waiting for
					boolean waitingOnMachine = job.steps.stream().anyMatch(s -> s.pending > 0);
					job.status = reason != null ? reason
							: waitingOnMachine ? "Waiting for a machine to finish (is it powered? is its connector, bus or assembler still on it?)"
							: "Waiting for ingredients (were they taken out of storage?)";
				}
				if (job.idleCycles == 75) tell(level, job, job.target.getHoverName().getString() + ": " + job.status); // ~30 seconds
			}
		}
		for (var it = jobs.iterator(); it.hasNext(); ) {
			var job = it.next();
			if (job.steps.isEmpty()) {
				tell(level, job, "Finished crafting " + job.amount + " " + job.target.getHoverName().getString());
				it.remove();
			}
		}
		for (int j = running; j < jobs.size(); j++) jobs.get(j).status = "Queued - waiting for a free Crafting CPU";
		setChanged();
	}

	/** A machine next to an assembler: its item and fluid access on the side facing the assembler (either may be null). */
	private record Machine(@Nullable net.minecraftforge.items.IItemHandler items, @Nullable net.minecraftforge.fluids.capability.IFluidHandler fluids, BlockPos pos, BlockEntity be) {}

	/** Why the last sendToMachine call failed, in words (for job status). */
	private @Nullable String lastFailure;

	/** One line per remaining step of a job, for the Jobs screen's detail panel. */
	public static java.util.List<String> describeSteps(com.robvanblerk.tieredpower.storage.CraftingJob job) {
		java.util.List<String> out = new java.util.ArrayList<>();
		for (var st : job.steps) {
			StringBuilder b = new StringBuilder(outputName(st.pattern));
			b.append(st.pattern.processing() ? (st.pattern.smelting() ? " (smelt)" : " (machine)") : " (craft)");
			b.append(": ").append(st.crafts).append(" to do");
			if (st.pending > 0) b.append(", ").append(st.pending).append(" running").append(st.machine != null ? " on " + st.machine : "");
			out.add(b.toString());
		}
		return out;
	}

	private static String outputName(com.robvanblerk.tieredpower.storage.CraftingPattern p) {
		if (p.hasItemOutput()) return p.output().getHoverName().getString();
		return p.fluidOutputs().isEmpty() ? "?" : p.fluidOutputs().get(0).getDisplayName().getString();
	}

	private static String describe(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock().getName().getString() + " at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	/** "Needs 2 Iron Ingot in storage" for the first ingredient one craft of this pattern is short of, or null. */
	private @Nullable String missingFor(com.robvanblerk.tieredpower.storage.CraftingPattern p, int ops) {
		for (var r : p.requirements()) {
			long have = 0;
			for (ItemKey o : r.options()) {
				have += network.count(o);
				for (ItemKey v : network.variantsOf(o)) have += network.count(v);
			}
			if (have < (long) r.count() * ops)
				return "Needs " + ((long) r.count() * ops - have) + " " + r.options().get(0).proto().getHoverName().getString() + " in storage";
		}
		return null;
	}

	/** Blocks next to an assembler that it can use as machines (not storage parts or pipes). */
	private static List<Machine> machinesNextTo(Level level, BlockPos pos) {
		List<Machine> out = new java.util.ArrayList<>();
		for (net.minecraft.core.Direction d : MachineConnectorBlockEntityHelper.sides(level, pos)) {
			BlockPos n = pos.relative(d);
			if (level.getBlockState(n).getBlock() instanceof com.robvanblerk.tieredpower.storage.StorageNetworkBlock) continue;
			BlockEntity be = level.getBlockEntity(n);
			if (be == null || be instanceof ItemPipeBlockEntity || be instanceof FluidPipeBlockEntity) continue;
			// This mod's machines: any face works (Sides settings don't apply to assemblers). Others: their own sided rules.
			var items = be instanceof MachineBlockEntity m ? m.automationHandler() : be.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).orElse(null);
			var fluids = be.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).orElse(null);
			if (items != null || fluids != null) out.add(new Machine(items, fluids, n, be));
		}
		return out;
	}

	/**
	 * Sends one operation's inputs from storage into a machine next to the assembler. For each ingredient it uses
	 * whichever accepted item is in stock (raw ore, ore block or dust for a smelting step). Water is only sent if
	 * storage has it - machines draw their own from a Sink or pipe otherwise. Built-in smelting only uses Electric
	 * Furnaces. On failure, lastFailure says why.
	 */
	private boolean sendToMachine(Level level, BlockPos iface, com.robvanblerk.tieredpower.storage.CraftingPlanner.Step step) {
		var pattern = step.pattern;
		lastFailure = null;
		// Items: pick an in-stock option for each ingredient.
		Map<ItemKey, Integer> counts = new java.util.LinkedHashMap<>();
		for (var r : pattern.requirements()) {
			ItemKey pick = null;
			for (ItemKey o : r.options()) if (network.count(o) - counts.getOrDefault(o, 0) >= r.count()) { pick = o; break; }
			if (pick == null) {
				lastFailure = "Needs " + r.count() + " " + r.options().get(0).proto().getHoverName().getString() + " in storage";
				return false;
			}
			counts.merge(pick, r.count(), Integer::sum);
		}
		// Fluids: water comes from the machine's own supply when storage has none.
		Map<com.robvanblerk.tieredpower.storage.FluidKey, Integer> fluidCounts = new java.util.LinkedHashMap<>();
		for (var e : pattern.fluidCounts().entrySet()) {
			boolean water = com.robvanblerk.tieredpower.storage.CraftingPattern.isWater(e.getKey().toStack(1));
			if (network.fluidCount(e.getKey()) >= e.getValue()) fluidCounts.put(e.getKey(), e.getValue());
			else if (!water) {
				lastFailure = "Needs " + e.getValue() + " mB of " + e.getKey().toStack(1).getDisplayName().getString() + " in storage";
				return false;
			}
		}
		List<Machine> machines = machinesFor(level, iface, pattern);
		if (machines.isEmpty()) {
			lastFailure = "No suitable machine is touching the " + describe(level, iface);
			return false;
		}
		for (Machine machine : machines) {
			if (!counts.isEmpty() && machine.items() == null) continue;
			if (!fluidCounts.isEmpty() && machine.fluids() == null) continue;
			boolean fits = true;
			for (Map.Entry<ItemKey, Integer> e : counts.entrySet())
				if (!net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(machine.items(), e.getKey().toStack(e.getValue()), true).isEmpty()) fits = false;
			for (var e : fluidCounts.entrySet())
				if (machine.fluids().fill(e.getKey().toStack(e.getValue()), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) < e.getValue()) fits = false;
			if (!fits) continue;
			for (Map.Entry<ItemKey, Integer> e : counts.entrySet()) {
				ItemStack got = network.extract(e.getKey(), e.getValue(), false);
				ItemStack left = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(machine.items(), got, false);
				if (!left.isEmpty()) dropIfFull(level, network.insert(left, false)); // shouldn't happen: we checked first
			}
			for (var e : fluidCounts.entrySet()) {
				var got = network.extractFluid(e.getKey(), e.getValue(), false);
				int filled = machine.fluids().fill(got, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
				if (filled < got.getAmount()) network.insertFluid(new net.minecraftforge.fluids.FluidStack(got, got.getAmount() - filled), false);
			}
			step.machine = describe(level, machine.pos());
			return true;
		}
		Machine first = machines.get(0);
		lastFailure = describe(level, first.pos()) + " won't take the inputs right now (its input slot is full or holds something else, or it's still busy)";
		return false;
	}

	/** Credits main output (items or mB) to a step waiting on it. */
	/**
	 * An Import Bus pulled items or fluid out of a machine into storage: counts them towards any running processing
	 * step that is waiting on that output from that kind of machine (otherwise the job would wait forever, since the
	 * bus got there first).
	 */
	public void creditImported(BlockPos machinePos, @Nullable ItemStack item, @Nullable net.minecraftforge.fluids.FluidStack fluid) {
		if (level == null) return;
		var block = level.getBlockState(machinePos).getBlock();
		boolean furnace = level.getBlockEntity(machinePos) instanceof ElectricFurnaceBlockEntity;
		int left = item != null ? item.getCount() : fluid != null ? fluid.getAmount() : 0;
		int running = Math.min(jobs.size(), network == null ? 0 : network.jobSlots());
		for (int j = 0; j < running && j < jobs.size() && left > 0; j++) {
			for (var step : jobs.get(j).steps) {
				if (left <= 0) break;
				var p = step.pattern;
				if (!p.processing() || step.pending <= 0) continue;
				if (p.smelting() ? !furnace : !(p.hasMachine() && p.isForMachine(block))) continue;
				boolean match;
				if (item != null) match = p.hasItemOutput() && !p.outputs().isEmpty() && ItemStack.isSameItemSameTags(p.outputs().get(0), item);
				else match = !p.hasItemOutput() && !p.fluidOutputs().isEmpty() && p.fluidOutputs().get(0).isFluidEqual(fluid);
				if (!match) continue;
				int owed = Math.max(0, step.pending * p.mainAmount() - step.received);
				int use = Math.min(owed, left);
				if (use <= 0) continue;
				credit(step, use);
				left -= use;
				setChanged();
			}
		}
	}

	private static void credit(com.robvanblerk.tieredpower.storage.CraftingPlanner.Step step, int amount) {
		step.received += amount;
		int per = step.pattern.mainAmount();
		while (step.received >= per && step.pending > 0) {
			step.received -= per;
			step.pending--;
		}
	}

	/** Pulls finished outputs (items, fluids, gases) of running processing steps out of the machines next to their assemblers. */
	/** Pulls finished results of running processing steps out of the machines their interfaces touch. */
	private void collectMachineOutputs(Level level, List<Iface> ifaces, int running) {
		for (int j = 0; j < running && j < jobs.size(); j++) {
			for (var step : jobs.get(j).steps) {
				if (!step.pattern.processing() || step.pending <= 0) continue;
				var pattern = step.pattern;
				for (Iface f : ifaces) {
					if (step.pending <= 0) break;
					if (!canServe(level, f, pattern)) continue;
					for (Machine machine : machinesFor(level, f.pos(), pattern)) {
						if (machine.items() != null) {
							var inv = machine.items();
							for (int slot = 0; slot < inv.getSlots(); slot++) {
								ItemStack peek = inv.extractItem(slot, 64, true);
								if (peek.isEmpty()) continue;
								int which = -1;
								for (int o = 0; o < pattern.outputs().size(); o++) if (ItemStack.isSameItemSameTags(pattern.outputs().get(o), peek)) which = o;
								if (which < 0) continue;
								int room = peek.getCount() - network.insert(peek, true).getCount();
								if (which == 0) room = Math.min(room, Math.max(0, step.pending * pattern.mainAmount() - step.received)); // only what this step is owed
								if (room <= 0) continue;
								ItemStack taken = inv.extractItem(slot, room, false);
								dropIfFull(level, network.insert(taken, false));
								if (which == 0) credit(step, taken.getCount());
							}
						}
						if (machine.fluids() != null) {
							for (int o = 0; o < pattern.fluidOutputs().size(); o++) {
								var want = pattern.fluidOutputs().get(o);
								var peek = machine.fluids().drain(new net.minecraftforge.fluids.FluidStack(want, 64_000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
								if (peek.isEmpty()) continue;
								int room = network.insertFluid(peek, true);
								boolean main = o == 0 && !pattern.hasItemOutput();
								if (main) room = Math.min(room, Math.max(0, step.pending * pattern.mainAmount() - step.received));
								if (room <= 0) continue;
								var taken = machine.fluids().drain(new net.minecraftforge.fluids.FluidStack(want, room), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
								int stored = network.insertFluid(taken, false);
								if (main) credit(step, stored);
							}
						}
					}
				}
			}
		}
	}

	/** A way into machines: a Molecular Assembler (its own patterns, and Matrix patterns) or a Machine Connector (Matrix patterns). */
	private record Iface(BlockPos pos, @Nullable MolecularAssemblerBlockEntity assembler, int budget) {}

	public static final int CONNECTOR_OPERATIONS = 4;

	private List<Iface> interfaces(Level level) {
		List<Iface> out = new java.util.ArrayList<>();
		for (MolecularAssemblerBlockEntity a : assemblerEntities(level)) out.add(new Iface(a.getBlockPos(), a, a.operationsPerCycle()));
		for (BlockPos c : network.connectors())
			out.add(new Iface(c, null, com.robvanblerk.tieredpower.storage.CraftingTier.CONNECTOR_OPS[com.robvanblerk.tieredpower.storage.CraftingTier.of(level.getBlockState(c))]));
		for (BlockPos b : network.buses()) out.add(new Iface(b, null, CONNECTOR_OPERATIONS));
		return out;
	}

	/** Can this interface run this pattern? */
	private static boolean canServe(Level level, Iface f, com.robvanblerk.tieredpower.storage.CraftingPattern p) {
		if (f.assembler() != null && f.assembler().holds(p)) return true; // an assembler's own pattern
		if (p.smelting()) return nextToFurnace(level, f.pos());
		if (!p.hasMachine()) return false; // a Matrix pattern has to say which machine
		for (net.minecraft.core.Direction d : MachineConnectorBlockEntityHelper.sides(level, f.pos()))
			if (p.isForMachine(level.getBlockState(f.pos().relative(d)).getBlock())) return true;
		return false;
	}

	/** The machines next to this interface that the pattern may use. */
	private static List<Machine> machinesFor(Level level, BlockPos iface, com.robvanblerk.tieredpower.storage.CraftingPattern p) {
		List<Machine> machines = machinesNextTo(level, iface);
		if (p.smelting()) machines.removeIf(m -> !(m.be() instanceof ElectricFurnaceBlockEntity));
		else if (p.hasMachine()) machines.removeIf(m -> !p.isForMachine(level.getBlockState(m.pos()).getBlock()));
		return machines;
	}

	private static String noInterfaceReason(com.robvanblerk.tieredpower.storage.CraftingPattern p) {
		if (p.smelting()) return "Needs a Molecular Assembler, Machine Connector or Import/Export Bus on an Electric Furnace (to smelt " + outputName(p) + ")";
		if (!p.hasMachine()) return "The pattern for " + outputName(p) + " doesn't say which machine - set its Machine in the Pattern Encoder";
		var b = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.tryParse(p.machine()));
		return "Needs a Molecular Assembler, Machine Connector or Import/Export Bus on a " + (b == null ? p.machine() : b.getName().getString());
	}

	/** The running job step (if any) that is waiting on results of this processing pattern. */
	private @Nullable com.robvanblerk.tieredpower.storage.CraftingPlanner.Step pendingStep(com.robvanblerk.tieredpower.storage.CraftingPattern pattern, int running) {
		var key = pattern.save();
		for (int j = 0; j < running && j < jobs.size(); j++)
			for (var step : jobs.get(j).steps) if (step.pending > 0 && step.pattern.save().equals(key)) return step;
		return null;
	}

	/**
	 * One craft in the assembler: picks, for each grid slot, an accepted item that's in storage (substitutes allowed),
	 * crafts, and stores the result and any leftovers (like empty buckets).
	 */
	private boolean craftOnce(Level level, com.robvanblerk.tieredpower.storage.CraftingPattern pattern) {
		if (!network.insert(pattern.output().copy(), true).isEmpty()) return false; // no room for the result
		List<ItemStack> chosen = chooseIngredients(pattern, true);
		ItemStack result = chosen == null ? ItemStack.EMPTY : tryGrid(level, chosen);
		if (result.isEmpty() && pattern.hasSubstitutes()) { // a mix of substitutes didn't work: try the exact recipe
			chosen = chooseIngredients(pattern, false);
			result = chosen == null ? ItemStack.EMPTY : tryGrid(level, chosen);
		}
		if (result.isEmpty() || !ItemStack.isSameItem(result, pattern.output())) return false;
		for (int i = 0; i < 9; i++) GRID.setItem(i, chosen.get(i).copy());
		var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, GRID, level);
		var remaining = recipe.map(r -> r.getRemainingItems(GRID)).orElse(net.minecraft.core.NonNullList.create());
		GRID.clearContent();
		for (ItemStack c : chosen) if (!c.isEmpty()) network.extract(new ItemKey(c), 1, false);
		dropIfFull(level, network.insert(result, false));
		for (ItemStack r : remaining) if (!r.isEmpty()) dropIfFull(level, network.insert(r, false)); // e.g. empty buckets
		return true;
	}

	/** One item per grid slot from storage, or null if something isn't there. */
	private @Nullable List<ItemStack> chooseIngredients(com.robvanblerk.tieredpower.storage.CraftingPattern pattern, boolean allowSubstitutes) {
		Map<ItemKey, Long> taken = new java.util.HashMap<>();
		List<ItemStack> chosen = new java.util.ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			List<ItemKey> options = pattern.options(i);
			if (options.isEmpty()) {
				chosen.add(ItemStack.EMPTY);
				continue;
			}
			if (!allowSubstitutes) options = options.subList(0, 1);
			// Charge-blind, like a crafting table: a charged (or otherwise tagged) copy of a plain item counts too.
			List<ItemKey> expanded = new java.util.ArrayList<>(options);
			for (ItemKey o : options) expanded.addAll(network.variantsOf(o));
			options = expanded;
			ItemKey pick = null;
			for (ItemKey o : options) {
				if (network.count(o) - taken.getOrDefault(o, 0L) >= 1) {
					pick = o;
					break;
				}
			}
			if (pick == null) return null;
			taken.merge(pick, 1L, Long::sum);
			chosen.add(pick.toStack(1));
		}
		return chosen;
	}

	private ItemStack tryGrid(Level level, List<ItemStack> nine) {
		for (int i = 0; i < 9; i++) GRID.setItem(i, nine.get(i).copy());
		ItemStack out = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, GRID, level)
				.map(r -> r.assemble(GRID, level.registryAccess())).orElse(ItemStack.EMPTY);
		GRID.clearContent();
		return out;
	}

	/** Anything storage can't take is dropped on top of the controller rather than lost. */
	private void dropIfFull(Level level, ItemStack left) {
		if (!left.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
	}

	private void tell(Level level, com.robvanblerk.tieredpower.storage.CraftingJob job, String message) {
		if (job.requester == null || level.getServer() == null) return;
		var player = level.getServer().getPlayerList().getPlayer(job.requester);
		if (player != null) player.displayClientMessage(net.minecraft.network.chat.Component.literal(message).withStyle(net.minecraft.ChatFormatting.AQUA), false);
	}

	/** The network's Security Terminal (the first one found), or null if it's an open network. */
	public @Nullable SecurityTerminalBlockEntity securityTerminal() {
		if (network == null || level == null) return null;
		for (BlockPos p : network.securityTerminals()) if (level.getBlockEntity(p) instanceof SecurityTerminalBlockEntity s) return s;
		return null;
	}

	public boolean isOnline() {
		return problem == Problem.NONE && network != null;
	}

	public Problem getProblem() { return problem; }
	public @Nullable StorageNetwork getNetwork() { return isOnline() ? network : null; }
	public int getEnergy() { return energy.getEnergyStored(); }

	public int costPerTick() {
		return Config.use(BASE_COST + (network == null ? 0 : COST_PER_BAY * network.bays().size() + COST_PER_ACCESS_POINT * network.accessPoints().size()
				+ COST_PER_ASSEMBLER * network.assemblers().size() + COST_PER_CPU * network.jobSlots()));
	}

	public StorageNetwork.Stats stats() {
		return isOnline() ? network.stats() : StorageNetwork.Stats.OFFLINE;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StorageControllerBlockEntity be) {
		if (++be.ticks % 20 == 0 || be.network == null) { // rescan the network once a second
			int[] controllers = new int[1];
			StorageNetwork.Parts parts = new StorageNetwork.Parts();
			List<BlockPos> bays = StorageNetwork.scan(level, pos, controllers, parts);
			boolean patternsChanged = be.network == null || !be.network.assemblers().equals(parts.assemblers);
			if (be.network == null || !be.network.bays().equals(bays) || patternsChanged) StorageNetwork.changed();
			be.network = new StorageNetwork(level, bays, parts);
			be.twoControllers = controllers[0] > 1;
		}
		Problem was = be.problem;
		int cost = be.costPerTick();
		if (be.twoControllers) {
			be.problem = Problem.TWO_CONTROLLERS;
		} else if (be.energy.getEnergyStored() >= cost) {
			be.energy.removeInternal(cost);
			be.problem = Problem.NONE;
		} else {
			be.problem = Problem.NO_POWER;
		}
		if (was != be.problem) StorageNetwork.changed();
		if (be.isOnline() && be.ticks % CRAFT_INTERVAL == 0 && !be.jobs.isEmpty()) be.runJobs(level);
		boolean lit = be.isOnline();
		if (state.getValue(StorageControllerBlock.LIT) != lit) level.setBlock(pos, state.setValue(StorageControllerBlock.LIT, lit), Block.UPDATE_ALL);
		if (be.ticks % 20 == 0 && be.network != null) { // access points glow while the network is online
			for (BlockPos ap : be.network.accessPoints()) {
				BlockState s = level.getBlockState(ap);
				if (s.hasProperty(com.robvanblerk.tieredpower.block.AccessPointBlock.LIT) && s.getValue(com.robvanblerk.tieredpower.block.AccessPointBlock.LIT) != lit)
					level.setBlock(ap, s.setValue(com.robvanblerk.tieredpower.block.AccessPointBlock.LIT, lit), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
		if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energyCap.invalidate();
		itemCap.invalidate();
		fluidCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		energyCap = LazyOptional.of(() -> energy);
		itemCap = LazyOptional.of(() -> items);
		fluidCap = LazyOptional.of(() -> fluids);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
		net.minecraft.nbt.ListTag jobList = new net.minecraft.nbt.ListTag();
		for (var job : jobs) jobList.add(job.save());
		tag.put("Jobs", jobList);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		jobs.clear();
		net.minecraft.nbt.ListTag jobList = tag.getList("Jobs", net.minecraft.nbt.Tag.TAG_COMPOUND);
		for (int i = 0; i < jobList.size(); i++) {
			var job = com.robvanblerk.tieredpower.storage.CraftingJob.load(jobList.getCompound(i));
			if (job != null) jobs.add(job);
		}
	}
}
