package io.github.elydre.cream;

import io.github.elydre.cream.items.FloppyDisk;

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
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), Identifier.fromNamespaceAndPath(CreamMain.MOD_ID, "cream")
    );

    public static final CreativeModeTab CUSTOM_CREATIVE_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.FLOPP_DISK))
            .title(Component.translatable("creativeTab.cream"))
            .displayItems((params, output) -> {
                output.accept(ModItems.ALED_BLOCK);
                output.accept(ModItems.FLASH_MACHINE);
                output.accept(ModItems.CABLE);
                output.accept(ModItems.WEATHER_PERIPHERAL);
                output.accept(ModItems.FLOPP_DISK);
                output.accept(FloppyDisk.createColoredStack(FloppyDisk.COLOR_PINK));
                output.accept(FloppyDisk.createColoredStack(FloppyDisk.COLOR_BLUE));
                output.accept(FloppyDisk.createColoredStack(FloppyDisk.COLOR_YELLOW));

                ItemStack basiliscFloppy = FloppyDisk.createProgramStack(
                        FloppyDisk.COLOR_BASILISC,
                        "Basilisc",
                        "/assets/cream/binaries/basilisc.bin"
                );
                if (!basiliscFloppy.isEmpty()) {
                    output.accept(basiliscFloppy);
                }
            })
            .build();

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CHOU_CREATIVE_TAB_KEY, CUSTOM_CREATIVE_TAB);
    }
}
