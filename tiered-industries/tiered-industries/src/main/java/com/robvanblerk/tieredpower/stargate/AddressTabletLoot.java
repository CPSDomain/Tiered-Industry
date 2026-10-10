package com.robvanblerk.tieredpower.stargate;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.robvanblerk.tieredpower.TieredPower;

/** Puts a Stargate Address tablet (for a random hidden world) into chests, with the chance set per loot table. */
public class AddressTabletLoot extends LootModifier {
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
			DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TieredPower.MOD_ID);

	public static final Supplier<Codec<AddressTabletLoot>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst)
			.and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
			.apply(inst, AddressTabletLoot::new)));

	public static final RegistryObject<Codec<AddressTabletLoot>> ADDRESS_TABLET = SERIALIZERS.register("address_tablet", CODEC);

	private final float chance;

	public AddressTabletLoot(LootItemCondition[] conditions, float chance) {
		super(conditions);
		this.chance = chance;
	}

	@Override
	protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
		if (context.getRandom().nextFloat() < chance) loot.add(AddressTabletItem.random(context.getRandom()));
		return loot;
	}

	@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC.get();
	}
}
