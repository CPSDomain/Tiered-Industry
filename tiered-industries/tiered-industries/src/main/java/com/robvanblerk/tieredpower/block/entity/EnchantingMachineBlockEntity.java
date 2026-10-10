package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.EnchantingMachineMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Enchants items with power - or, with Liquid Experience in its tank, on XP at twice the speed. Put an enchantable item (or a book) in the left slot and lapis in the middle;
 * pick level 10, 20 or 30 with the button. Uses 1 lapis per 10 levels. Books become enchanted books.
 * Slots: 0 item, 1 lapis, 2 output, 3-4 upgrades.
 */
public class EnchantingMachineBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 500_000, MAX_INPUT = 8_000;
	public static final int ENERGY_PER_TICK = 400, TICKS_PER_LEVEL = 10;
	public static final int ITEM_SLOT = 0, LAPIS_SLOT = 1, OUTPUT_SLOT = 2;
	public static final int[] LEVELS = {10, 20, 30};

	private int levelIndex = 2;
	private int progress;

	/** Liquid Experience: with enough in the tank it enchants on XP instead of power, twice as fast. */
	public static final int XP_TANK = 16_000, XP_MB_PER_LEVEL = 40;
	public final net.minecraftforge.fluids.capability.templates.FluidTank xp = new net.minecraftforge.fluids.capability.templates.FluidTank(XP_TANK,
			f -> f.getFluid() == com.robvanblerk.tieredpower.registry.ModFluids.EXPERIENCE.get()) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> xpCap = net.minecraftforge.common.util.LazyOptional.of(() -> xp);

	public static int xpCostFor(int level) {
		return level * XP_MB_PER_LEVEL;
	}

	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
		if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) return xpCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); xpCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); xpCap = net.minecraftforge.common.util.LazyOptional.of(() -> xp); }

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> LEVELS[levelIndex] * TICKS_PER_LEVEL;
				case 4 -> levelIndex;
				case 5 -> energyCost(ENERGY_PER_TICK);
				case 6 -> xp.getFluidAmount();
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return EnchantingMachineMenu.DATA_COUNT;
		}
	};

	public EnchantingMachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ENCHANTING_MACHINE.get(), pos, state, 5, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(3);
	}

	public void cycleLevel() {
		levelIndex = (levelIndex + 1) % LEVELS.length;
		progress = 0;
		setChanged();
	}

	public static int lapisFor(int level) {
		return level / 10;
	}

	public static boolean canEnchant(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(Items.BOOK) || (stack.isEnchantable() && !stack.isEnchanted()));
	}

	public static void tick(Level level, BlockPos pos, BlockState state, EnchantingMachineBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		ItemStack item = be.items.get(ITEM_SLOT), lapis = be.items.get(LAPIS_SLOT);
		int enchLevel = LEVELS[be.levelIndex];
		int cost = be.energyCost(ENERGY_PER_TICK);
		boolean ready = canEnchant(item) && lapis.is(Items.LAPIS_LAZULI) && lapis.getCount() >= lapisFor(enchLevel) && be.items.get(OUTPUT_SLOT).isEmpty();
		boolean onXp = ready && be.xp.getFluidAmount() >= xpCostFor(enchLevel); // XP mode: no power, twice as fast
		boolean working = ready && (onXp || be.energy.getEnergyStored() >= cost);
		if (working) {
			if (!onXp) be.energy.removeInternal(cost);
			be.progress += be.progressStep() * (onXp ? 2 : 1);
			if (be.progress >= enchLevel * TICKS_PER_LEVEL * 100) {
				be.progress = 0;
				if (onXp) be.xp.drain(xpCostFor(enchLevel), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
				ItemStack result = EnchantmentHelper.enchantItem(level.random, item.copyWithCount(1), enchLevel, false);
				item.shrink(1);
				lapis.shrink(lapisFor(enchLevel));
				be.items.set(OUTPUT_SLOT, result);
			}
			be.setChanged();
		} else if (!canEnchant(item) && be.progress != 0) {
			be.progress = 0;
		}
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("levelIndex", levelIndex);
		tag.putInt("progress", progress);
		tag.put("xp", xp.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		levelIndex = tag.contains("levelIndex") ? Math.floorMod(tag.getInt("levelIndex"), LEVELS.length) : 2;
		progress = tag.getInt("progress");
		xp.readFromNBT(tag.getCompound("xp"));
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{ITEM_SLOT, LAPIS_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == ITEM_SLOT) return canEnchant(stack);
		if (slot == LAPIS_SLOT) return stack.is(Items.LAPIS_LAZULI);
		return false;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.enchanting_machine");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new EnchantingMachineMenu(containerId, inventory, this, data);
	}
}
