package com.example;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "aled";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModNetworking.initialize();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModComponents.initialize();
        ModItems.initialize();

        ItemComponentTooltipProviderRegistry.addAfter(
                DataComponents.DAMAGE,
                ModComponents.FLOPPY_PROGRAM
        );

        ModCreativeTabs.initialize();
        ModCommands.initialize();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
