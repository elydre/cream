package com.example;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> CHOU_CREATIVE_TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "aled")
    );

    public static final CreativeModeTab CUSTOM_CREATIVE_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.FLOPP_DISK))
            .title(Component.translatable("creativeTab.aled"))
            .displayItems((params, output) -> {
                output.accept(ModItems.ALED_BLOCK);
                output.accept(ModItems.FLASH_MACHINE);
                output.accept(ModItems.FLOPP_DISK);
            })
            .build();

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CHOU_CREATIVE_TAB_KEY, CUSTOM_CREATIVE_TAB);
    }
}
