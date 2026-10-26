package io.github.elydre.cream.client.datagen;

import java.util.concurrent.CompletableFuture;

import io.github.elydre.cream.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;

public class BlocksLootTableProvider extends FabricBlockLootSubProvider {
    public BlocksLootTableProvider(
            FabricPackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture
    ) {
        super(output, registriesFuture);
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.ALED_BLOCK);
        dropSelf(ModBlocks.FLASH_MACHINE);
        dropSelf(ModBlocks.CABLE);
        dropSelf(ModBlocks.WEATHER_PERIPHERAL);
        dropSelf(ModBlocks.INVENTORY_PERIPHERAL);
    }
}
