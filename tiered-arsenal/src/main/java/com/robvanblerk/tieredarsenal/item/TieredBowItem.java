package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A bow per tier: more durability and more arrow damage the higher the tier.
 * A vanilla arrow does 2 base damage; the bonus is added on top, before bow charge
 * and Power are applied. Enchants and repairs like the tier's other gear.
 */
public class TieredBowItem extends BowItem {
    private final Tier tier;
    private final float arrowBonus;

    public TieredBowItem(Tier tier, float arrowBonus, Properties properties) {
        super(properties);
        this.tier = tier;
        this.arrowBonus = arrowBonus;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        arrow.setBaseDamage(arrow.getBaseDamage() + arrowBonus);
        return arrow;
    }

    @Override
    public int getEnchantmentValue() {
        return tier.getEnchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return tier.getRepairIngredient().test(repair) || super.isValidRepairItem(stack, repair);
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
