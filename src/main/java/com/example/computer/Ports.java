package com.example.computer;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Ports {
    // keyboard buffer
    private int[] keyboardBuffer = new int[16];
    private int keyboardBufferSize = 0;
    public boolean screenNeedsUpdate = false;

    public void keyPress(int key) {
        if (keyboardBufferSize < keyboardBuffer.length) {
            keyboardBuffer[keyboardBufferSize++] = key;
            return;
        }
        for (int i = 1; i < keyboardBuffer.length; i++) {
            keyboardBuffer[i - 1] = keyboardBuffer[i];
        }
        keyboardBuffer[keyboardBuffer.length - 1] = key;
    }

    public int portIn(int port) {
        switch (port) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                // redstone input
                return 0;

            case 0x1010:
                if (keyboardBufferSize == 0) {
                    return 0;
                }

                return (keyboardBuffer[0] & 0xFF0000) >> 16;

            case 0x1011:
                if (keyboardBufferSize == 0) {
                    return 0;
                }

                int val = keyboardBuffer[0] & 0xFFFF;

                // shift the buffer
                for (int i = 1; i < keyboardBufferSize; i++) {
                    keyboardBuffer[i - 1] = keyboardBuffer[i];
                }
                keyboardBufferSize--;
                return val;

            case 0x1030:
                // get minecraft time (24h) in ticks
                return 0;
            default:
                // unknown port
                return 0;
        }
    }

    public void portOut(int port, int value) {
        switch (port) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                // redstone output
                return;
            case 0x1000:
                // debug port (printf("0x%x\n", value))
                return;
            case 0x1001:
                // debug port (printf("%d\n", value))
                return;
            case 0x1002:
                // debug port (putchar(value & 0xFF))
                return;
            case 0x1020:
                screenNeedsUpdate = true;
                return;
            case 0x1021:
                // move cursor
                return;
            default:
                // unknown port
                return;
        }
    }

    public boolean doesScreenNeedUpdate() {
        return screenNeedsUpdate;
    }

    public void save(ValueOutput output) {
        int[] buffer = new int[keyboardBufferSize];
        System.arraycopy(keyboardBuffer, 0, buffer, 0, keyboardBufferSize);
        output.putIntArray("KeyboardBuffer", buffer);
        output.putBoolean("ScreenNeedsUpdate", screenNeedsUpdate);
    }

    public void load(ValueInput input) {
        input.getIntArray("KeyboardBuffer").ifPresent(buffer -> {
            keyboardBufferSize = Math.min(buffer.length, keyboardBuffer.length);
            System.arraycopy(buffer, 0, keyboardBuffer, 0, keyboardBufferSize);
        });
        screenNeedsUpdate = input.getBooleanOr("ScreenNeedsUpdate", false);
    }
}
