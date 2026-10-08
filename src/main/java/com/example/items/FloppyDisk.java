package com.example.items;

import com.example.components.FloppyProgram;
import com.example.ModComponents;
import com.example.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FloppyDisk extends Item {
    public static final String PINK_COLOR = "pink";
    public static final String BLUE_COLOR = "blue";
    public static final String YELLOW_COLOR = "yellow";

    public FloppyDisk() {
        super(new Item.Properties().setId(ModItems.keyOfItem("floppy_disk")).stacksTo(1));
    }

    public static ItemStack createColoredStack(String color) {
        ItemStack stack = new ItemStack(ModItems.FLOPP_DISK);
        stack.set(ModComponents.FLOPPY_COLOR, color);
        return stack;
    }

    public static boolean hasProgram(ItemStack stack) {
        return stack.has(ModComponents.FLOPPY_PROGRAM);
    }

    public static short[] getProgram(ItemStack stack) {
        FloppyProgram program =
                stack.get(ModComponents.FLOPPY_PROGRAM);

        if (program == null) {
            return null;
        }

        return program.copyData();
    }

    public static void setProgram(
            ItemStack stack,
            String author,
            short[] program
    ) {
        stack.set(
                ModComponents.FLOPPY_PROGRAM,
                new FloppyProgram(author, program.clone())
        );
    }

    public static void clearProgram(ItemStack stack) {
        stack.remove(ModComponents.FLOPPY_PROGRAM);
    }
}
