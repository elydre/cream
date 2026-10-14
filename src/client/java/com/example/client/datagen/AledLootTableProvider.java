package com.example.client.datagen;

import java.util.concurrent.CompletableFuture;

import com.example.ModBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;

public class AledLootTableProvider extends FabricBlockLootSubProvider {
    public AledLootTableProvider(
            FabricPackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.ALED_BLOCK);
    }
}
