package com.robvanblerk.tieredpower.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.block.CableBlock;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.BankControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.BatteryBoxBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FusionControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;
import com.robvanblerk.tieredpower.energy.RedstoneMode;

/** The individual Jade tooltip providers. Server side puts numbers in a tag; client side turns them into lines. */
final class TieredPowerProviders {
	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, path);
	}

	static String shortFe(long fe) {
		String[] units = {"", " K", " M", " G", " T", " P"};
		double v = fe;
		int u = 0;
		while (v >= 1000 && u < units.length - 1) {
			v /= 1000;
			u++;
		}
		return u == 0 ? String.format("%d", fe) : String.format("%.2f%s", v, units[u]);
	}

	/** Every machine: running/idle, redstone mode, upgrades. */
	enum Machine implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			if (accessor.getBlockEntity() instanceof MachineBlockEntity tm) {
				if (tm.supportsTiers() && tm.getTier() > 0) data.putString("tpTier", MachineBlockEntity.TIER_NAMES[tm.getTier()] + " - " + tm.tierEffect(tm.getTier()));
				if (tm.isMuffled()) data.putBoolean("tpMuffled", true);
				if ((tm instanceof com.robvanblerk.tieredpower.block.entity.FissionReactorBlockEntity fr && fr.hasReflector())
						|| (tm instanceof com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity fc && fc.hasReflector())) data.putBoolean("tpReflector", true);
				int plasma = tm instanceof com.robvanblerk.tieredpower.block.entity.FusionControllerBlockEntity fu ? fu.getPlasmaTier()
						: tm instanceof com.robvanblerk.tieredpower.block.entity.FusionReactorBlockEntity fs ? fs.getPlasmaTier() : 0;
				if (plasma > 0) data.putString("tpPlasma", "Plasma " + com.robvanblerk.tieredpower.block.entity.PlasmaTiers.NAMES[plasma] + ": x" + com.robvanblerk.tieredpower.block.entity.PlasmaTiers.OUTPUT[plasma] + " output");
			}
			if (accessor.getBlockEntity() instanceof MachineBlockEntity machine) {
				data.putInt("tpRedstone", machine.getRedstoneMode().ordinal());
				data.putInt("tpSpeed", machine.speedUpgrades());
				data.putInt("tpEfficiency", machine.efficiencyUpgrades());
			}
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag up = accessor.getServerData();
			if (up.contains("tpTier")) tooltip.add(Component.literal(up.getString("tpTier")).withStyle(ChatFormatting.LIGHT_PURPLE));
			if (up.getBoolean("tpMuffled")) tooltip.add(Component.literal("Muffled").withStyle(ChatFormatting.GRAY));
			if (up.contains("tpPlasma")) tooltip.add(Component.literal(up.getString("tpPlasma")).withStyle(ChatFormatting.AQUA));
			if (up.getBoolean("tpReflector")) tooltip.add(Component.literal("Neutron Reflector: rods last 50% longer").withStyle(ChatFormatting.LIGHT_PURPLE));
			if (accessor.getBlockState().hasProperty(MachineBlock.LIT)) {
				boolean running = accessor.getBlockState().getValue(MachineBlock.LIT);
				tooltip.add(Component.literal(running ? "Running" : "Idle").withStyle(running ? ChatFormatting.GREEN : ChatFormatting.GRAY));
			}
			CompoundTag data = accessor.getServerData();
			if (data.contains("tpRedstone")) {
				RedstoneMode mode = RedstoneMode.byId(data.getInt("tpRedstone"));
				if (mode != RedstoneMode.ALWAYS) tooltip.add(Component.literal("Redstone: " + mode.getDescription()).withStyle(ChatFormatting.RED));
			}
			int speed = data.getInt("tpSpeed"), eff = data.getInt("tpEfficiency");
			if (speed > 0 || eff > 0) {
				tooltip.add(Component.literal("Upgrades: " + speed + " Speed, " + eff + " Efficiency").withStyle(ChatFormatting.AQUA));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return id("machine");
		}
	}

	/** Batteries: live in/out rates. */
	enum Battery implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			if (accessor.getBlockEntity() instanceof BatteryBoxBlockEntity battery) {
				data.putInt("tpIn", battery.getRateIn());
				data.putInt("tpOut", battery.getRateOut());
			}
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();
			if (!data.contains("tpIn")) return;
			tooltip.add(Component.literal("In: " + shortFe(data.getInt("tpIn")) + "FE/t   Out: " + shortFe(data.getInt("tpOut")) + "FE/t"));
		}

		@Override
		public ResourceLocation getUid() {
			return id("battery");
		}
	}

	/** Multiblock Fusion Reactor controller. */
	enum Fusion implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			if (accessor.getBlockEntity() instanceof FusionControllerBlockEntity c) {
				data.putBoolean("tpFormed", c.isFormed());
				data.putInt("tpCoils", c.getCoils());
				data.putInt("tpGenerated", c.getGenerated());
				data.putInt("tpTemp", c.getTemperature());
			}
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();
			if (!data.contains("tpFormed")) return;
			if (!data.getBoolean("tpFormed")) {
				tooltip.add(Component.literal("Not formed - right-click for details").withStyle(ChatFormatting.RED));
				return;
			}
			tooltip.add(Component.literal(data.getInt("tpCoils") + " Magnet Coils, plasma " + data.getInt("tpTemp") / 10 + "%"));
			tooltip.add(Component.literal("Making " + shortFe(data.getInt("tpGenerated")) + "FE/t").withStyle(ChatFormatting.GREEN));
		}

		@Override
		public ResourceLocation getUid() {
			return id("fusion");
		}
	}

	/** Energy Bank controller. */
	enum Bank implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			if (accessor.getBlockEntity() instanceof BankControllerBlockEntity b) {
				data.putBoolean("tpFormed", b.isFormed());
				data.putLong("tpStored", b.getStored());
				data.putLong("tpCapacity", b.getCapacity());
				data.putLong("tpIn", b.getRateIn());
				data.putLong("tpOut", b.getRateOut());
				data.putInt("tpCells", b.getCells());
			}
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();
			if (!data.contains("tpFormed")) return;
			if (!data.getBoolean("tpFormed")) {
				tooltip.add(Component.literal("Not formed - right-click for details").withStyle(ChatFormatting.RED));
				return;
			}
			long stored = data.getLong("tpStored"), cap = Math.max(1, data.getLong("tpCapacity"));
			tooltip.add(Component.literal(shortFe(stored) + "FE / " + shortFe(cap) + "FE (" + String.format("%.1f", 100.0 * stored / cap) + "%)"));
			tooltip.add(Component.literal("In: " + shortFe(data.getLong("tpIn")) + "FE/t   Out: " + shortFe(data.getLong("tpOut")) + "FE/t"));
			tooltip.add(Component.literal(data.getInt("tpCells") + " cells").withStyle(ChatFormatting.GRAY));
		}

		@Override
		public ResourceLocation getUid() {
			return id("bank");
		}
	}

	/** Cables: tier rate (no server data needed). */
	enum Cable implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (accessor.getBlock() instanceof CableBlock cable) {
				tooltip.add(Component.literal(String.format("Up to %,d FE/t per connection", cable.getTier().getTransferRate()))
						.withStyle(ChatFormatting.GRAY));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return id("cable");
		}
	}

	/** Crafting CPU and Crafting Accelerator: tier and what it gives. */
	enum CraftingTierInfo implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			var state = accessor.getBlockState();
			int t = com.robvanblerk.tieredpower.storage.CraftingTier.of(state);
			String gain = state.getBlock() instanceof com.robvanblerk.tieredpower.block.CraftingCpuBlock
					? com.robvanblerk.tieredpower.storage.CraftingTier.CPU_JOBS[t] + " job" + (t > 0 ? "s" : "") + " at once"
					: state.getBlock() instanceof com.robvanblerk.tieredpower.block.MachineConnectorBlock
							? com.robvanblerk.tieredpower.storage.CraftingTier.CONNECTOR_OPS[t] + " operations per cycle"
							: "+" + com.robvanblerk.tieredpower.storage.CraftingTier.ACCELERATOR_CRAFTS[t] + " crafts per cycle";
			tooltip.add(Component.literal(com.robvanblerk.tieredpower.storage.CraftingTier.NAMES[t] + " - " + gain)
					.withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(com.robvanblerk.tieredpower.storage.CraftingTier.COLOUR[t])));
		}

		@Override
		public ResourceLocation getUid() {
			return id("crafting_tier");
		}
	}

	/** Pipes: kind and rate; for Item Pipes, the filter and priority on each connection. */
	enum Pipe implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			net.minecraft.nbt.ListTag lines = new net.minecraft.nbt.ListTag();
			var state = accessor.getBlockState();
			if (state.hasProperty(com.robvanblerk.tieredpower.block.FluidPipeBlock.SIDES.get(net.minecraft.core.Direction.NORTH))) {
				for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
					var mode = state.getValue(com.robvanblerk.tieredpower.block.FluidPipeBlock.SIDES.get(dir));
					if (mode == com.robvanblerk.tieredpower.energy.PipeSide.NONE) continue;
					String label = switch (mode) {
						case PIPE -> "push";
						case EXTRACT -> "PULL";
						case BOTH -> "push + PULL";
						case DISABLED -> "disabled";
						default -> "";
					};
					lines.add(net.minecraft.nbt.StringTag.valueOf(dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1) + ": " + label));
				}
			}
			if (!(accessor.getBlockEntity() instanceof com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity pipe)) {
				data.put("tpPipe", lines);
				return;
			}
			var network = pipe.getNetwork();
			if (network != null && !network.hasDestinations(accessor.getLevel())) {
				data.putString("tpWarn", "Nothing on this pipe network to deliver to - set a connection to Push");
			} else if (network != null && network.isStuck()) {
				data.putString("tpWarn", "Nothing on this network will accept the pulled items");
			}
			for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
				ItemStack filter = pipe.getFilter(dir);
				int priority = pipe.getPriority(dir);
				if (filter.isEmpty() && priority == 0) continue;
				StringBuilder line = new StringBuilder(dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1) + ":");
				if (priority != 0) line.append(" priority ").append(priority);
				if (!filter.isEmpty()) {
					long count = com.robvanblerk.tieredpower.item.ItemFilterItem.getItems(filter).stream().filter(java.util.Objects::nonNull).count();
					line.append(priority != 0 ? "," : "").append(" filter (")
							.append(com.robvanblerk.tieredpower.item.ItemFilterItem.isWhitelist(filter) ? "allow " : "block ")
							.append(count).append(" by ").append(com.robvanblerk.tieredpower.item.ItemFilterItem.MODES[com.robvanblerk.tieredpower.item.ItemFilterItem.getMode(filter)].toLowerCase()).append(")");
				}
				lines.add(net.minecraft.nbt.StringTag.valueOf(line.toString()));
			}
			data.put("tpPipe", lines);
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (accessor.getBlock() instanceof com.robvanblerk.tieredpower.block.FluidPipeBlock pipe) {
				tooltip.add(Component.literal(String.format("Up to %,d mB/t per connection", pipe.getTier().getRate())).withStyle(ChatFormatting.GRAY));
				net.minecraft.nbt.ListTag modes = accessor.getServerData().getList("tpPipe", 8);
				for (int i = 0; i < modes.size(); i++) tooltip.add(Component.literal(modes.getString(i)).withStyle(ChatFormatting.AQUA));
			} else if (accessor.getBlock() instanceof com.robvanblerk.tieredpower.block.ItemPipeBlock pipe) {
				tooltip.add(Component.literal(String.format("Pulls up to %,d items/s per pull connection", pipe.getTier().getItemsPerSecond())).withStyle(ChatFormatting.GRAY));
				net.minecraft.nbt.ListTag lines = accessor.getServerData().getList("tpPipe", 8);
				for (int i = 0; i < lines.size(); i++) tooltip.add(Component.literal(lines.getString(i)).withStyle(ChatFormatting.AQUA));
				if (accessor.getServerData().contains("tpWarn")) tooltip.add(Component.literal(accessor.getServerData().getString("tpWarn")).withStyle(ChatFormatting.RED));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return id("pipe");
		}
	}

	/** Solar panels and wind turbines: what they're making right now. */
	enum Generator implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			if (accessor.getBlockEntity() instanceof com.robvanblerk.tieredpower.block.entity.SolarPanelBlockEntity s) data.putInt("tpGen", s.getGenerating());
			if (accessor.getBlockEntity() instanceof com.robvanblerk.tieredpower.block.entity.GasBurnerGeneratorBlockEntity gb) data.putInt("tpGen", gb.getGenerating());
			if (accessor.getBlockEntity() instanceof com.robvanblerk.tieredpower.block.entity.SteamEngineBlockEntity se) data.putInt("tpGen", se.getGenerating());
			if (accessor.getBlockEntity() instanceof com.robvanblerk.tieredpower.block.entity.WindTurbineBlockEntity w) data.putInt("tpGen", w.getGenerating());
			if (accessor.getBlockEntity() instanceof com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity fc && fc.isFormed()) {
				data.putInt("tpGen", fc.getGenerating());
				data.putInt("tpHeat", fc.getHeat());
				data.putBoolean("tpScram", fc.isScrammed());
			}
			if (accessor.getBlockEntity() instanceof com.robvanblerk.tieredpower.block.entity.FissionReactorBlockEntity f) {
				data.putInt("tpGen", f.getGenerating());
				data.putInt("tpHeat", f.getHeat());
				data.putBoolean("tpScram", f.isScrammed());
			}
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();
			if (!data.contains("tpGen")) return;
			tooltip.add(Component.literal(String.format("Making %,d FE/t", data.getInt("tpGen"))).withStyle(ChatFormatting.GREEN));
			if (data.contains("tpHeat")) {
				tooltip.add(Component.literal("Heat " + data.getInt("tpHeat") / 10 + "%" + (data.getBoolean("tpScram") ? " - SCRAM" : ""))
						.withStyle(data.getBoolean("tpScram") ? ChatFormatting.RED : ChatFormatting.GOLD));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return id("generator");
		}
	}

	/**
	 * Status lines for blocks without their own provider: storage, autocrafting, lightning and the simple generators.
	 * The server writes ready-made lines; the client just shows them.
	 */
	enum Info implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendServerData(CompoundTag data, BlockAccessor accessor) {
			java.util.List<String> lines = new java.util.ArrayList<>();
			var be = accessor.getBlockEntity();
			if (be instanceof com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity c) {
				var s = c.stats();
				if (!c.isOnline()) lines.add("!Offline - " + (c.getProblem() == com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity.Problem.TWO_CONTROLLERS ? "two controllers" : "needs power"));
				else {
					lines.add(shortFe(s.used()) + " / " + shortFe(s.capacity()) + " items, " + s.types() + " types");
					if (s.fluidDisks() > 0) lines.add(shortFe(s.fluidUsed() / 1000) + " / " + shortFe(s.fluidCapacity() / 1000) + " buckets of fluid");
					if (!c.getJobs().isEmpty()) lines.add(c.getJobs().size() + " crafting job(s)");
				}
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.DriveBayBlockEntity bay) {
				long used = 0, cap = 0;
				int disks = 0;
				for (var d : bay.disks()) { used += d.used(); cap += d.capacity(); disks++; }
				for (var d : bay.fluidDisks()) { disks++; }
				lines.add(disks + " / 8 disks" + (cap > 0 ? ", items " + (int) Math.round(100.0 * used / cap) + "% full" : ""));
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity a) {
				lines.add(a.patterns().size() + " / 9 patterns, " + a.operationsPerCycle() + " operation(s) per cycle");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.StockKeeperBlockEntity k) {
				int set = 0;
				for (int i = 0; i < k.items().getContainerSize(); i++) if (!k.items().getItem(i).isEmpty()) set++;
				lines.add("Keeping " + set + " item(s) in stock");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.LightningCollectorBlockEntity lc) {
				lines.add(shortFe(lc.energy.getEnergyStored()) + " / " + shortFe(com.robvanblerk.tieredpower.block.entity.LightningCollectorBlockEntity.CAPACITY) + " FE");
				lines.add(lc.getStrikes() + " strike(s) caught");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.StormCallerBlockEntity sc) {
				String[] st = {"Ready", "Charging", "!Needs a Storm Charge", "Storm in progress", "!No weather here", "Off (redstone)"};
				lines.add(st[Math.max(0, Math.min(st.length - 1, sc.getStatus()))]);
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.TritiumBreederBlockEntity tb) {
				lines.add(tb.isReactorRunning() ? "Breeding (reactor running)" : "!Needs a running Fission Reactor next to it");
				lines.add(String.format("%,d mB tritium", tb.getTritium()));
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.CryoInjectorBlockEntity ci) {
				lines.add(ci.isActive() ? "Cooling the reactor: +50% power" : "!Idle - needs nitrogen and a running reactor");
				lines.add(String.format("%,d mB nitrogen", ci.getNitrogen()));
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.AirSeparatorBlockEntity as && !as.hasAirAccess()) {
				lines.add("!Needs a side open to the air");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.PowerTransmitterBlockEntity pt) {
				lines.add(shortFe(pt.energy.getEnergyStored()) + " / " + shortFe(com.robvanblerk.tieredpower.block.entity.PowerTransmitterBlockEntity.CAPACITY) + " FE");
				lines.add(pt.getReceivers() > 0 ? "Sending " + shortFe(pt.getSentPerTick()) + " FE/t to " + pt.getReceivers() + " receiver(s)" : "!No receivers drawing power");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.PowerReceiverBlockEntity pr) {
				lines.add(shortFe(pr.energy.getEnergyStored()) + " / " + shortFe(com.robvanblerk.tieredpower.block.entity.PowerReceiverBlockEntity.CAPACITY) + " FE");
				if (pr.getStatus() == 0) lines.add("!Not linked - use a Power Linker");
				else if (pr.getStatus() == 2) lines.add("!Transmitter missing or its chunk isn't loaded");
				else lines.add("Receiving " + shortFe(pr.getReceived()) + " FE/t from " + pr.getLinkPos().toShortString());
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.AntimatterReactorBlockEntity ar) {
				lines.add(shortFe(ar.energy.getEnergyStored()) + " / " + shortFe(com.robvanblerk.tieredpower.block.entity.AntimatterReactorBlockEntity.CAPACITY) + " FE");
				lines.add(ar.getGenerating() > 0 ? "Making " + shortFe(ar.getGenerating()) + " FE/t" : ar.getAntimatter() > 0 ? "Idle (buffer full)" : "!No antimatter");
				lines.add(String.format("%,d mB antimatter", ar.getAntimatter()));
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.WirelessSenderBlockEntity ws) {
				lines.add(ws.itemCount() + " item(s) waiting");
				if (!ws.tank.isEmpty()) lines.add(String.format("%,d mB %s", ws.tank.getFluidAmount(), ws.tank.getFluid().getDisplayName().getString()));
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.WirelessReceiverBlockEntity wr) {
				if (wr.getStatus() == 0) lines.add("!Not linked - use a Wireless Linker");
				else if (wr.getStatus() == 2) lines.add("!Sender missing or its chunk isn't loaded");
				else lines.add("Linked to the Sender at " + wr.getLinkPos().toShortString());
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.BufferBlockEntity bf) {
				if (bf.isFluid()) lines.add(bf.tank.isEmpty() ? "Empty" : String.format("%,d / %,d mB %s", bf.tank.getFluidAmount(), com.robvanblerk.tieredpower.block.entity.BufferBlockEntity.TANK, bf.tank.getFluid().getDisplayName().getString()));
				else lines.add(bf.itemCount() + " item(s) queued");
			} else if (be instanceof com.robvanblerk.tieredpower.greenhouse.GreenhouseBlockEntity gh) {
				if (gh.isSprinkler()) lines.add(gh.water.isEmpty() ? "!No water - pipe some in" : String.format("%,d mB water", gh.water.getFluidAmount()));
				else lines.add(gh.isRunning() ? "Lit" : "!Needs power");
				if (gh.isRunning()) lines.add("Helping " + gh.getPlants() + " plants grow");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.FluidicPlenisherBlockEntity fp) {
				lines.add(fp.tank.isEmpty() ? "!No fluid - pipe some in" : String.format("%,d mB %s", fp.tank.getFluidAmount(), fp.tank.getFluid().getDisplayName().getString()));
				lines.add(fp.isDone() ? "Finished - nothing left to fill" : fp.getPlaced() + " blocks placed");
			} else if (be instanceof com.robvanblerk.tieredpower.industry.DieselGeneratorBlockEntity dg) {
				lines.add(shortFe(dg.energy.getEnergyStored()) + " / " + shortFe(com.robvanblerk.tieredpower.industry.DieselGeneratorBlockEntity.CAPACITY) + " FE");
				lines.add(dg.tank.isEmpty() ? "!No fuel - Biodiesel, Rocket Fuel or Creosote Oil" : String.format("%,d mB %s", dg.tank.getFluidAmount(), dg.tank.getFluid().getDisplayName().getString()));
				if (dg.getGenerating() > 0) lines.add("Making " + shortFe(dg.getGenerating()) + " FE/t");
			} else if (be instanceof com.robvanblerk.tieredpower.stargate.StargateDialerBlockEntity sd) {
				lines.addAll(sd.info());
			} else if (be instanceof com.robvanblerk.tieredpower.quantum.QuantumChestBlockEntity qc) {
				lines.addAll(qc.info());
			} else if (be instanceof com.robvanblerk.tieredpower.redstone.WirelessRedstoneBlockEntity wr) {
				lines.addAll(wr.info());
			} else if (be instanceof com.robvanblerk.tieredpower.logic.MachineStatusDisplayBlockEntity ms) {
				lines.add(ms.entries().size() + " machines nearby" + (ms.problems() > 0 ? ", " + ms.problems() + " need attention" : ""));
			} else if (be instanceof com.robvanblerk.tieredpower.turbine.TurbineControllerBlockEntity tc) {
				lines.addAll(tc.info());
			} else if (be instanceof com.robvanblerk.tieredpower.drone.DroneStationBlockEntity ds) {
				lines.addAll(ds.info());
			} else if (be instanceof com.robvanblerk.tieredpower.conveyor.ConveyorBlockEntity belt) {
				lines.addAll(belt.info());
			} else if (be instanceof com.robvanblerk.tieredpower.core.EnergyCoreBlockEntity ec) {
				lines.add("Tier " + ec.getTier() + ": " + shortFe(ec.getStored()) + " / " + shortFe(ec.getCapacity()) + " FE");
				lines.add("In " + shortFe(ec.getRateIn()) + " FE/t, out " + shortFe(ec.getRateOut()) + " FE/t");
			} else if (be instanceof com.robvanblerk.tieredpower.core.EnergyPylonBlockEntity py) {
				lines.add(py.core() == null ? "!No Energy Core within 8 blocks" : (py.isInput() ? "Into the core: " : "Out of the core: ") + shortFe(py.getMoved()) + " FE/t");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.GasTankBlockEntity gt) {
				lines.add(gt.tank.isEmpty() ? "Empty" : String.format("%,d / %,d mB %s", gt.tank.getFluidAmount(), com.robvanblerk.tieredpower.block.entity.GasTankBlockEntity.CAPACITY, gt.tank.getFluid().getDisplayName().getString()));
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.DigitalMinerBlockEntity dm) {
				lines.add(dm.getMined() + " ores mined");
				if (dm.getScanY() != Integer.MIN_VALUE) lines.add("Scanning layer Y " + dm.getScanY());
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.LaserDrillBlockEntity ld) {
				lines.add(ld.getSatelliteId() >= 0 ? "Mining Satellite #" + ld.getSatelliteId() : "!No Mining Satellite claimed");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.ReceiverDishBlockEntity rd) {
				lines.add(shortFe(rd.energy.getEnergyStored()) + " / " + shortFe(com.robvanblerk.tieredpower.block.entity.ReceiverDishBlockEntity.CAPACITY) + " FE");
				lines.add(rd.getSatelliteId() >= 0 ? "Solar Satellite #" + rd.getSatelliteId() + ": " + shortFe(rd.getGenerating()) + " FE/t" : "!No satellite - right-click to claim one");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.GeothermalGeneratorBlockEntity g) {
				lines.add(g.getGenerating() > 0 ? "Making " + g.getGenerating() + " FE/t" : "!No lava or magma touching it");
			} else if (be instanceof com.robvanblerk.tieredpower.block.entity.WaterWheelBlockEntity w) {
				lines.add(w.getGenerating() > 0 ? "Making " + w.getGenerating() + " FE/t" : "!No flowing water beside it");
			}
			if (lines.isEmpty()) return;
			net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
			for (String l : lines) list.add(net.minecraft.nbt.StringTag.valueOf(l));
			data.put("tpInfo", list);
		}

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();
			if (!data.contains("tpInfo")) return;
			var list = data.getList("tpInfo", net.minecraft.nbt.Tag.TAG_STRING);
			for (int i = 0; i < list.size(); i++) {
				String l = list.getString(i);
				boolean warn = l.startsWith("!");
				tooltip.add(Component.literal(warn ? l.substring(1) : l).withStyle(warn ? ChatFormatting.RED : ChatFormatting.AQUA));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return id("info");
		}
	}

	private TieredPowerProviders() {}
}
