package com.robvanblerk.tieredpower.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Works out how to make an item from what's in storage plus the network's patterns: which stored items and fluids get
 * used, what gets crafted along the way (sub-parts first), and what's missing. Every output of a step - byproducts
 * included - is counted as available for later steps.
 */
public final class CraftingPlanner {
	public static final int MAX_DEPTH = 24;

	/** One pattern to run a number of times. Steps are in the order they can run (ingredients before what uses them). */
	public static final class Step {
		public final CraftingPattern pattern;
		/** Crafts (or machine operations) still to start. */
		public int crafts;
		/** Processing: operations sent to a machine whose results haven't come back yet, and main output received so far. */
		public int pending, received;
		/** The machine this step last sent work to ("Electric Furnace at 10, 64, -3"), for status messages. */
		public String machine;

		public Step(CraftingPattern pattern, int crafts) {
			this.pattern = pattern;
			this.crafts = crafts;
		}

		public boolean done() {
			return crafts <= 0 && pending <= 0;
		}
	}

	public record Plan(List<Step> steps, Map<ItemKey, Long> used, Map<ItemKey, Long> crafted, Map<ItemKey, Long> missing,
			Map<FluidKey, Long> usedFluids, Map<FluidKey, Long> madeFluids, Map<FluidKey, Long> missingFluids) {
		public boolean possible() {
			return missing.isEmpty() && missingFluids.isEmpty();
		}

		public static Plan impossible(ItemKey item, long amount) {
			return new Plan(List.of(), Map.of(), Map.of(), Map.of(item, amount), Map.of(), Map.of(), Map.of());
		}
	}

	private final Map<ItemKey, Long> available = new HashMap<>();
	private final Map<FluidKey, Long> availableFluids = new HashMap<>();
	private final Map<ItemKey, CraftingPattern> patterns = new HashMap<>();
	private final Map<FluidKey, CraftingPattern> fluidPatterns = new HashMap<>();
	private final List<Step> steps = new ArrayList<>();
	private final Map<ItemKey, Long> used = new LinkedHashMap<>(), crafted = new LinkedHashMap<>(), missing = new LinkedHashMap<>();
	private final Map<FluidKey, Long> usedFluids = new LinkedHashMap<>(), madeFluids = new LinkedHashMap<>(), missingFluids = new LinkedHashMap<>();
	private final Set<Object> inProgress = new HashSet<>();
	/** Built-in steps (smelting) for items no pattern makes; null when the network can't do them. */
	private final java.util.function.Function<ItemKey, CraftingPattern> builtIn;

	private CraftingPlanner(Map<ItemKey, Long> stock, Map<FluidKey, Long> fluidStock, List<CraftingPattern> known,
			java.util.function.Function<ItemKey, CraftingPattern> builtIn) {
		this.builtIn = builtIn;
		available.putAll(stock);
		availableFluids.putAll(fluidStock);
		for (CraftingPattern p : known) {
			if (p.hasItemOutput()) patterns.putIfAbsent(p.outputKey(), p);
			for (FluidStack f : p.fluidOutputs()) fluidPatterns.putIfAbsent(new FluidKey(f), p);
		}
	}

	public static Plan plan(Map<ItemKey, Long> stock, Map<FluidKey, Long> fluidStock, List<CraftingPattern> patterns, ItemKey target, long amount) {
		return plan(stock, fluidStock, patterns, null, target, amount);
	}

	public static Plan plan(Map<ItemKey, Long> stock, Map<FluidKey, Long> fluidStock, List<CraftingPattern> patterns,
			java.util.function.Function<ItemKey, CraftingPattern> builtIn, ItemKey target, long amount) {
		CraftingPlanner planner = new CraftingPlanner(stock, fluidStock, patterns, builtIn);
		planner.craft(target, amount, 0); // the requested item is always crafted, not just taken from storage
		return new Plan(planner.steps, planner.used, planner.crafted, planner.missing, planner.usedFluids, planner.madeFluids, planner.missingFluids);
	}

	/**
	 * Crafting recipes accept a plain item with any NBT (a crafting table doesn't care whether a jetpack is charged), so
	 * stored copies with NBT count as options too. Processing patterns stay exact - a charging pattern needs the
	 * difference.
	 */
	private List<ItemKey> withStoredVariants(List<ItemKey> options) {
		List<ItemKey> out = new ArrayList<>(options);
		for (ItemKey o : options) {
			if (!o.isPlain()) continue;
			for (ItemKey k : available.keySet()) if (k.sameItem(o) && k.isChargedVariant() && !out.contains(k)) out.add(k);
		}
		return out;
	}

	/** The pattern that makes this item: an encoded one first, otherwise a built-in step (smelting) if there is one. */
	private @org.jetbrains.annotations.Nullable CraftingPattern patternFor(ItemKey item) {
		CraftingPattern p = patterns.get(item);
		if (p == null && builtIn != null && item.isPlain()) {
			p = builtIn.apply(item);
			if (p != null) patterns.put(item, p);
		}
		return p;
	}

	/** Get 'qty' of any of these items: from storage first (plentiful items first), then by crafting. */
	private void need(List<ItemKey> allOptions, long qty, int depth) {
		// Never feed a step with the item we're in the middle of making - e.g. grinding stored gold ingots into dust just
		// to smelt them back into gold ingots. That would loop forever and get nowhere.
		List<ItemKey> options = allOptions.stream().filter(o -> !inProgress.contains(o)).toList();
		if (options.isEmpty()) {
			missing.merge(allOptions.get(0), qty, Long::sum);
			return;
		}
		List<ItemKey> byStock = new ArrayList<>(options);
		byStock.sort((a, b) -> Long.compare(available.getOrDefault(b, 0L), available.getOrDefault(a, 0L)));
		for (ItemKey item : byStock) {
			if (qty <= 0) return;
			long have = available.getOrDefault(item, 0L);
			long take = Math.min(have, qty);
			if (take > 0) {
				available.put(item, have - take);
				used.merge(item, take, Long::sum);
				qty -= take;
			}
		}
		if (qty <= 0) return;
		for (ItemKey item : options) { // craft the first one there's a pattern for (the encoded item first)
			if (!inProgress.contains(item) && patternFor(item) != null) {
				craft(item, qty, depth);
				return;
			}
		}
		missing.merge(options.get(0), qty, Long::sum);
	}

	/** Get 'mb' of a fluid: from storage first, then from a pattern that makes it. */
	private void needFluid(FluidKey fluid, long mb, int depth) {
		if (inProgress.contains(fluid)) { // same rule as items: no feeding a fluid back into its own making
			missingFluids.merge(fluid, mb, Long::sum);
			return;
		}
		long have = availableFluids.getOrDefault(fluid, 0L);
		long take = Math.min(have, mb);
		if (take > 0) {
			availableFluids.put(fluid, have - take);
			usedFluids.merge(fluid, take, Long::sum);
			mb -= take;
		}
		if (mb <= 0) return;
		CraftingPattern p = fluidPatterns.get(fluid);
		if (p == null || depth >= MAX_DEPTH || inProgress.contains(fluid)) {
			missingFluids.merge(fluid, mb, Long::sum);
			return;
		}
		int perOp = 0;
		for (FluidStack f : p.fluidOutputs()) if (fluid.equals(new FluidKey(f))) perOp += f.getAmount();
		long ops = (mb + Math.max(1, perOp) - 1) / Math.max(1, perOp);
		inProgress.add(fluid);
		run(p, ops, depth);
		inProgress.remove(fluid);
		availableFluids.merge(fluid, -mb, Long::sum); // what we needed out of what was made
	}

	private void craft(ItemKey item, long qty, int depth) {
		CraftingPattern p = patternFor(item);
		if (p == null || depth >= MAX_DEPTH || inProgress.contains(item)) {
			missing.merge(item, qty, Long::sum);
			return;
		}
		long ops = (qty + p.mainAmount() - 1) / p.mainAmount();
		inProgress.add(item);
		run(p, ops, depth);
		inProgress.remove(item);
		available.merge(item, -qty, Long::sum); // what we needed out of what was made
	}

	/** Runs a pattern 'ops' times: gathers its inputs, adds the step, and makes all its outputs available. */
	private void run(CraftingPattern p, long ops, int depth) {
		for (CraftingPattern.Requirement r : p.requirements())
			need(p.processing() ? r.options() : withStoredVariants(r.options()), ops * r.count(), depth + 1);
		for (Map.Entry<FluidKey, Integer> e : p.fluidCounts().entrySet()) {
			if (CraftingPattern.isWater(e.getKey().toStack(1))) continue; // machines draw their own water (Sink, pump, pipe)
			needFluid(e.getKey(), ops * e.getValue(), depth + 1);
		}
		steps.add(new Step(p, (int) Math.min(Integer.MAX_VALUE, ops)));
		for (ItemStack out : p.outputs()) {
			ItemKey k = new ItemKey(out);
			available.merge(k, ops * out.getCount(), Long::sum);
			crafted.merge(k, ops * out.getCount(), Long::sum);
		}
		for (FluidStack out : p.fluidOutputs()) {
			FluidKey k = new FluidKey(out);
			availableFluids.merge(k, ops * out.getAmount(), Long::sum);
			madeFluids.merge(k, ops * out.getAmount(), Long::sum);
		}
	}
}
