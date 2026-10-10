package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A crossbow per tier: more durability and extra arrow damage. The damage bonus is added when
 * the arrow is fired (event/WeaponEvents.java), since the vanilla crossbow has no hook for it.
 */
public class TieredCrossbowItem extends CrossbowItem {
    private final String material;
    private final float arrowBonus;
    private final Supplier<Ingredient> repair;

    public TieredCrossbowItem(String material, float arrowBonus, Supplier<Ingredient> repair, Properties properties) {
        super(properties);
        this.material = material;
        this.arrowBonus = arrowBonus;
        this.repair = repair;
    }

    public String getMaterial() {
        return material;
    }

    public float getArrowBonus() {
        return arrowBonus;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairItem) {
        return repair.get().test(repairItem);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (arrowBonus > 0) {
            tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".bow",
                    String.valueOf(arrowBonus)).withStyle(ChatFormatting.GOLD));
        }
    }
}
