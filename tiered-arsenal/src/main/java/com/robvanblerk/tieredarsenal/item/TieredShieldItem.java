package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.client.TieredShieldRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A shield per tier. Blocks like the vanilla shield, with more durability, and the top tiers
 * do something to melee attackers when you block them (event/WeaponEvents.java):
 *   Titanium  - knocks the attacker back
 *   Infernium - sets the attacker on fire
 *   Aetherium - reflects 25% of the blocked damage
 */
public class TieredShieldItem extends ShieldItem {
    private final String material;
    private final Supplier<Ingredient> repair;

    public TieredShieldItem(String material, Supplier<Ingredient> repair, Properties properties) {
        super(properties);
        this.material = material;
        this.repair = repair;
    }

    public String getMaterial() {
        return material;
    }

    /** 64x64 texture in the vanilla shield layout. */
    public ResourceLocation getTexture() {
        return new ResourceLocation(TieredArsenal.MOD_ID, "textures/entity/shield/" + material + ".png");
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairItem) {
        return repair.get().test(repairItem);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (material.equals("titanium") || material.equals("infernium") || material.equals("aetherium")) {
            tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".shield." + material)
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return TieredShieldRenderer.get();
            }
        });
    }
}
