package com.robvanblerk.tieredarsenal.block.entity;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.item.ModuleItem;
import com.robvanblerk.tieredarsenal.menu.ArmouryMenu;
import com.robvanblerk.tieredarsenal.module.GearModule;
import com.robvanblerk.tieredarsenal.module.ModuleHelper;
import com.robvanblerk.tieredarsenal.registry.ModBlockEntities;
import com.robvanblerk.tieredarsenal.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArmouryWorkbenchBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 2_000;
    public static final int INSTALL_COST = 10_000;

    public static final int GEAR_SLOT = 0;
    public static final int MODULE_SLOT = 1;

    public final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == GEAR_SLOT ? ModuleHelper.getSlots(stack) > 0 : stack.getItem() instanceof ModuleItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == GEAR_SLOT ? 1 : 64;
        }
    };

    /** Receives up to 2,000 FE/t; nothing can pull power out. */
    private final WorkbenchEnergy energy = new WorkbenchEnergy();
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);

    /** Energy split into two 16-bit halves, because menus sync data as shorts. */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int e = energy.getEnergyStored();
            return index == 0 ? e & 0xFFFF : (e >>> 16) & 0xFFFF;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ArmouryWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARMOURY_WORKBENCH.get(), pos, state);
    }

    // ---- Module fitting (called from the menu's buttons, server side) ----

    public void install(Player player) {
        ItemStack gear = items.getStackInSlot(GEAR_SLOT);
        ItemStack moduleStack = items.getStackInSlot(MODULE_SLOT);
        if (gear.isEmpty() || !(moduleStack.getItem() instanceof ModuleItem moduleItem)) return;
        GearModule module = moduleItem.getModule();

        String fail = null;
        if (!module.canFit(gear)) fail = "doesnt_fit";
        else if (ModuleHelper.has(gear, module)) fail = "already_fitted";
        else if (ModuleHelper.getModules(gear).size() >= ModuleHelper.getSlots(gear)) fail = "no_slot";
        else if (energy.getEnergyStored() < INSTALL_COST) fail = "no_energy";
        if (fail != null) {
            player.displayClientMessage(Component.translatable("message." + TieredArsenal.MOD_ID + "." + fail), true);
            return;
        }

        ItemStack upgraded = gear.copy();
        ModuleHelper.add(upgraded, module);
        items.setStackInSlot(GEAR_SLOT, upgraded);
        moduleStack.shrink(1);
        items.setStackInSlot(MODULE_SLOT, moduleStack);
        energy.consume(INSTALL_COST);
        setChanged();
    }

    public void remove(Player player, int index) {
        ItemStack gear = items.getStackInSlot(GEAR_SLOT);
        List<GearModule> fitted = ModuleHelper.getModules(gear);
        if (gear.isEmpty() || index < 0 || index >= fitted.size()) return;

        ItemStack changed = gear.copy();
        GearModule removed = ModuleHelper.removeAt(changed, index);
        if (removed == null) return;
        items.setStackInSlot(GEAR_SLOT, changed);

        // Give the module back: into the module slot if it fits there, otherwise to the player.
        ItemStack back = new ItemStack(ModItems.MODULES.get(removed).get());
        ItemStack leftover = items.insertItem(MODULE_SLOT, back, false);
        if (!leftover.isEmpty()) {
            player.getInventory().placeItemBackInInventory(leftover);
        }
        setChanged();
    }

    public int getEnergy() {
        return energy.getEnergyStored();
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    items.getStackInSlot(i));
        }
    }

    // ---- Menu ----

    @Override
    public Component getDisplayName() {
        return Component.translatable("block." + TieredArsenal.MOD_ID + ".armoury_workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ArmouryMenu(id, inventory, this, data);
    }

    // ---- Power: bottom face only ----

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && (side == null || side == Direction.DOWN)) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> energy);
    }

    // ---- Saving ----

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.put("Energy", energy.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        if (tag.contains("Energy")) {
            energy.deserializeNBT(tag.get("Energy"));
        }
    }

    private class WorkbenchEnergy extends EnergyStorage {
        WorkbenchEnergy() {
            super(CAPACITY, MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) setChanged();
            return received;
        }

        void consume(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }
    }
}
