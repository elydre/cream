package com.example.items;

import com.example.ModComponents;
import com.example.ModItems;
import com.example.components.FloppyProgram;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class floppyDisk extends Item {
    public floppyDisk() {
        super(new Item.Properties().setId(ModItems.keyOfItem("floppy_disk")).stacksTo(1));
    }

    public static boolean hasProgram(ItemStack stack) {
        return stack.has(ModComponents.FLOPPY_PROGRAM);
    }

    public static byte[] getProgram(ItemStack stack) {

        FloppyProgram program =
                stack.get(ModComponents.FLOPPY_PROGRAM);

        if (program == null) {
            return null;
        }

        return program.copyData();
    }

    public static void setProgram(
            ItemStack stack,
            byte[] program
    ) {
        stack.set(
                ModComponents.FLOPPY_PROGRAM,
                new FloppyProgram(program.clone())
        );
    }

    public static void clearProgram(ItemStack stack) {
        stack.remove(ModComponents.FLOPPY_PROGRAM);
    }
}
