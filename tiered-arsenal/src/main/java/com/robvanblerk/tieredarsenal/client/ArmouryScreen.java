package com.robvanblerk.tieredarsenal.client;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.block.entity.ArmouryWorkbenchBlockEntity;
import com.robvanblerk.tieredarsenal.menu.ArmouryMenu;
import com.robvanblerk.tieredarsenal.module.GearModule;
import com.robvanblerk.tieredarsenal.module.ModuleHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.List;

public class ArmouryScreen extends AbstractContainerScreen<ArmouryMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TieredArsenal.MOD_ID, "textures/gui/armoury_workbench.png");
    private static final String KEY = "gui." + TieredArsenal.MOD_ID + ".armoury.";

    // Layout (matches the texture)
    private static final int LIST_X = 68, LIST_Y = 20, ROW_H = 14, MAX_ROWS = 5;
    private static final int BAR_X = 161, BAR_Y = 18, BAR_W = 6, BAR_H = 70;

    private Button installButton;
    private final Button[] removeButtons = new Button[MAX_ROWS];

    public ArmouryScreen(ArmouryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        String cost = NumberFormat.getIntegerInstance().format(ArmouryWorkbenchBlockEntity.INSTALL_COST);
        installButton = addRenderableWidget(Button.builder(Component.translatable(KEY + "install"),
                        b -> press(ArmouryMenu.BUTTON_INSTALL))
                .bounds(leftPos + 7, topPos + 74, 54, 16)
                .tooltip(Tooltip.create(Component.translatable(KEY + "install_tooltip", cost)))
                .build());
        for (int i = 0; i < MAX_ROWS; i++) {
            final int row = i;
            removeButtons[i] = addRenderableWidget(Button.builder(Component.literal("x"), b -> press(1 + row))
                    .bounds(leftPos + 142, topPos + LIST_Y - 3 + i * ROW_H, 12, 12)
                    .tooltip(Tooltip.create(Component.translatable(KEY + "remove_tooltip")))
                    .build());
        }
        updateButtons();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        if (installButton == null) return;
        ItemStack gear = menu.getSlot(ArmouryWorkbenchBlockEntity.GEAR_SLOT).getItem();
        int fitted = ModuleHelper.getModules(gear).size();
        installButton.active = !gear.isEmpty() && menu.getSlot(ArmouryWorkbenchBlockEntity.MODULE_SLOT).hasItem();
        for (int i = 0; i < MAX_ROWS; i++) {
            removeButtons[i].visible = i < fitted;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int energy = menu.getEnergy();
        int h = (int) ((long) BAR_H * energy / ArmouryWorkbenchBlockEntity.CAPACITY);
        if (h > 0) {
            g.fill(leftPos + BAR_X, topPos + BAR_Y + BAR_H - h, leftPos + BAR_X + BAR_W, topPos + BAR_Y + BAR_H,
                    0xFFD63A2E);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        ItemStack gear = menu.getSlot(ArmouryWorkbenchBlockEntity.GEAR_SLOT).getItem();
        if (gear.isEmpty()) {
            g.drawString(font, Component.translatable(KEY + "insert_gear"), LIST_X, LIST_Y, 0xE0E0E0, false);
            return;
        }
        int slots = ModuleHelper.getSlots(gear);
        List<GearModule> fitted = ModuleHelper.getModules(gear);
        Component count = Component.translatable(KEY + "slots", fitted.size(), slots);
        g.drawString(font, count, imageWidth - 8 - font.width(count), titleLabelY, 0x404040, false);

        for (int i = 0; i < Math.min(slots, MAX_ROWS); i++) {
            Component line = i < fitted.size()
                    ? Component.translatable("module." + TieredArsenal.MOD_ID + "." + fitted.get(i).id())
                    : Component.translatable(KEY + "empty_slot");
            int colour = i < fitted.size() ? 0xFFFFFF : 0xA0A0A0;
            g.drawString(font, line, LIST_X, LIST_Y + i * ROW_H, colour, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            NumberFormat nf = NumberFormat.getIntegerInstance();
            g.renderTooltip(font, Component.literal(nf.format(menu.getEnergy()) + " / "
                    + nf.format(ArmouryWorkbenchBlockEntity.CAPACITY) + " FE"), mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
