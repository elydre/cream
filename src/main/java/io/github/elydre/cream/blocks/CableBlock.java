package io.github.elydre.cream.blocks;

import io.github.elydre.cream.ModBlocks;
import io.github.elydre.cream.computer.PeripheralNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CableBlock extends Block {
    public CableBlock() {
        super(Properties.of()
                .strength(0.5f)
                .noOcclusion()
                .setId(ModBlocks.keyOfBlock("cable")));
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        if (level instanceof Level actualLevel) {
            PeripheralNetwork.cableChanged(actualLevel);
        }
    }
}
