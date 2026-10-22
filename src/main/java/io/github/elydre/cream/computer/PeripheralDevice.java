package io.github.elydre.cream.computer;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface PeripheralDevice {
    int getPeripheralType();

    int read(int port);

    default boolean isAvailable() {
        if (!(this instanceof BlockEntity blockEntity)) {
            return true;
        }

        Level level = blockEntity.getLevel();
        return level != null
                && !blockEntity.isRemoved()
                && level.getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }

    default void write(int port, int value) {
    }
}