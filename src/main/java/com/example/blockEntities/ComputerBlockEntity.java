package com.example.blockEntities;

import com.example.computer.Computer;
import com.example.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ComputerBlockEntity extends BlockEntity {

    private final Computer computer;

    public ComputerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.ALED_BLOCK_E,
                pos,
                state
        );

        computer = new Computer();
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            ComputerBlockEntity computer
    ) {
        if (level.isClientSide()) {
            return;
        }

        computer.computer.tick();
    }

    public Computer getComputer() {
        return computer;
    }
}
