package com.example.client;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class ComputerClientState {

    private static final Map<BlockPos, byte[]> screens =
            new HashMap<>();

    public static void updateScreen(
            BlockPos pos,
            byte[] screen
    ) {
        if (screen.length != 80 * 25) {
            return;
        }

        // On fait une copie !
        screens.put(
                pos,
                screen.clone()
        );
    }

    public static byte[] getScreen(BlockPos pos) {
        return screens.get(pos);
    }
}
