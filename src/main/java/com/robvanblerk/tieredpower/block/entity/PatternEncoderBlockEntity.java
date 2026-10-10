package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.menu.PatternEncoderMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.storage.CraftingGrid;
import com.robvanblerk.tieredpower.storage.CraftingPattern;

/**
 * Records recipes onto patterns.
 * Crafting mode: lay out a crafting-table recipe (copies only); the result shows on the right. With Substitutes on, the
 * pattern also accepts the other items the recipe allows in each slot (any planks, any cobblestone...).
 * Processing mode: set up to 9 inputs and 3 outputs with amounts - a machine recipe, done by the machine next to the
 * Molecular Assembler that holds the pattern.
 */
public class PatternEncoderBlockEntity extends BlockEntity implements MenuProvider {
	public static final int BLANK_SLOT = 0, OUTPUT_SLOT = 1;
	public static final int MODE_CRAFTING = 0, MODE_PROCESSING = 1;

	private int mode = MODE_CRAFTING;
	private boolean substitutes = true;

	private final SimpleContainer slots = listening(2, false);
	private final SimpleContainer grid = listening(9, true);
	private final SimpleContainer outputs = listening(3, false);
	private final SimpleContainer fluidIn = listening(3, false);
	private final SimpleContainer fluidOut = listening(3, false);
	/** Processing mode: which machine the pattern is for (a copy of the machine's item). */
	private final SimpleContainer machine = listening(1, false);
	private final SimpleContainer preview = new SimpleContainer(1);

	private final ContainerData data = new ContainerData() {
		@Override public int get(int i) { return i == 0 ? mode : (substitutes ? 1 : 0); }
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return 2; }
	};

	public PatternEncoderBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PATTERN_ENCODER.get(), pos, state);
	}

	private SimpleContainer listening(int size, boolean updatesPreview) {
		return new SimpleContainer(size) {
			@Override
			public void setChanged() {
				super.setChanged();
				if (updatesPreview) updatePreview();
				PatternEncoderBlockEntity.this.setChanged();
			}
		};
	}

	public SimpleContainer slots() { return slots; }
	public SimpleContainer grid() { return grid; }
	public SimpleContainer outputs() { return outputs; }
	public SimpleContainer fluidIn() { return fluidIn; }
	public SimpleContainer fluidOut() { return fluidOut; }
	public SimpleContainer machine() { return machine; }

	/** The block id of the machine in the Machine slot, or "" if none. */
	private String machineId() {
		if (!(machine.getItem(0).getItem() instanceof net.minecraft.world.item.BlockItem bi)) return "";
		var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bi.getBlock());
		return id == null ? "" : id.toString();
	}
	public SimpleContainer preview() { return preview; }
	public ContainerData data() { return data; }

	private List<ItemStack> gridItems() {
		List<ItemStack> out = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) out.add(grid.getItem(i));
		return out;
	}

	private void updatePreview() {
		if (level == null || level.isClientSide()) return;
		preview.setItem(0, mode == MODE_CRAFTING ? CraftingGrid.result(level, gridItems()) : ItemStack.EMPTY);
	}

	/** Turns one Blank Pattern into a pattern for what's set up. */
	public void encode() {
		if (level == null) return;
		ItemStack blank = slots.getItem(BLANK_SLOT);
		if (!blank.is(ModBlocks.BLANK_PATTERN.get()) || !slots.getItem(OUTPUT_SLOT).isEmpty()) return;
		CraftingPattern pattern;
		if (mode == MODE_CRAFTING) {
			updatePreview();
			ItemStack result = preview.getItem(0);
			if (result.isEmpty()) return;
			List<List<ItemStack>> subs = substitutes ? CraftingGrid.substitutes(level, gridItems()) : emptySubs();
			pattern = new CraftingPattern(false, gridItems(), List.of(result.copy()), subs);
		} else {
			List<ItemStack> outs = new ArrayList<>();
			for (int i = 0; i < 3; i++) if (!outputs.getItem(i).isEmpty()) outs.add(outputs.getItem(i).copy());
			List<net.minecraftforge.fluids.FluidStack> fins = fluids(fluidIn), fouts = fluids(fluidOut);
			boolean anyInput = !fins.isEmpty();
			for (ItemStack s : gridItems()) anyInput |= !s.isEmpty();
			if ((outs.isEmpty() && fouts.isEmpty()) || !anyInput) return;
			List<ItemStack> ins = new ArrayList<>();
			for (ItemStack s : gridItems()) ins.add(s.copy());
			pattern = new CraftingPattern(true, ins, outs, emptySubs(), fins, fouts).withMachine(machineId());
		}
		ItemStack encoded = new ItemStack(ModBlocks.CRAFTING_PATTERN.get());
		pattern.writeTo(encoded);
		blank.shrink(1);
		slots.setItem(BLANK_SLOT, blank);
		slots.setItem(OUTPUT_SLOT, encoded);
	}

	private static List<net.minecraftforge.fluids.FluidStack> fluids(SimpleContainer c) {
		List<net.minecraftforge.fluids.FluidStack> out = new ArrayList<>();
		for (int i = 0; i < c.getContainerSize(); i++) {
			var f = com.robvanblerk.tieredpower.item.FluidDropItem.fluid(c.getItem(i));
			if (!f.isEmpty()) out.add(f);
		}
		return out;
	}

	private static List<List<ItemStack>> emptySubs() {
		List<List<ItemStack>> out = new ArrayList<>();
		for (int i = 0; i < 9; i++) out.add(List.of());
		return out;
	}

	public void clearAll() {
		for (int i = 0; i < 9; i++) grid.setItem(i, ItemStack.EMPTY);
		for (int i = 0; i < 3; i++) {
			outputs.setItem(i, ItemStack.EMPTY);
			fluidIn.setItem(i, ItemStack.EMPTY);
			fluidOut.setItem(i, ItemStack.EMPTY);
			machine.setItem(0, ItemStack.EMPTY);
		}
		grid.setChanged();
	}

	public void toggleMode() {
		mode = mode == MODE_CRAFTING ? MODE_PROCESSING : MODE_CRAFTING;
		clearAll();
		setChanged();
	}

	/**
	 * Processing mode: fill every energy item in the outputs to full charge. With the same item as the input, that's
	 * a charging pattern - an assembler next to a Charger sends the item in and takes it back once it's full.
	 */
	public void chargeOutputs() {
		for (int i = 0; i < outputs.getContainerSize(); i++) {
			ItemStack out = outputs.getItem(i);
			if (out.isEmpty()) continue;
			ItemStack charged = out.copy();
			charged.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).ifPresent(e -> {
				for (int guard = 0; guard < 100_000 && e.getEnergyStored() < e.getMaxEnergyStored(); guard++)
					if (e.receiveEnergy(Integer.MAX_VALUE, false) <= 0) break;
			});
			outputs.setItem(i, charged);
		}
		outputs.setChanged();
	}

	public void toggleSubstitutes() {
		substitutes = !substitutes;
		setChanged();
	}

	/** JEI +: a crafting recipe (switches to crafting mode). */
	public void setCrafting(List<ItemStack> items) {
		mode = MODE_CRAFTING;
		for (int i = 0; i < 3; i++) outputs.setItem(i, ItemStack.EMPTY);
		for (int i = 0; i < 9; i++) grid.setItem(i, i < items.size() && !items.get(i).isEmpty() ? items.get(i).copyWithCount(1) : ItemStack.EMPTY);
		grid.setChanged();
	}

	/** JEI +: a machine recipe (switches to processing mode), with amounts; fluids come as Fluid Drop stacks. */
	public void setProcessing(List<ItemStack> ins, List<ItemStack> outs, List<ItemStack> fins, List<ItemStack> fouts, ItemStack machineItem) {
		if (!machineItem.isEmpty()) machine.setItem(0, machineItem.copyWithCount(1));
		mode = MODE_PROCESSING;
		for (int i = 0; i < 9; i++) grid.setItem(i, i < ins.size() ? ins.get(i).copy() : ItemStack.EMPTY);
		for (int i = 0; i < 3; i++) {
			outputs.setItem(i, i < outs.size() ? outs.get(i).copy() : ItemStack.EMPTY);
			fluidIn.setItem(i, i < fins.size() ? fins.get(i).copy() : ItemStack.EMPTY);
			fluidOut.setItem(i, i < fouts.size() ? fouts.get(i).copy() : ItemStack.EMPTY);
		}
		grid.setChanged();
	}

	private static ListTag save(SimpleContainer c) {
		ListTag list = new ListTag();
		for (int i = 0; i < c.getContainerSize(); i++) list.add(c.getItem(i).save(new CompoundTag()));
		return list;
	}

	private static void load(SimpleContainer c, ListTag list) {
		for (int i = 0; i < c.getContainerSize(); i++) c.setItem(i, i < list.size() ? ItemStack.of(list.getCompound(i)) : ItemStack.EMPTY);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("Slots", save(slots));
		tag.put("Grid", save(grid));
		tag.put("Outputs", save(outputs));
		tag.put("FluidIn", save(fluidIn));
		tag.put("FluidOut", save(fluidOut));
		tag.put("Machine", save(machine));
		tag.putInt("Mode", mode);
		tag.putBoolean("NoSubstitutes", !substitutes);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		load(slots, tag.getList("Slots", Tag.TAG_COMPOUND));
		load(grid, tag.getList("Grid", Tag.TAG_COMPOUND));
		load(outputs, tag.getList("Outputs", Tag.TAG_COMPOUND));
		load(fluidIn, tag.getList("FluidIn", Tag.TAG_COMPOUND));
		load(fluidOut, tag.getList("FluidOut", Tag.TAG_COMPOUND));
		load(machine, tag.getList("Machine", Tag.TAG_COMPOUND));
		mode = tag.getInt("Mode");
		substitutes = !tag.getBoolean("NoSubstitutes");
	}

	@Override
	public void onLoad() {
		super.onLoad();
		updatePreview();
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.pattern_encoder");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new PatternEncoderMenu(id, inv, this);
	}
}
