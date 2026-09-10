package com.example.blocks;

import com.example.ModBlockEntities;
import com.example.blockEntities.ComputerBlockEntity;
import com.example.networking.ComputerScreenPayload;
import com.example.networking.OpenComputerPayload;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class aledblock extends BaseEntityBlock {

    public aledblock(Properties properties) {
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

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new ComputerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return createTickerHelper(
                type,
                ModBlockEntities.ALED_BLOCK_E,
                ComputerBlockEntity::tick
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(aledblock::new);
    }
}

/*
package com.example.blocks;

import com.example.ModBlockEntities;
import com.example.ModBlocks;
import com.example.blockEntities.ComputerBlockEntity;
import com.example.networking.OpenComputerPayload;
import com.mojang.serialization.MapCodec;
import com.example.networking.ComputerScreenPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;


public class aledblock extends BaseEntityBlock {
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

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new ComputerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return createTickerHelper(
                type,
                ModBlockEntities.ALED_BLOCK_E,
                ComputerBlockEntity::tick
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(aledblock::new);
    }
}
*/
