package com.robvanblerk.tieredpower.industry;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Turns crops into Biodiesel. Seeds are pressed into plant oil (sunflowers give the most); sugary crops are fermented
 * into ethanol; the two are blended 1:1 into Biodiesel, which is pushed out into Fluid Pipes. 20 FE/t while working.
 * Slots: 0 input, 1-2 upgrades.
 */
public class BioRefineryBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 50_000, MAX_INPUT = 1_000, ENERGY_PER_TICK = 20, TANK = 8_000, TIME = 20;
	private int oil, ethanol, biodiesel, progress;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> oil;
				case 3 -> ethanol;
				case 4 -> biodiesel;
				case 5 -> progress;
				case 6 -> TIME * 100;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return BioRefineryMenu.DATA_COUNT; }
	};

	public BioRefineryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BIO_REFINERY.get(), pos, state, 3, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(1);
	}

	/** mB of plant oil from one item, or 0. */
	public static int oilFrom(ItemStack s) {
		if (s.is(Items.SUNFLOWER)) return 200;
		if (s.is(Tags.Items.SEEDS)) return 50;
		return 0;
	}

	/** mB of ethanol from one item, or 0. */
	public static int ethanolFrom(ItemStack s) {
		if (s.is(Items.HONEY_BOTTLE)) return 150;
		if (s.is(Items.SUGAR)) return 100;
		if (s.is(Items.SUGAR_CANE) || s.is(Items.PUMPKIN)) return 80;
		if (s.is(Items.APPLE)) return 60;
		if (s.is(Items.WHEAT) || s.is(Items.POTATO)) return 50;
		if (s.is(Items.CARROT) || s.is(Items.BEETROOT)) return 40;
		if (s.is(Items.SWEET_BERRIES) || s.is(Items.GLOW_BERRIES)) return 30;
		if (s.is(Items.MELON_SLICE)) return 25;
		return 0;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BioRefineryBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		boolean working = false;
		ItemStack in = be.items.get(0);
		int oil = oilFrom(in), eth = ethanolFrom(in);
		int cost = be.energyCost(ENERGY_PER_TICK);
		boolean room = !in.isEmpty() && (oil > 0 && be.oil + oil <= TANK || eth > 0 && be.ethanol + eth <= TANK);
		if (room && be.energy.getEnergyStored() >= cost) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			working = true;
			if (be.progress >= TIME * 100) {
				be.progress = 0;
				be.oil += oil;
				be.ethanol += eth;
				in.shrink(1);
			}
		} else if (!room) be.progress = 0;
		// blend 1:1 into biodiesel
		int blend = Math.min(Math.min(be.oil, be.ethanol), Math.min(20, (TANK - be.biodiesel) / 2));
		if (blend > 0) {
			be.oil -= blend;
			be.ethanol -= blend;
			be.biodiesel += blend * 2;
			working = true;
		}
		if (be.biodiesel > 0) be.biodiesel = com.robvanblerk.tieredpower.block.entity.PushHelper.pushFluid(level, pos, ModFluids.BIODIESEL.get(), be.biodiesel);
		be.setChanged();
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && (oilFrom(stack) > 0 || ethanolFrom(stack) > 0); }
	@Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("oil", oil);
		tag.putInt("ethanol", ethanol);
		tag.putInt("biodiesel", biodiesel);
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		oil = tag.getInt("oil");
		ethanol = tag.getInt("ethanol");
		biodiesel = tag.getInt("biodiesel");
		progress = tag.getInt("progress");
	}

	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.bio_refinery"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new BioRefineryMenu(id, inv, this, data);
	}
}
