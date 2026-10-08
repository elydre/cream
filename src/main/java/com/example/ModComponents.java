package com.example;

import com.example.components.FloppyProgram;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;

public class ModComponents {

    public static final DataComponentType<String> FLOPPY_COLOR =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(
                            ExampleMod.MOD_ID,
                            "floppy_color"
                    ),
                    DataComponentType
                            .<String>builder()
                            .persistent(com.mojang.serialization.Codec.STRING)
                            .build()
            );

    public static final DataComponentType<FloppyProgram> FLOPPY_PROGRAM =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(
                            ExampleMod.MOD_ID,
                            "floppy_program"
                    ),
                    DataComponentType
                            .<FloppyProgram>builder()
                            .persistent(FloppyProgram.CODEC)
                            .build()
            );

    public static void initialize() {
    }
}
