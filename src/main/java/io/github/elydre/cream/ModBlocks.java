package io.github.elydre.cream;

import java.util.function.Supplier;

import io.github.elydre.cream.blocks.AledBlock;
import io.github.elydre.cream.blocks.FlashMachine;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public class ModBlocks {

    private static Block register(Supplier<Block> blockFactory) {

        Block block = blockFactory.get();
        return Registry.register(
                BuiltInRegistries.BLOCK,
                block.properties().blockId(),
                block
        );
    }

    public static final Block ALED_BLOCK = register(AledBlock::new);
    public static final Block FLASH_MACHINE = register(FlashMachine::new);

    public static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(
                        ExampleMod.MOD_ID,
                        name
                )
        );
    }

    public static void initialize() {
    }
}
