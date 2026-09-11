package com.example;

import com.example.items.floppyDisk;

import com.google.common.base.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModItems {
	private static Item register_item(String name, Supplier<Item> blockFactory) {
		ResourceKey<Item> itemKey = ModItems.keyOfItem(name);
		return Registry.register(BuiltInRegistries.ITEM, itemKey, blockFactory.get());
    }

	private static Item register_block(String name, Block block) {
		ResourceKey<Item> itemKey = keyOfItem(name);
		return Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
	}

    public static final Item FLOPP_DISK = register_item("floppy_disk", floppyDisk::new);
	public static final Item ALED_BLOCK = register_block("aledblock", ModBlocks.ALED_BLOCK);

	public static ResourceKey<Item> keyOfItem(String name) {
		return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, name));
	}

	public static void initialize() {}
}
