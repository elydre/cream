package com.example.blocks;

import com.example.ModBlockEntities;
import com.example.blockEntities.ComputerBlockEntity;
import com.example.items.FloppyDisk;
import com.example.networking.ComputerOpenPayload;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

public class AledBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public AledBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof ComputerBlockEntity computer) {

                if (itemStack.getItem() instanceof FloppyDisk) {
                    ejectFloppyDisk(level, pos, state, computer);
                    String error = computer.insertFloppyDisk(itemStack);
                    if (error == null) {
                        player.sendSystemMessage(Component.translatable("component.aled.floppy_disk.inserted"));
                    } else {
                        player.sendSystemMessage(Component.translatable(error).withStyle(ChatFormatting.RED));
                        ejectFloppyDisk(level, pos, state, computer);
                    }

                    return InteractionResult.SUCCESS;
                }

                if (player.isShiftKeyDown()) {
                    ejectFloppyDisk(level, pos, state, computer);
                    return InteractionResult.SUCCESS;
                }

                computer.addViewer(serverPlayer);

                ServerPlayNetworking.send(
                        serverPlayer,
                        new ComputerOpenPayload(pos)
                );
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static void ejectFloppyDisk(
            Level level,
            BlockPos pos,
            BlockState state,
            ComputerBlockEntity computer
    ) {
        ItemStack disk = computer.ejectFloppyDisk();
        if (!disk.isEmpty()) {
            BlockPos frontPos = pos.relative(state.getValue(FACING));
            level.addFreshEntity(new ItemEntity(
                    level,
                frontPos.getX() + 0.5,
                frontPos.getY() + 0.5,
                frontPos.getZ() + 0.5,
                    disk
            ));
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComputerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> type)
    {
        return createTickerHelper(type, ModBlockEntities.ALED_BLOCK_E, ComputerBlockEntity::tick);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(AledBlock::new);
    }
}
