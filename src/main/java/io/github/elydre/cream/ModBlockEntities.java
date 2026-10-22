package io.github.elydre.cream;

import io.github.elydre.cream.blockEntities.ComputerBlockEntity;
import io.github.elydre.cream.blockEntities.WeatherPeripheralBlockEntity;

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
                            CreamMain.MOD_ID,
                            "computer"
                    ),
                    FabricBlockEntityTypeBuilder
                            .<ComputerBlockEntity>create(
                                    ComputerBlockEntity::new,
                                    ModBlocks.ALED_BLOCK
                            )
                            .build()
            );

    public static final BlockEntityType<WeatherPeripheralBlockEntity> WEATHER_PERIPHERAL =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(CreamMain.MOD_ID, "weather_peripheral"),
                    FabricBlockEntityTypeBuilder
                            .<WeatherPeripheralBlockEntity>create(
                                    WeatherPeripheralBlockEntity::new,
                                    ModBlocks.WEATHER_PERIPHERAL
                            )
                            .build()
            );

    public static void initialize() {
    }
}
