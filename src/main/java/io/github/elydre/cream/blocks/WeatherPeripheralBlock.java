package io.github.elydre.cream.blocks;

import io.github.elydre.cream.ModBlocks;
import io.github.elydre.cream.blockEntities.WeatherPeripheralBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WeatherPeripheralBlock extends BaseEntityBlock {
    public WeatherPeripheralBlock() {
        super(Properties.of()
                .strength(1.5f)
                .isRedstoneConductor((state, level, pos) -> false)
                .setId(ModBlocks.keyOfBlock("weather_peripheral")));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeatherPeripheralBlockEntity(pos, state);
    }
}