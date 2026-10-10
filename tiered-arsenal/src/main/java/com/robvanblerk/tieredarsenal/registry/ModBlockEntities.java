package com.robvanblerk.tieredarsenal.registry;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.block.entity.ArmouryWorkbenchBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TieredArsenal.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ArmouryWorkbenchBlockEntity>> ARMOURY_WORKBENCH =
            BLOCK_ENTITIES.register("armoury_workbench", () -> BlockEntityType.Builder
                    .of(ArmouryWorkbenchBlockEntity::new, ModBlocks.ARMOURY_WORKBENCH.get())
                    .build(null));
}
