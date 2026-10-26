package io.github.elydre.cream.blocks;

import io.github.elydre.cream.ModBlocks;
import io.github.elydre.cream.blockEntities.InventoryPeripheralBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class InventoryPeripheralBlock extends BaseEntityBlock {
    public InventoryPeripheralBlock() {
        super(Properties.of()
                .strength(1.5f)
                .isRedstoneConductor((state, level, pos) -> false)
                .setId(ModBlocks.keyOfBlock("inventory_peripheral")));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InventoryPeripheralBlockEntity(pos, state);
    }
}