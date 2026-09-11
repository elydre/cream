package com.example.blocks;

import com.example.items.floppyDisk;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;


public class flashmachine extends Block {

    public flashmachine(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            // check if the item in player's hand is a floppy disk

            if (itemStack.getItem() instanceof floppyDisk) {
                // get the data from the floppy disk
                byte[] data = floppyDisk.getProgram(itemStack);

                // do something with the data, e.g., print it to the console
                System.out.println("Data from floppy disk: ");
                if (data != null) {
                    for (byte b : data) {
                        System.out.print(b + " ");
                    }
                    System.out.println();
                } else {
                    // write a program to the floppy disk if it doesn't have one
                    byte[] newProgram = {1, 2, 3, 4, 5}; // example program data
                    floppyDisk.setProgram(itemStack, "hello", newProgram);
                    System.out.println("Wrote new program to floppy disk.");
                }
            } else {
                // if the item is not a floppy disk, send a message to the player
                Component message = Component.literal("You need to use a floppy disk on the flash machine.");
                serverPlayer.sendSystemMessage(message);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
