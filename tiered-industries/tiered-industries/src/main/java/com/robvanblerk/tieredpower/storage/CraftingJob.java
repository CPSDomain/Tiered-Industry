package com.robvanblerk.tieredpower.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** A running autocraft: what was asked for, who asked, and the steps still to do. Saved with the Storage Controller. */
public class CraftingJob {
	public final ItemStack target;
	public final long amount;
	public final @Nullable UUID requester;
	public final List<CraftingPlanner.Step> steps;
	public String status = "Queued";
	public int idleCycles;

	public CraftingJob(ItemStack target, long amount, @Nullable UUID requester, List<CraftingPlanner.Step> steps) {
		this.target = target;
		this.amount = amount;
		this.requester = requester;
		this.steps = steps;
	}

	public int craftsLeft() {
		int n = 0;
		for (CraftingPlanner.Step s : steps) n += s.crafts + s.pending;
		return n;
	}

	public CompoundTag save() {
		CompoundTag t = new CompoundTag();
		t.put("Target", target.save(new CompoundTag()));
		t.putLong("Amount", amount);
		if (requester != null) t.putUUID("Requester", requester);
		ListTag list = new ListTag();
		for (CraftingPlanner.Step s : steps) {
			CompoundTag st = new CompoundTag();
			st.put("Pattern", s.pattern.save());
			st.putInt("Crafts", s.crafts);
			st.putInt("Pending", s.pending);
			st.putInt("Received", s.received);
			list.add(st);
		}
		t.put("Steps", list);
		return t;
	}

	public static @Nullable CraftingJob load(CompoundTag t) {
		ItemStack target = ItemStack.of(t.getCompound("Target"));
		List<CraftingPlanner.Step> steps = new ArrayList<>();
		ListTag list = t.getList("Steps", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag st = list.getCompound(i);
			CraftingPattern p = CraftingPattern.load(st.getCompound("Pattern"));
			if (p == null || (st.getInt("Crafts") <= 0 && st.getInt("Pending") <= 0)) continue;
			CraftingPlanner.Step step = new CraftingPlanner.Step(p, st.getInt("Crafts"));
			step.pending = st.getInt("Pending");
			step.received = st.getInt("Received");
			steps.add(step);
		}
		if (target.isEmpty() || steps.isEmpty()) return null;
		return new CraftingJob(target, t.getLong("Amount"), t.hasUUID("Requester") ? t.getUUID("Requester") : null, steps);
	}
}
