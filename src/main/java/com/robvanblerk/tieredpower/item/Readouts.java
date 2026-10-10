package com.robvanblerk.tieredpower.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.entity.EnergyMeterBlockEntity;
import com.robvanblerk.tieredpower.energy.CableNetwork;
import com.robvanblerk.tieredpower.energy.PowerInfo;

/** The chat readouts shared by the Multimeter, Energy Meter and Power Monitor. */
public final class Readouts {
	private static MutableComponent line(String label, String value, ChatFormatting colour) {
		return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY).append(Component.literal(value).withStyle(colour));
	}

	private static MutableComponent header(String text) {
		return Component.literal("-- " + text + " --").withStyle(ChatFormatting.GOLD);
	}

	public static void send(Player player, List<Component> lines) {
		for (Component c : lines) player.sendSystemMessage(c);
	}

	public static List<Component> network(@Nullable CableNetwork.Stats s) {
		List<Component> out = new ArrayList<>();
		out.add(header("Cable network"));
		if (s == null) {
			out.add(Component.literal("Not touching a cable network").withStyle(ChatFormatting.RED));
			return out;
		}
		out.add(line("Coming in", PowerInfo.shortFe(s.in()) + " FE/t", ChatFormatting.GREEN));
		out.add(line("To machines", PowerInfo.shortFe(s.toMachines()) + " FE/t", ChatFormatting.YELLOW));
		out.add(line("Into storage", PowerInfo.shortFe(s.toStorage()) + " FE/t", ChatFormatting.AQUA));
		if (s.capacity() > 0) {
			int pct = (int) Math.round(100.0 * s.stored() / s.capacity());
			out.add(line("Stored", PowerInfo.shortFe(s.stored()) + " / " + PowerInfo.shortFe(s.capacity()) + " FE (" + pct + "%)", ChatFormatting.AQUA));
		} else {
			out.add(line("Stored", "no batteries on this network", ChatFormatting.DARK_GRAY));
		}
		out.add(line("Connected", s.cables() + " cables, " + s.generators() + " generators, " + s.machines() + " machines, " + s.storages() + " storage",
				ChatFormatting.WHITE));
		if (s.in() > 0 && s.toMachines() + s.toStorage() == 0) out.add(Component.literal("Power is arriving but nothing is taking it").withStyle(ChatFormatting.RED));
		return out;
	}

	public static List<Component> meter(EnergyMeterBlockEntity m) {
		List<Component> out = new ArrayList<>();
		out.add(header("Energy Meter"));
		if (m.isBlocked()) out.add(Component.literal("Switched OFF by redstone").withStyle(ChatFormatting.RED));
		out.add(line("Flowing", String.format("%,d FE/t", m.getFlow()), ChatFormatting.GREEN));
		out.add(line("Peak", String.format("%,d FE/t", m.getPeak()), ChatFormatting.YELLOW));
		out.add(line("Total", PowerInfo.shortFe(m.getTotal()) + " FE", ChatFormatting.AQUA));
		out.add(Component.literal("Sneak + right-click the meter to reset").withStyle(ChatFormatting.DARK_GRAY));
		return out;
	}

	/** Energy, generator output and fluids of any block (ours or another mod's). */
	public static List<Component> block(BlockEntity be, Direction face) {
		List<Component> out = new ArrayList<>();
		out.add(header(be.getBlockState().getBlock().getName().getString()));
		IEnergyStorage energy = be.getCapability(ForgeCapabilities.ENERGY, null).orElse(null);
		if (energy == null) energy = be.getCapability(ForgeCapabilities.ENERGY, face).orElse(null);
		if (energy != null) {
			int max = energy.getMaxEnergyStored();
			int pct = max <= 0 ? 0 : (int) Math.round(100.0 * energy.getEnergyStored() / max);
			out.add(line("Energy", String.format("%,d / %,d FE (%d%%)", energy.getEnergyStored(), max, pct), ChatFormatting.AQUA));
		}
		if (be instanceof com.robvanblerk.tieredpower.block.entity.LightningCollectorBlockEntity lc)
			out.add(line("Strikes caught", String.format("%,d", lc.getStrikes()), ChatFormatting.YELLOW));
		int gen = PowerInfo.generating(be);
		if (gen >= 0) out.add(line("Making", String.format("%,d FE/t", gen), ChatFormatting.GREEN));
		IFluidHandler fluids = be.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
		if (fluids == null) fluids = be.getCapability(ForgeCapabilities.FLUID_HANDLER, face).orElse(null);
		if (fluids != null) {
			for (int t = 0; t < fluids.getTanks(); t++) {
				FluidStack f = fluids.getFluidInTank(t);
				String name = f.isEmpty() ? "empty" : f.getDisplayName().getString();
				out.add(line("Tank " + (t + 1), String.format("%s, %,d / %,d mB", name, f.getAmount(), fluids.getTankCapacity(t)), ChatFormatting.BLUE));
			}
		}
		if (be instanceof com.robvanblerk.tieredpower.block.entity.MachineBlockEntity m) machineFaces(m, out);
		if (out.size() == 1) out.add(Component.literal("Nothing to measure here").withStyle(ChatFormatting.DARK_GRAY));
		return out;
	}

	private static String dirName(Direction d) {
		String n = d.getName();
		return Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	/** The first item this handler would give up, or empty. */
	private static ItemStack peek(net.minecraftforge.items.IItemHandler h) {
		for (int s = 0; s < h.getSlots(); s++) {
			ItemStack st = h.extractItem(s, 64, true);
			if (!st.isEmpty()) return st;
		}
		return ItemStack.EMPTY;
	}

	/** Every connection of an item pipe, what the Pull ones can take and whether the Push ones would accept it. */
	public static List<Component> itemPipe(com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity pipe) {
		List<Component> out = new ArrayList<>();
		out.add(header("Item pipe"));
		var level = pipe.getLevel();
		if (level == null) return out;
		List<ItemStack> offered = new ArrayList<>();
		List<net.minecraftforge.items.IItemHandler> pushTargets = new ArrayList<>();
		List<String> pushNames = new ArrayList<>();
		for (Direction d : Direction.values()) {
			var mode = pipe.getBlockState().getValue(com.robvanblerk.tieredpower.block.FluidPipeBlock.SIDES.get(d));
			if (mode == com.robvanblerk.tieredpower.energy.PipeSide.NONE) continue;
			BlockEntity n = level.getBlockEntity(pipe.getBlockPos().relative(d));
			if (n instanceof com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity) continue; // pipe-to-pipe
			String name = n == null ? "nothing" : n.getBlockState().getBlock().getName().getString();
			var h = n == null ? null : n.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).orElse(null);
			String modeName = mode.pulls() && mode.pushes() ? "Push + Pull" : mode.pulls() ? "Pull" : mode.pushes() ? "Push" : "Disabled";
			String extra = "";
			if (h == null) extra = " - no inventory on that face";
			else if (mode.pulls()) {
				ItemStack p = peek(h);
				extra = p.isEmpty() ? " - nothing it will give up" : " - can take " + p.getCount() + " " + p.getHoverName().getString();
				if (!p.isEmpty()) offered.add(p);
			}
			if (h != null && mode.pushes()) {
				pushTargets.add(h);
				pushNames.add(dirName(d) + " (" + name + ")");
			}
			ItemStack f = pipe.getFilter(d);
			if (!f.isEmpty()) extra += ", filter: " + f.getHoverName().getString();
			out.add(line(dirName(d), modeName + " " + (mode.pulls() && !mode.pushes() ? "from " : "into ") + name + extra,
					mode == com.robvanblerk.tieredpower.energy.PipeSide.DISABLED ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE));
		}
		if (pushTargets.isEmpty()) out.add(Component.literal("No Push connections: pulled items have nowhere to go").withStyle(ChatFormatting.RED));
		for (ItemStack item : offered) {
			for (int i = 0; i < pushTargets.size(); i++) {
				boolean ok = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(pushTargets.get(i), item.copyWithCount(1), true).isEmpty();
				out.add(Component.literal(pushNames.get(i) + (ok ? " accepts " : " REFUSES ") + item.getHoverName().getString())
						.withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
			}
		}
		var network = pipe.network();
		if (network != null && network.isStuck()) out.add(Component.literal("Network is stuck: items found but nothing takes them").withStyle(ChatFormatting.RED));
		if (network == null) out.add(Component.literal("Pipe network not built yet - try again in a moment").withStyle(ChatFormatting.YELLOW));
		return out;
	}

	/** A machine's faces: setting and what an Out face is offering. */
	public static void machineFaces(com.robvanblerk.tieredpower.block.entity.MachineBlockEntity m, List<Component> out) {
		if (!m.getBlockState().hasProperty(com.robvanblerk.tieredpower.block.MachineBlock.FACING)) return;
		Direction front = m.getBlockState().getValue(com.robvanblerk.tieredpower.block.MachineBlock.FACING);
		for (Direction d : Direction.values()) {
			String faceName = com.robvanblerk.tieredpower.energy.SideConfig.FACES[com.robvanblerk.tieredpower.energy.SideConfig.faceIndex(front, d)] + " (" + dirName(d) + ")";
			if (d == Direction.DOWN) {
				out.add(line("Bottom", "power face", ChatFormatting.RED));
				continue;
			}
			int mode = m.sideMode(d);
			String text = com.robvanblerk.tieredpower.energy.SideConfig.MODES[mode];
			if (com.robvanblerk.tieredpower.energy.SideConfig.canOutput(mode)) {
				var h = m.getCapability(ForgeCapabilities.ITEM_HANDLER, d).orElse(null);
				ItemStack p = h == null ? ItemStack.EMPTY : peek(h);
				text += p.isEmpty() ? " (nothing ready)" : " (" + p.getCount() + " " + p.getHoverName().getString() + " ready)";
			}
			out.add(line(faceName, text, mode == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE));
		}
		out.add(line("Auto-output", m.isAutoOutput() ? "on" : "off", ChatFormatting.WHITE));
	}

	private Readouts() {}
}
