package com.robvanblerk.tieredpower.block.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.MobGrinderMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Attacks mobs in a 5x3x5 area in front of it, as if a player hit them (so they drop player-only loot and XP).
 * Picks up the drops into its buffer and stores the XP - right-click it with an empty hand to take the XP.
 * Never touches players, bosses, name-tagged mobs, tamed pets or armour stands. Button: hostile only / all mobs.
 * Slots: 0-8 buffer, 9-10 upgrades.
 */
public class MobGrinderBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 200_000, MAX_INPUT = 8_000;
	public static final int ENERGY_PER_TICK = 40, ATTACK_TICKS = 20, DAMAGE = 20, BUFFER = 9;

	private boolean allMobs;
	private int progress;
	private int storedXp;

	/** The stored XP, drained from pipes as Liquid Experience (20 mB a point). Fill-only from the grinder itself. */
	private final net.minecraftforge.fluids.capability.IFluidHandler xpOut = new net.minecraftforge.fluids.capability.IFluidHandler() {
		private final int mb = com.robvanblerk.tieredpower.registry.ModFluids.MB_PER_XP;
		@Override public int getTanks() { return 1; }
		@Override public net.minecraftforge.fluids.FluidStack getFluidInTank(int t) {
			return storedXp > 0 ? new net.minecraftforge.fluids.FluidStack(com.robvanblerk.tieredpower.registry.ModFluids.EXPERIENCE.get(), (int) Math.min(Integer.MAX_VALUE, (long) storedXp * mb)) : net.minecraftforge.fluids.FluidStack.EMPTY;
		}
		@Override public int getTankCapacity(int t) { return Integer.MAX_VALUE; }
		@Override public boolean isFluidValid(int t, net.minecraftforge.fluids.FluidStack s) { return false; }
		@Override public int fill(net.minecraftforge.fluids.FluidStack r, FluidAction a) { return 0; }
		@Override public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack r, FluidAction a) {
			return r.getFluid() == com.robvanblerk.tieredpower.registry.ModFluids.EXPERIENCE.get() ? drain(r.getAmount(), a) : net.minecraftforge.fluids.FluidStack.EMPTY;
		}
		@Override public net.minecraftforge.fluids.FluidStack drain(int max, FluidAction a) {
			int points = Math.min(storedXp, max / mb); // whole points only
			if (points <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
			if (a.execute()) { storedXp -= points; setChanged(); }
			return new net.minecraftforge.fluids.FluidStack(com.robvanblerk.tieredpower.registry.ModFluids.EXPERIENCE.get(), points * mb);
		}
	};
	private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> xpCap = net.minecraftforge.common.util.LazyOptional.of(() -> xpOut);

	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
		if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) return xpCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); xpCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); xpCap = net.minecraftforge.common.util.LazyOptional.of(() -> xpOut); }

	/** Pushes stored XP as Liquid Experience into tanks and machines next to it (whole points only). */
	private void pushXp(Level level) {
		int mb = com.robvanblerk.tieredpower.registry.ModFluids.MB_PER_XP;
		for (Direction d : Direction.values()) {
			if (storedXp <= 0) return;
			var n = level.getBlockEntity(worldPosition.relative(d));
			if (n == null) continue;
			var h = n.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, d.getOpposite()).orElse(null);
			if (h == null) continue;
			int offer = (int) Math.min(64_000, (long) storedXp * mb);
			int fits = h.fill(new net.minecraftforge.fluids.FluidStack(com.robvanblerk.tieredpower.registry.ModFluids.EXPERIENCE.get(), offer), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) / mb;
			if (fits <= 0) continue;
			h.fill(new net.minecraftforge.fluids.FluidStack(com.robvanblerk.tieredpower.registry.ModFluids.EXPERIENCE.get(), fits * mb), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
			storedXp -= fits;
			setChanged();
		}
	}
	private int kills;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> allMobs ? 1 : 0;
				case 3 -> storedXp & 0xFFFF;
				case 4 -> (storedXp >>> 16) & 0xFFFF;
				case 5 -> kills & 0xFFFF;
				case 6 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return MobGrinderMenu.DATA_COUNT;
		}
	};

	public MobGrinderBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MOB_GRINDER.get(), pos, state, BUFFER + 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(BUFFER);
	}

	public void toggleMode() {
		allMobs = !allMobs;
		setChanged();
	}

	/** Gives the player all the stored XP; returns how much. */
	public int giveXp(Player player) {
		int xp = storedXp;
		if (xp > 0) {
			player.giveExperiencePoints(xp);
			storedXp = 0;
			setChanged();
		}
		return xp;
	}

	private AABB area(BlockState state) {
		Direction front = state.getValue(MachineBlock.FACING);
		BlockPos centre = worldPosition.relative(front, 3);
		return new AABB(centre).inflate(2, 1, 2);
	}

	private boolean isTarget(LivingEntity e) {
		if (!e.isAlive() || e instanceof Player || e instanceof ArmorStand || e.hasCustomName()) return false;
		if (e.getType().is(Tags.EntityTypes.BOSSES)) return false;
		if (e instanceof TamableAnimal t && t.isTame()) return false;
		return allMobs || e instanceof Enemy;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MobGrinderBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		if (be.preTick(level, pos, state)) return;
		AABB area = be.area(state);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, be::isTarget);
		boolean hasRoom = be.items.subList(0, BUFFER).stream().anyMatch(ItemStack::isEmpty);
		int cost = be.energyCost(ENERGY_PER_TICK);
		boolean working = !targets.isEmpty() && hasRoom && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= ATTACK_TICKS * 100) {
				be.progress = 0;
				FakePlayer fake = FakePlayerFactory.getMinecraft(server);
				for (LivingEntity target : targets) {
					boolean wasAlive = target.isAlive();
					target.hurt(level.damageSources().playerAttack(fake), DAMAGE);
					if (wasAlive && !target.isAlive()) be.kills++;
				}
			}
			be.setChanged();
		}
		// Collect drops and XP in the area (and a block around it).
		if (level.getGameTime() % 5 == 0) {
			AABB pickup = area.inflate(1);
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, pickup)) {
				ItemStack rest = be.store(item.getItem().copy());
				if (rest.isEmpty()) item.discard();
				else item.setItem(rest);
			}
			for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, pickup)) {
				be.storedXp += orb.getValue();
				orb.discard();
			}
			be.pushXp(level);
		}
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	private ItemStack store(ItemStack stack) {
		for (int i = 0; i < BUFFER && !stack.isEmpty(); i++) {
			ItemStack slot = items.get(i);
			if (slot.isEmpty()) {
				items.set(i, stack);
				return ItemStack.EMPTY;
			}
			if (ItemStack.isSameItemSameTags(slot, stack)) {
				int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(move);
				stack.shrink(move);
			}
		}
		setChanged();
		return stack;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("allMobs", allMobs);
		tag.putInt("xp", storedXp);
		tag.putInt("kills", kills);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		allMobs = tag.getBoolean("allMobs");
		storedXp = tag.getInt("xp");
		kills = tag.getInt("kills");
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot < BUFFER;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.mob_grinder");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new MobGrinderMenu(containerId, inventory, this, data);
	}
}
