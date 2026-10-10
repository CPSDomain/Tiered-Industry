package com.robvanblerk.tieredarsenal.registry;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.block.ArmouryWorkbenchBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Ores and storage blocks.
 *
 *   Cobalt    - Overworld, y -48 to 32 (stone + deepslate). Needs an iron pickaxe.
 *   Titanium  - Overworld, y -64 to -16 (mostly deepslate). Needs a diamond pickaxe.
 *   Infernium - Nether, in netherrack. Needs a diamond pickaxe.
 *   Aetherium - Outer End islands, in end stone. Needs a netherite pickaxe.
 */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TieredArsenal.MOD_ID);

    // ---- Ores ----
    public static final RegistryObject<Block> COBALT_ORE = register("cobalt_ore", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)
                    .strength(3.0F, 3.0F).requiresCorrectToolForDrops(), UniformInt.of(1, 3)), false);
    public static final RegistryObject<Block> DEEPSLATE_COBALT_ORE = register("deepslate_cobalt_ore", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE)
                    .strength(4.5F, 3.0F).requiresCorrectToolForDrops(), UniformInt.of(1, 3)), false);

    public static final RegistryObject<Block> TITANIUM_ORE = register("titanium_ore", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE)
                    .strength(3.5F, 4.0F).requiresCorrectToolForDrops(), UniformInt.of(2, 5)), false);
    public static final RegistryObject<Block> DEEPSLATE_TITANIUM_ORE = register("deepslate_titanium_ore", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE)
                    .strength(5.0F, 4.0F).requiresCorrectToolForDrops(), UniformInt.of(2, 5)), false);

    public static final RegistryObject<Block> NETHER_INFERNIUM_ORE = register("nether_infernium_ore", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHER_GOLD_ORE)
                    .strength(4.0F, 6.0F).lightLevel(state -> 5).requiresCorrectToolForDrops(),
                    UniformInt.of(3, 6)), true);

    public static final RegistryObject<Block> END_AETHERIUM_ORE = register("end_aetherium_ore", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.END_STONE)
                    .strength(6.0F, 12.0F).lightLevel(state -> 3).requiresCorrectToolForDrops(),
                    UniformInt.of(4, 8)), true);

    // ---- Raw blocks ----
    public static final RegistryObject<Block> RAW_COBALT_BLOCK = register("raw_cobalt_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.RAW_IRON_BLOCK)), false);
    public static final RegistryObject<Block> RAW_TITANIUM_BLOCK = register("raw_titanium_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.RAW_IRON_BLOCK).strength(6.0F, 7.0F)), false);
    public static final RegistryObject<Block> RAW_INFERNIUM_BLOCK = register("raw_infernium_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.RAW_GOLD_BLOCK).strength(6.0F, 9.0F)
                    .lightLevel(state -> 7)), true);
    public static final RegistryObject<Block> RAW_AETHERIUM_BLOCK = register("raw_aetherium_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.RAW_IRON_BLOCK).strength(7.0F, 12.0F)
                    .lightLevel(state -> 5)), true);

    // ---- Storage blocks ----
    public static final RegistryObject<Block> COBALT_BLOCK = register("cobalt_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)), false);
    public static final RegistryObject<Block> TITANIUM_BLOCK = register("titanium_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(6.0F, 8.0F)), false);
    public static final RegistryObject<Block> INFERNIUM_BLOCK = register("infernium_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).lightLevel(state -> 10)), true);
    public static final RegistryObject<Block> AETHERIUM_BLOCK = register("aetherium_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK).lightLevel(state -> 8)), true);

    // ---- Machines ----
    public static final RegistryObject<Block> ARMOURY_WORKBENCH = register("armoury_workbench", () ->
            new ArmouryWorkbenchBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.5F, 6.0F)), false);

    /** Registers the block and a matching BlockItem. */
    private static RegistryObject<Block> register(String name, Supplier<Block> block, boolean fireResistant) {
        RegistryObject<Block> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> {
            Item.Properties props = new Item.Properties();
            if (fireResistant) {
                props.fireResistant();
            }
            return new BlockItem(registered.get(), props);
        });
        return registered;
    }
}
