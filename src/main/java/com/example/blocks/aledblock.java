package com.example.blocks;

import com.example.ModBlocks;
import com.example.networking.OpenComputerPayload;
import com.example.networking.ComputerScreenPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;


public class aledblock extends Block {
    public aledblock() {
        super(BlockBehaviour.Properties.of().setId(ModBlocks.keyOfBlock("plate")).sound(SoundType.DEEPSLATE).noOcclusion());
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

            OpenComputerPayload payload =
                    new OpenComputerPayload(pos);

            ServerPlayNetworking.send(
                    serverPlayer,
                    payload
            );

            // create the byte array for the screen data (80 * 25)
            byte[] screenData = new byte[80 * 25];

            // write hello world to the screen data
            String helloWorld = "Hello World!";
            for (int i = 0; i < helloWorld.length(); i++) {
                screenData[i] = (byte) helloWorld.charAt(i);
            }

            ComputerScreenPayload screenPayload =
                    new ComputerScreenPayload(
                            pos,
                            screenData
                    );
            
            ServerPlayNetworking.send(
                    serverPlayer,
                    screenPayload
            );
        }

        return InteractionResult.SUCCESS;
    }
}
