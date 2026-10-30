package io.github.elydre.cream.computer;

import io.github.elydre.cream.ModBlocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PeripheralNetwork {
    private static final int MAX_SEARCH_BLOCKS = 256;
    private static final Map<Level, Long> CABLE_VERSIONS = new WeakHashMap<>();

    private PeripheralNetwork() {
    }

    public static void cableChanged(Level level) {
        CABLE_VERSIONS.merge(level, 1L, Long::sum);
    }

    public static long cableVersion(Level level) {
        return CABLE_VERSIONS.getOrDefault(level, 0L);
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

    public static boolean isConnected(Level level, BlockPos computerPos, PeripheralDevice peripheral) {
        if (!(peripheral instanceof BlockEntity blockEntity)) {
            return true;
        }

        BlockPos peripheralPos = blockEntity.getBlockPos();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visitedCables = new HashSet<>();

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
                if (adjacent.equals(peripheralPos)) {
                    return true;
                }

                if (level.getBlockState(adjacent).is(ModBlocks.CABLE)
                        && !visitedCables.contains(adjacent)) {
                    queue.addLast(adjacent);
                }
            }
        }

        return false;
    }
}
