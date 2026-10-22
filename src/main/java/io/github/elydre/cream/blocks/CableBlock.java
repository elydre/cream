package io.github.elydre.cream.blocks;

import io.github.elydre.cream.ModBlocks;

import net.minecraft.world.level.block.Block;

public class CableBlock extends Block {
    public CableBlock() {
        super(Properties.of()
                .strength(0.5f)
                .noOcclusion()
                .setId(ModBlocks.keyOfBlock("cable")));
    }
}