package com.robvanblerk.tieredarsenal.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.robvanblerk.tieredarsenal.config.ArsenalConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Adds an item to a loot table. Which chest and how often is set in the JSON files under
 * data/tieredarsenal/loot_modifiers (loot table id + random chance conditions).
 */
public class AddItemModifier extends LootModifier {
    public static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).and(inst.group(
                    ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item),
                    Codec.INT.optionalFieldOf("min", 1).forGetter(m -> m.min),
                    Codec.INT.optionalFieldOf("max", 1).forGetter(m -> m.max)
            )).apply(inst, AddItemModifier::new)));

    private final Item item;
    private final int min;
    private final int max;

    public AddItemModifier(LootItemCondition[] conditions, Item item, int min, int max) {
        super(conditions);
        this.item = item;
        this.min = min;
        this.max = Math.max(min, max);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!ArsenalConfig.CHEST_LOOT.get()) return loot;
        int count = min + context.getRandom().nextInt(max - min + 1);
        loot.add(new ItemStack(item, count));
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
