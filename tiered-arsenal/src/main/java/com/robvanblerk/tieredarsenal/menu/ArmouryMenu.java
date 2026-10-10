package com.robvanblerk.tieredarsenal.menu;

import com.robvanblerk.tieredarsenal.block.entity.ArmouryWorkbenchBlockEntity;
import com.robvanblerk.tieredarsenal.item.ModuleItem;
import com.robvanblerk.tieredarsenal.module.ModuleHelper;
import com.robvanblerk.tieredarsenal.registry.ModBlocks;
import com.robvanblerk.tieredarsenal.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ArmouryMenu extends AbstractContainerMenu {
    /** Button ids: 0 = install, 1..5 = remove the module in that row. */
    public static final int BUTTON_INSTALL = 0;

    private static final int MACHINE_SLOTS = 2;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    public final ArmouryWorkbenchBlockEntity workbench;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side: opened from the server with the block's position. */
    public ArmouryMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory,
                (ArmouryWorkbenchBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(2));
    }

    public ArmouryMenu(int id, Inventory inventory, ArmouryWorkbenchBlockEntity workbench, ContainerData data) {
        super(ModMenus.ARMOURY_WORKBENCH.get(), id);
        this.workbench = workbench;
        this.data = data;
        this.access = ContainerLevelAccess.create(workbench.getLevel(), workbench.getBlockPos());

        addSlot(new SlotItemHandler(workbench.items, ArmouryWorkbenchBlockEntity.GEAR_SLOT, 26, 24));
        addSlot(new SlotItemHandler(workbench.items, ArmouryWorkbenchBlockEntity.MODULE_SLOT, 26, 54));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 162));
        }
        addDataSlots(data);
    }

    public int getEnergy() {
        return (data.get(0) & 0xFFFF) | ((data.get(1) & 0xFFFF) << 16);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide()) return true;
        if (id == BUTTON_INSTALL) {
            workbench.install(player);
        } else {
            workbench.remove(player, id - 1);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (ModuleHelper.getSlots(stack) > 0) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ModuleItem) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ARMOURY_WORKBENCH.get());
    }
}
