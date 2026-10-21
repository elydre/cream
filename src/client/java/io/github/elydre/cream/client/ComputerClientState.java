package io.github.elydre.cream.client;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class ComputerClientState {
    private static final Map<BlockPos, Integer> cursorPositions = new HashMap<>();
    private static final Map<BlockPos, short[]> screens = new HashMap<>();

    public static void updateScreen(BlockPos pos, short[] screen, int cursorPosition) {
        if (screen.length != 80 * 25) {
            return;
        }

        screens.put(pos, screen.clone());
        cursorPositions.put(pos, cursorPosition);
    }

    public static short[] getScreen(BlockPos pos) {
        return screens.get(pos);
    }

    public static int getCursorPosition(BlockPos pos) {
        return cursorPositions.getOrDefault(pos, 0);
    }
}
