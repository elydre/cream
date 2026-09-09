package com.example.blocks;

import com.example.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        player.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        return InteractionResult.SUCCESS;
    }
}