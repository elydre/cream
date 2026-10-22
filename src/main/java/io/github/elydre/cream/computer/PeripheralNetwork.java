package io.github.elydre.cream.computer;

import io.github.elydre.cream.ModBlocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PeripheralNetwork {
    private static final int MAX_SEARCH_BLOCKS = 256;

    private PeripheralNetwork() {
    }

    public static List<PeripheralDevice> findAll(Level level, BlockPos computerPos) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visitedCables = new HashSet<>();
        Set<BlockPos> visitedPeripherals = new HashSet<>();
        List<PeripheralDevice> peripherals = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos firstCable = computerPos.relative(direction);
            if (level.getBlockState(firstCable).is(ModBlocks.CABLE)) {
                queue.addLast(firstCable);
            }
        }

        while (!queue.isEmpty() && visitedCables.size() < MAX_SEARCH_BLOCKS) {
            BlockPos current = queue.removeFirst();
            if (!visitedCables.add(current)) {
                continue;
            }

            for (Direction side : Direction.values()) {
                BlockPos adjacent = current.relative(side);
                BlockEntity blockEntity = level.getBlockEntity(adjacent);
                if (blockEntity instanceof PeripheralDevice peripheral
                        && visitedPeripherals.add(adjacent)) {
                    peripherals.add(peripheral);
                }

                if (level.getBlockState(adjacent).is(ModBlocks.CABLE)
                        && !visitedCables.contains(adjacent)) {
                    queue.addLast(adjacent);
                }
            }
        }

        return peripherals;
    }
}