package com.robvanblerk.tieredpower.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * A recipe recorded on a pattern.
 * Crafting pattern: the 9 grid items (one each) and the result; with substitutes on, each slot also lists the other
 * items the recipe accepts there (any planks, any cobblestone...).
 * Processing pattern: up to 9 item inputs and 3 fluid inputs, up to 3 item outputs and 3 fluid outputs, done by the
 * machine next to the Molecular Assembler holding it. The main output is the first item output, or the first fluid
 * output if there are no items.
 */
public record CraftingPattern(boolean processing, List<ItemStack> inputs, List<ItemStack> outputs, List<List<ItemStack>> substitutes,
		List<FluidStack> fluidInputs, List<FluidStack> fluidOutputs, boolean smelting, String machine) {
	public static final String TAG = "Pattern";

	/** One ingredient need: any of these items (first = the one encoded), this many per craft. */
	public record Requirement(List<ItemKey> options, int count) {}

	/** Item-only pattern (crafting, or processing without fluids). */
	public CraftingPattern(boolean processing, List<ItemStack> inputs, List<ItemStack> outputs, List<List<ItemStack>> substitutes) {
		this(processing, inputs, outputs, substitutes, List.of(), List.of(), false, "");
	}

	public CraftingPattern(boolean processing, List<ItemStack> inputs, List<ItemStack> outputs, List<List<ItemStack>> substitutes,
			List<FluidStack> fluidInputs, List<FluidStack> fluidOutputs) {
		this(processing, inputs, outputs, substitutes, fluidInputs, fluidOutputs, false, "");
	}

	/** This pattern for a given machine (block id, e.g. "tieredpower:electric_furnace"; "" = any machine touching its assembler). */
	public CraftingPattern withMachine(String machineId) {
		return new CraftingPattern(processing, inputs, outputs, substitutes, fluidInputs, fluidOutputs, smelting, machineId == null ? "" : machineId);
	}

	public boolean hasMachine() {
		return !machine.isEmpty();
	}

	/** Is this block the machine the pattern is for? */
	public boolean isForMachine(net.minecraft.world.level.block.Block block) {
		var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
		return id != null && id.toString().equals(machine);
	}

	/**
	 * A built-in smelting step (no pattern item): any assembler touching an Electric Furnace can do it. 'options' are
	 * every item the furnace recipe accepts (raw ore, ore block, dust...).
	 */
	public static CraftingPattern builtInSmelting(List<ItemStack> options, ItemStack result) {
		List<ItemStack> inputs = new ArrayList<>();
		inputs.add(options.get(0).copyWithCount(1));
		List<List<ItemStack>> subs = new ArrayList<>();
		List<ItemStack> rest = new ArrayList<>();
		for (int i = 1; i < options.size(); i++) rest.add(options.get(i).copyWithCount(1));
		subs.add(rest);
		for (int i = 1; i < 9; i++) { inputs.add(ItemStack.EMPTY); subs.add(List.of()); }
		return new CraftingPattern(true, inputs, List.of(result.copy()), subs, List.of(), List.of(), true, "tieredpower:electric_furnace");
	}

	/** Is this fluid plain water? Machines draw their own water, so it never counts as missing. */
	public static boolean isWater(FluidStack f) {
		return f.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER);
	}

	public boolean hasItemOutput() {
		return !outputs.isEmpty();
	}

	/** The main item output, or empty for a fluid-only pattern. */
	public ItemStack output() {
		return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0);
	}

	public ItemKey outputKey() {
		return new ItemKey(output());
	}

	/** How much of the main output one operation makes (items, or mB for a fluid-only pattern). */
	public int mainAmount() {
		return hasItemOutput() ? Math.max(1, output().getCount()) : Math.max(1, fluidOutputs.get(0).getAmount());
	}

	/** Reads a pattern from an encoded pattern item, or null. */
	public static @Nullable CraftingPattern fromStack(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null || !tag.contains(TAG, Tag.TAG_COMPOUND) ? null : load(tag.getCompound(TAG));
	}

	public static @Nullable CraftingPattern load(CompoundTag p) {
		boolean processing = p.getBoolean("Processing");
		List<ItemStack> inputs = readList(p.getList("Inputs", Tag.TAG_COMPOUND), 9);
		List<ItemStack> outputs = new ArrayList<>();
		if (p.contains("Outputs", Tag.TAG_LIST)) {
			for (ItemStack s : readList(p.getList("Outputs", Tag.TAG_COMPOUND), 3)) if (!s.isEmpty()) outputs.add(s);
		} else {
			ItemStack out = ItemStack.of(p.getCompound("Output")); // 1.13.0 patterns
			if (!out.isEmpty()) outputs.add(out);
		}
		List<List<ItemStack>> subs = new ArrayList<>();
		ListTag subList = p.getList("Substitutes", Tag.TAG_LIST);
		for (int i = 0; i < 9; i++) {
			List<ItemStack> options = new ArrayList<>();
			if (i < subList.size()) {
				ListTag l = subList.getList(i);
				for (int j = 0; j < l.size(); j++) {
					ItemStack s = ItemStack.of(l.getCompound(j));
					if (!s.isEmpty()) options.add(s);
				}
			}
			subs.add(options);
		}
		List<FluidStack> fin = readFluids(p.getList("FluidInputs", Tag.TAG_COMPOUND));
		List<FluidStack> fout = readFluids(p.getList("FluidOutputs", Tag.TAG_COMPOUND));
		if (outputs.isEmpty() && fout.isEmpty()) return null;
		return new CraftingPattern(processing, inputs, outputs, subs, fin, fout, p.getBoolean("Smelting"), p.getString("Machine"));
	}

	private static List<ItemStack> readList(ListTag list, int size) {
		List<ItemStack> out = new ArrayList<>(size);
		for (int i = 0; i < size; i++) out.add(i < list.size() ? ItemStack.of(list.getCompound(i)) : ItemStack.EMPTY);
		return out;
	}

	private static List<FluidStack> readFluids(ListTag list) {
		List<FluidStack> out = new ArrayList<>();
		for (int i = 0; i < list.size() && i < 3; i++) {
			FluidStack f = FluidStack.loadFluidStackFromNBT(list.getCompound(i));
			if (!f.isEmpty()) out.add(f);
		}
		return out;
	}

	private static ListTag writeFluids(List<FluidStack> fluids) {
		ListTag l = new ListTag();
		for (FluidStack f : fluids) l.add(f.writeToNBT(new CompoundTag()));
		return l;
	}

	public CompoundTag save() {
		CompoundTag p = new CompoundTag();
		p.putBoolean("Processing", processing);
		ListTag in = new ListTag();
		for (ItemStack s : inputs) in.add(s.isEmpty() ? new CompoundTag() : (processing ? s : s.copyWithCount(1)).save(new CompoundTag()));
		p.put("Inputs", in);
		ListTag out = new ListTag();
		for (ItemStack s : outputs) out.add(s.save(new CompoundTag()));
		p.put("Outputs", out);
		ListTag subs = new ListTag();
		for (List<ItemStack> options : substitutes) {
			ListTag l = new ListTag();
			for (ItemStack s : options) l.add(s.copyWithCount(1).save(new CompoundTag()));
			subs.add(l);
		}
		p.put("Substitutes", subs);
		if (!fluidInputs.isEmpty()) p.put("FluidInputs", writeFluids(fluidInputs));
		if (!fluidOutputs.isEmpty()) p.put("FluidOutputs", writeFluids(fluidOutputs));
		if (smelting) p.putBoolean("Smelting", true);
		if (!machine.isEmpty()) p.putString("Machine", machine);
		return p;
	}

	public void writeTo(ItemStack stack) {
		stack.getOrCreateTag().put(TAG, save());
	}

	public boolean hasSubstitutes() {
		for (List<ItemStack> s : substitutes) if (!s.isEmpty()) return true;
		return false;
	}

	/** Each slot's accepted items: the encoded one first, then its substitutes. */
	public List<ItemKey> options(int slot) {
		List<ItemKey> out = new ArrayList<>();
		ItemStack main = inputs.get(slot);
		if (main.isEmpty()) return out;
		out.add(new ItemKey(main));
		if (slot < substitutes.size()) for (ItemStack s : substitutes.get(slot)) {
			ItemKey k = new ItemKey(s);
			if (!out.contains(k)) out.add(k);
		}
		// Machine patterns: any item sharing the input's material tag works too - so a pattern made with Iron Ore also
		// takes Deepslate Iron Ore (and other mods' iron ore), Raw Iron patterns take any raw iron, and so on.
		if (processing && !main.hasTag()) for (ItemStack alt : materialAlternatives(main)) {
			ItemKey k = new ItemKey(alt);
			if (!out.contains(k)) out.add(k);
		}
		return out;
	}

	private static final String[] MATERIAL_TAGS = {"ores/", "raw_materials/", "dusts/", "ingots/", "gems/", "nuggets/", "storage_blocks/"};

	/** Other items in the same forge material tag (forge:ores/iron, forge:raw_materials/copper, ...), at most 32. */
	public static List<ItemStack> materialAlternatives(ItemStack stack) {
		List<ItemStack> out = new ArrayList<>();
		var tags = stack.getItem().builtInRegistryHolder().tags().filter(t -> {
			var id = t.location();
			if (!id.getNamespace().equals("forge")) return false;
			for (String prefix : MATERIAL_TAGS) if (id.getPath().startsWith(prefix)) return true;
			return false;
		}).toList();
		for (var tag : tags) {
			for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
				if (out.size() >= 32) return out;
				if (holder.value() == stack.getItem()) continue;
				out.add(new ItemStack(holder.value()));
			}
		}
		return out;
	}

	/** What one craft needs in items, with slots that accept the same items merged. */
	public List<Requirement> requirements() {
		Map<List<ItemKey>, Integer> merged = new LinkedHashMap<>();
		for (int i = 0; i < inputs.size(); i++) {
			ItemStack s = inputs.get(i);
			if (s.isEmpty()) continue;
			merged.merge(options(i), processing ? s.getCount() : 1, Integer::sum);
		}
		List<Requirement> out = new ArrayList<>();
		for (Map.Entry<List<ItemKey>, Integer> e : merged.entrySet()) out.add(new Requirement(e.getKey(), e.getValue()));
		return out;
	}

	/** Exact items and amounts of one craft, ignoring substitutes. */
	public Map<ItemKey, Integer> ingredientCounts() {
		Map<ItemKey, Integer> counts = new LinkedHashMap<>();
		for (ItemStack s : inputs) if (!s.isEmpty()) counts.merge(new ItemKey(s), processing ? s.getCount() : 1, Integer::sum);
		return counts;
	}

	/** Fluids (mB) one operation takes. */
	public Map<FluidKey, Integer> fluidCounts() {
		Map<FluidKey, Integer> counts = new LinkedHashMap<>();
		for (FluidStack f : fluidInputs) counts.merge(new FluidKey(f), f.getAmount(), Integer::sum);
		return counts;
	}
}
