package com.robvanblerk.tieredpower.block.entity;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;

import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.energy.RedstoneMode;
import com.robvanblerk.tieredpower.block.MachineBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.util.ImplementedContainer;

/**
 * Common base for our machines: an item inventory plus an energy buffer, both exposed to other mods
 * through Forge capabilities (so pipes, hoppers and cables from any mod can interact with them).
 */
public abstract class MachineBlockEntity extends BlockEntity implements ImplementedContainer, WorldlyContainer, MenuProvider {
	protected final NonNullList<ItemStack> items;
	public final ModEnergyStorage energy;

	private LazyOptional<IEnergyStorage> energyCap;

	/** Index of the Speed upgrade slot (Efficiency is the next one), or -1 if this machine takes no upgrades. */
	private int upgradeSlot = -1;
	public static final int MAX_UPGRADES = 8;

	private RedstoneMode redstoneMode = RedstoneMode.ALWAYS;

	private boolean autoOutput;
	private final int[] sideModes = {3, 3, 3, 3, 3, 3};

	/** Item mode for an absolute side (see SideConfig). Machines without a facing treat every side as In+Out. */
	public int sideMode(Direction dir) {
		BlockState state = getBlockState();
		if (!state.hasProperty(MachineBlock.FACING)) return com.robvanblerk.tieredpower.energy.SideConfig.BOTH;
		if (dir == Direction.DOWN) return com.robvanblerk.tieredpower.energy.SideConfig.OFF; // the bottom is the power face
		return sideModes[com.robvanblerk.tieredpower.energy.SideConfig.faceIndex(state.getValue(MachineBlock.FACING), dir)];
	}

	public int getRelativeMode(int face) {
		return sideModes[face];
	}

	public void cycleSide(int face) {
		if (face < 0 || face >= 6) return;
		sideModes[face] = (sideModes[face] + 1) % 4;
		setChanged();
		invalidateCaps();
		reviveCaps();
		if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock()); // pipes reconnect
	}

	/**
	 * Item access for one side. The side's setting decides the direction: In faces accept items into the machine's input
	 * slots and Out faces give up its output slots, on any face. (Which slots are inputs and outputs comes from the
	 * machine's hopper rules: outputs are what a hopper could pull from the bottom, inputs what could go in elsewhere.)
	 */
	private class SideLimitedHandler implements net.minecraftforge.items.IItemHandler {
		private final Direction side;
		private final boolean anySide;
		private final net.minecraftforge.items.wrapper.InvWrapper inner = new net.minecraftforge.items.wrapper.InvWrapper(MachineBlockEntity.this);

		SideLimitedHandler(Direction side) {
			this(side, false);
		}

		SideLimitedHandler(Direction side, boolean anySide) {
			this.side = side;
			this.anySide = anySide;
		}

		private int mode() {
			return anySide ? com.robvanblerk.tieredpower.energy.SideConfig.BOTH : sideMode(side);
		}

		private boolean isInput(int slot, ItemStack stack) {
			for (Direction d : Direction.values()) {
				if (d == Direction.DOWN) continue;
				for (int s : getSlotsForFace(d)) if (s == slot && canPlaceItemThroughFace(slot, stack, d)) return true;
			}
			return false;
		}

		private boolean isOutput(int slot, ItemStack stack) {
			for (Direction d : Direction.values()) {
				for (int s : getSlotsForFace(d)) if (s == slot && canTakeItemThroughFace(slot, stack, d)) return true;
			}
			return false;
		}

		@Override
		public int getSlots() {
			return mode() == com.robvanblerk.tieredpower.energy.SideConfig.OFF ? 0 : inner.getSlots();
		}

		@Override
		public @NotNull ItemStack getStackInSlot(int slot) {
			return mode() == com.robvanblerk.tieredpower.energy.SideConfig.OFF ? ItemStack.EMPTY : inner.getStackInSlot(slot);
		}

		@Override
		public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
			if (!com.robvanblerk.tieredpower.energy.SideConfig.canInput(mode()) || stack.isEmpty() || !isInput(slot, stack)
					|| !acceptsAutomatedInput(slot, stack)) return stack;
			return inner.insertItem(slot, stack, simulate);
		}

		@Override
		public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (!com.robvanblerk.tieredpower.energy.SideConfig.canOutput(mode())) return ItemStack.EMPTY;
			ItemStack in = inner.getStackInSlot(slot);
			if (in.isEmpty() || !isOutput(slot, in)) return ItemStack.EMPTY;
			return inner.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return inner.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return com.robvanblerk.tieredpower.energy.SideConfig.canInput(mode()) && isInput(slot, stack) && acceptsAutomatedInput(slot, stack);
		}
	}

	/**
	 * Extra rule for items arriving from pipes, buses and hoppers (not the player). Machines with several input slots
	 * override it so automation fills each slot with the right ingredient instead of one item taking every slot.
	 */
	protected boolean acceptsAutomatedInput(int slot, ItemStack stack) {
		return true;
	}

	public boolean isAutoOutput() {
		return autoOutput;
	}

	public void toggleAutoOutput() {
		autoOutput = !autoOutput;
		setChanged();
	}

	/**
	 * Call at the start of every machine tick: pushes finished items out (if auto-output is on) and returns true
	 * if redstone control says the machine should be paused.
	 */
	public boolean preTick(Level level, BlockPos pos, BlockState state) {
		// Tiers, Speed upgrades and overclocking raise what the machine uses per tick, so let it take power in faster too.
		if (baseMaxReceive > 0) {
			int want = (int) Math.min(Integer.MAX_VALUE, (long) baseMaxReceive * lanes() * (1 + speedUpgrades() + overclockLevels()));
			if (energy.getMaxReceive() != want) energy.setMaxReceive(want);
			if (energy.getMaxEnergyStored() < want * 2) energy.setCapacity(want * 2);
		}
		if (autoOutput && level.getGameTime() % 10 == 0) pushOutputs(level, pos);
		return redstoneBlocked(level, pos, state);
	}

	/** Output slots are the ones hoppers can pull from the bottom. Push them into any neighbouring inventory or pipe. */
	private void pushOutputs(Level level, BlockPos pos) {
		for (int slot : getSlotsForFace(net.minecraft.core.Direction.DOWN)) {
			ItemStack stack = items.get(slot);
			if (stack.isEmpty() || !canTakeItemThroughFace(slot, stack, net.minecraft.core.Direction.DOWN)) continue;
			ItemStack rest = stack.copy();
			for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
				if (rest.isEmpty()) break;
				if (!com.robvanblerk.tieredpower.energy.SideConfig.canOutput(sideMode(dir))) continue;
				net.minecraftforge.items.IItemHandler target = com.robvanblerk.tieredpower.util.ItemUtil.neighbour(level, pos, dir);
				if (target != null) rest = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(target, rest, false);
			}
			if (rest.getCount() != stack.getCount()) {
				items.set(slot, rest);
				setChanged();
			}
		}
	}

	public RedstoneMode getRedstoneMode() {
		return redstoneMode;
	}

	public void cycleRedstoneMode() {
		redstoneMode = redstoneMode.next();
		setChanged();
	}

	/**
	 * Call at the start of a machine's tick. Returns true (and turns the machine's light off) if redstone
	 * says the machine should be paused right now.
	 */
	public boolean redstoneBlocked(Level level, BlockPos pos, BlockState state) {
		if (redstoneMode.allows(level.hasNeighborSignal(pos))) return false;
		if (state.hasProperty(MachineBlock.LIT) && state.getValue(MachineBlock.LIT)) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, false), Block.UPDATE_ALL);
		}
		return true;
	}
	private final Map<Direction, LazyOptional<IItemHandler>> itemCaps = new EnumMap<>(Direction.class);
	private final int baseMaxReceive;

	protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots,
			int capacity, int maxReceive, int maxExtract) {
		super(type, pos, state);
		this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
		this.energy = new ModEnergyStorage(capacity, maxReceive, maxExtract, this::setChanged);
		this.baseMaxReceive = maxReceive;
		this.energyCap = LazyOptional.of(() -> energy);
		for (Direction dir : Direction.values()) {
			itemCaps.put(dir, LazyOptional.of(() -> new SideLimitedHandler(dir)));
		}
	}

	/** Call from a machine's constructor; the two slots from firstSlot onwards become Speed and Efficiency slots. */
	protected void enableUpgrades(int firstSlot) {
		this.upgradeSlot = firstSlot;
	}

	public int speedUpgrades() {
		if (upgradeSlot < 0) return 0;
		ItemStack s = items.get(upgradeSlot);
		if (s.is(ModBlocks.SPEED_UPGRADE_2.get())) return 2 * Math.min(MAX_UPGRADES, s.getCount()); // Mk II counts double
		return s.is(ModBlocks.SPEED_UPGRADE.get()) ? Math.min(MAX_UPGRADES, s.getCount()) : 0;
	}

	public int efficiencyUpgrades() {
		if (upgradeSlot < 0) return 0;
		ItemStack s = items.get(upgradeSlot + 1);
		if (s.is(ModBlocks.EFFICIENCY_UPGRADE_2.get())) return 2 * Math.min(MAX_UPGRADES, s.getCount());
		return s.is(ModBlocks.EFFICIENCY_UPGRADE.get()) ? Math.min(MAX_UPGRADES, s.getCount()) : 0;
	}

	/** Progress added per tick, in hundredths of a tick: 100 normally, +50 per Speed upgrade. */
	// ---- Tiers (Tier Installers) and the Muffler ----

	/** 0 = Basic, then Advanced, Elite, Ultimate, Quantum, Naquadah (planet tier). */
	public static final String[] TIER_NAMES = {"Basic", "Advanced", "Elite", "Ultimate", "Quantum", "Naquadah"};
	public static final int[] TIER_LANES = {1, 3, 5, 7, 9, 12};
	private int tier;
	private boolean muffled;

	/** Items fitted to the machine (beyond upgrades and the muffler) that should drop when it's broken. */
	public java.util.List<ItemStack> installedDrops() {
		return java.util.List.of();
	}

	/**
	 * Item access for a touching Molecular Assembler: the same slot rules as pipes (ingredients into input slots,
	 * results out of output slots) but ignoring the Sides window, so an assembler works from any face.
	 */
	public net.minecraftforge.items.IItemHandler automationHandler() {
		return new SideLimitedHandler(Direction.UP, true);
	}

	/** Machines that can be upgraded with Tier Installers override this. */
	public boolean supportsTiers() {
		return false;
	}

	public int getTier() {
		return tier;
	}

	public void setTier(int tier) {
		this.tier = Math.max(0, Math.min(TIER_LANES.length - 1, tier));
		setChanged();
		syncToClient();
	}

	/** What a tier gives this machine, for messages and Jade ("5 at once"; the Chunk Loader says its area). */
	public String tierEffect(int tier) {
		return TIER_LANES[tier] + " at once";
	}

	/** How many operations this machine runs side by side (1, or 3/5/7/9 once upgraded). */
	public int lanes() {
		return supportsTiers() ? TIER_LANES[tier] : 1;
	}

	public boolean isMuffled() {
		return muffled;
	}

	public void setMuffled(boolean muffled) {
		this.muffled = muffled;
		setChanged();
		syncToClient();
	}

	/** How many copies of 'result' still fit in this output slot (a big number when nothing comes out). */
	public static int roomFor(ItemStack slot, ItemStack result) {
		if (result.isEmpty()) return Integer.MAX_VALUE;
		if (slot.isEmpty()) return result.getMaxStackSize() / Math.max(1, result.getCount());
		if (!ItemStack.isSameItemSameTags(slot, result)) return 0;
		return (slot.getMaxStackSize() - slot.getCount()) / Math.max(1, result.getCount());
	}

	private void syncToClient() {
		if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		tag.putInt("Tier", tier);
		tag.putBoolean("Muffled", muffled);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag) {
		tier = tag.getInt("Tier"); // only what the client needs - not the inventory or energy
		muffled = tag.getBoolean("Muffled");
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection connection, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
		CompoundTag tag = packet.getTag();
		if (tag != null) handleUpdateTag(tag);
	}

	public int progressStep() {
		return 100 + 50 * (speedUpgrades() + overclockLevels());
	}

	// ---- Overclock (from an Assembly Matrix with Overclock Accelerators, while it's crafting with this machine) ----
	private int overclock;
	private long overclockUntil;

	/** Counts like this many extra Speed upgrades until the given game time. */
	public void overclock(int levels, long until) {
		overclock = levels;
		overclockUntil = until;
	}

	public int overclockLevels() {
		return level != null && level.getGameTime() < overclockUntil ? overclock : 0;
	}

	/** Energy used per tick: +75% per Speed upgrade, then -15% (compounding) per Efficiency upgrade. */
	/** 0.85 per Energy level, but never below 15% - so even 16 Mk II levels can't make machines nearly free. */
	public static double efficiencyFactor(int levels) {
		return Math.max(0.15, Math.pow(0.85, levels));
	}

	public int energyCost(int base) {
		double cost = base * (1 + 0.75 * (speedUpgrades() + overclockLevels())) * efficiencyFactor(efficiencyUpgrades());
		return Math.max(1, com.robvanblerk.tieredpower.Config.use((int) Math.ceil(cost)));
	}

	/** True if result can be added to the stack in an output slot. */
	public static boolean canMerge(ItemStack output, ItemStack result) {
		if (output.isEmpty()) return true;
		if (!ItemStack.isSameItemSameTags(output, result)) return false;
		return output.getCount() + result.getCount() <= output.getMaxStackSize();
	}

	/** Adds a copy of result into the given slot. Call canMerge first. */
	public static void merge(NonNullList<ItemStack> items, int slot, ItemStack result) {
		ItemStack output = items.get(slot);
		if (output.isEmpty()) items.set(slot, result.copy());
		else output.grow(result.getCount());
	}

	@Override
	public NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY && exposesEnergy() && (side == null || side == Direction.DOWN || !powerFromBottomOnly())) return energyCap.cast();
		if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) return itemCaps.get(side).cast();
		return super.getCapability(cap, side);
	}

	/** Machines that don't use power (like the Boiler) return false so cables don't connect to them. */
	/**
	 * Machines that use power take it only through the bottom (so the other faces are free for items), unless the
	 * config says otherwise. Generators and machines that need their bottom (Pump, Quarry) override this.
	 */
	protected boolean powerFromBottomOnly() {
		return energy.canReceive() && !energy.canExtract() && getBlockState().hasProperty(MachineBlock.FACING)
				&& com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.POWER_FROM_BOTTOM);
	}

	protected boolean exposesEnergy() {
		return true;
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energyCap.invalidate();
		itemCaps.values().forEach(LazyOptional::invalidate);
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		energyCap = LazyOptional.of(() -> energy);
		for (Direction dir : Direction.values()) {
			itemCaps.put(dir, LazyOptional.of(() -> new SideLimitedHandler(dir)));
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("Tier", tier);
		tag.putBoolean("Muffled", muffled);
		ContainerHelper.saveAllItems(tag, items);
		tag.putInt("energy", energy.getEnergyStored());
		tag.putInt("redstone", redstoneMode.ordinal());
		tag.putBoolean("autoOutput", autoOutput);
		tag.putIntArray("sides", sideModes);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		tier = tag.getInt("Tier");
		muffled = tag.getBoolean("Muffled");
		items.clear();
		ContainerHelper.loadAllItems(tag, items);
		energy.setEnergy(tag.getInt("energy"));
		redstoneMode = RedstoneMode.byId(tag.getInt("redstone"));
		autoOutput = tag.getBoolean("autoOutput");
		int[] sides = tag.getIntArray("sides");
		for (int i = 0; i < 6 && i < sides.length; i++) sideModes[i] = sides[i] & 3;
	}

	@Override
	public boolean stillValid(Player player) {
		// Same rule as vanilla furnaces: block still exists and player is within 8 blocks.
		return level != null && level.getBlockEntity(worldPosition) == this
				&& player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
	}
}
