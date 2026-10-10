package com.robvanblerk.tieredarsenal.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.robvanblerk.tieredarsenal.module.GearModule;
import com.robvanblerk.tieredarsenal.module.ModuleHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** Auto-Smelt module: blocks broken with the tool drop what they would smelt into. */
public class AutoSmeltModifier extends LootModifier {
    public static final Supplier<Codec<AutoSmeltModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, AutoSmeltModifier::new)));

    public AutoSmeltModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (tool == null || !ModuleHelper.has(tool, GearModule.AUTO_SMELT)) return loot;
        if (context.getParamOrNull(LootContextParams.BLOCK_STATE) == null) return loot; // blocks only

        ServerLevel level = context.getLevel();
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
        for (ItemStack stack : loot) {
            ItemStack smelted = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), level)
                    .map(r -> r.getResultItem(level.registryAccess()))
                    .filter(out -> !out.isEmpty())
                    .map(out -> {
                        ItemStack copy = out.copy();
                        copy.setCount(Math.min(copy.getMaxStackSize(), out.getCount() * stack.getCount()));
                        return copy;
                    })
                    .orElse(stack);
            result.add(smelted);
        }
        return result;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
