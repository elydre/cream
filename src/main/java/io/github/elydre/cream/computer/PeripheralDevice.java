package io.github.elydre.cream.computer;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.IntFunction;

public interface PeripheralDevice {
    int getPeripheralType();

    int read(int port);

    default void updateContext(RWMem memory, IntFunction<PeripheralDevice> peripheralResolver) {
    }

    default boolean isAvailable() {
        if (!(this instanceof BlockEntity blockEntity)) {
            return true;
        }

        Level level = blockEntity.getLevel();
        return level != null
                && !blockEntity.isRemoved()
                && level.getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }

    default int write(int port, int value) {
        return 0;
    }
}