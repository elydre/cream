package com.example;

import com.google.common.base.Supplier;

import com.example.blocks.aledblock;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModItems {
	// private static Item register(String name, Supplier<Item> blockFactory) {
	// 	ResourceKey<Item> itemKey = ModItems.keyOfItem(name);
	// 	return Registry.register(BuiltInRegistries.ITEM, itemKey, blockFactory.get());
    // }

	private static Item register_block(String name, Block block) {
		ResourceKey<Item> itemKey = keyOfItem(name);
		return Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
	}

    // public static final Item GREAT_SWORD = register("great_sword", great_sword::new);
	public static final Item ALED_BLOCK = register_block("aled", ModBlocks.ALED_BLOCK);

	public static ResourceKey<Item> keyOfItem(String name) {
		return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, name));
	}

	public static void initialize() {}
}
