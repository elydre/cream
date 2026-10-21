package io.github.elydre.cream.blocks;

import io.github.elydre.cream.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;


public class FlashMachine extends Block {

    public FlashMachine() {
        super(Properties.of().strength(2.0f).setId(ModBlocks.keyOfBlock("flash_machine")));
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

        /*if (player instanceof ServerPlayer serverPlayer) {
            // check if the item in player's hand is a floppy disk

            if (itemStack.getItem() instanceof FloppyDisk) {
                // get the data from the floppy disk
                short[] data = FloppyDisk.getProgram(itemStack);

                // do something with the data, e.g., print it to the console
                System.out.println("Data from floppy disk: ");
                if (data != null) {
                    int sum = 0;
                    for (short value : data) {
                        // print each value as unsigned short (0 to 65535)
                        System.out.print((value & 0xFFFF) + " ");
                        sum += (value & 0xFFFF);
                    }
                    System.out.println();
                    System.out.println("Sum: " + sum);
                } else {
                    System.out.println("No data found on the floppy disk.");
                }
            } else {
                // if the item is not a floppy disk, send a message to the player
                Component message = Component.literal("You need to use a floppy disk on the flash machine.");
                serverPlayer.sendSystemMessage(message);
            }
        }*/

        return InteractionResult.SUCCESS;
    }
}
