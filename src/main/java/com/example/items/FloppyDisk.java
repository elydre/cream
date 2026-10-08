package com.example.items;

import com.example.components.FloppyProgram;
import com.example.ModComponents;
import com.example.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;

public class FloppyDisk extends Item {
    public static final String COLOR_PINK = "pink";
    public static final String COLOR_BLUE = "blue";
    public static final String COLOR_YELLOW = "yellow";
    public static final String COLOR_BASILISC = "basilisc";

    public FloppyDisk() {
        super(new Item.Properties().setId(ModItems.keyOfItem("floppy")).stacksTo(1));
    }

    public static ItemStack createColoredStack(String color) {
        ItemStack stack = new ItemStack(ModItems.FLOPP_DISK);
        stack.set(ModComponents.FLOPPY_COLOR, color);
        return stack;
    }

    public static ItemStack createProgramStack(
            String color,
            String progname,
            String resourcePath
    ) {
        ItemStack stack = createColoredStack(color);

        try (InputStream input = FloppyDisk.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                System.err.println("Unable to find floppy program resource: " + resourcePath);
                return ItemStack.EMPTY;
            }

            byte[] bytes = input.readAllBytes();
            if (bytes.length == 0 || bytes.length % 2 != 0) {
                System.err.println("Invalid floppy program resource size: " + resourcePath);
                return ItemStack.EMPTY;
            }

            short[] program = new short[bytes.length / 2];
            for (int i = 0; i < program.length; i++) {
                program[i] = (short) ((bytes[i * 2] & 0xFF)
                        | ((bytes[i * 2 + 1] & 0xFF) << 8));
            }

            stack.set(
                    ModComponents.FLOPPY_PROGRAM,
                    new FloppyProgram(progname, program)
            );
            return stack;
        } catch (IOException exception) {
            System.err.println("Unable to read floppy program resource: " + resourcePath);
            return ItemStack.EMPTY;
        }
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
            String progname,
            short[] program
    ) {
        stack.set(
                ModComponents.FLOPPY_PROGRAM,
                new FloppyProgram(progname, program.clone())
        );
    }

    public static void clearProgram(ItemStack stack) {
        stack.remove(ModComponents.FLOPPY_PROGRAM);
    }
}
