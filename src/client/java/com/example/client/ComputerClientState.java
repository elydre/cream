package com.example.client;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class ComputerClientState {

    private static final Map<BlockPos, short[]> screens =
            new HashMap<>();

    public static void updateScreen(BlockPos pos, short[] screen) {
        if (screen.length != 80 * 25) {
            return;
        }

        screens.put(pos, screen.clone());
    }

    public static short[] getScreen(BlockPos pos) {
        return screens.get(pos);
    }
}
