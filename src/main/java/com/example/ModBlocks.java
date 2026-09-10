package com.example;

import java.util.function.Supplier;

import com.example.blocks.aledblock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public class ModBlocks {

	private static Block register(String name, Supplier<Block> blockFactory) {
		ResourceKey<Block> blockKey = keyOfBlock(name);
		Block block = blockFactory.get();

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static final Block ALED_BLOCK = register("aledblock", aledblock::new);

	public static ResourceKey<Block> keyOfBlock(String name) {
		return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, name));
	}

	public static void initialize() {}
}