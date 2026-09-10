package com.example;

import com.example.blockEntities.ComputerBlockEntity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static final BlockEntityType<ComputerBlockEntity> ALED_BLOCK_E =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            ExampleMod.MOD_ID,
                            "computer"
                    ),
                    FabricBlockEntityTypeBuilder
                            .<ComputerBlockEntity>create(
                                    ComputerBlockEntity::new,
                                    ModBlocks.ALED_BLOCK
                            )
                            .build()
            );

    public static void initialize() {
    }
}
