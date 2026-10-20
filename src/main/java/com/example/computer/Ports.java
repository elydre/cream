package com.example.computer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Ports {
    private int[] keyboardBuffer = new int[16];
    private int keyboardBufferSize = 0;
    private final int[] redstoneOutputs = new int[8];
    private Level level;
    private BlockPos blockPos;
    private Direction facing = Direction.NORTH;
    public boolean screenNeedsUpdate = true;
    private int toWait = 0;
    private int cursorPosition = 0;

    public void updateContext(Level level, BlockPos blockPos, Direction facing) {
        this.level = level;
        this.blockPos = blockPos;
        this.facing = facing;
    }

    public void clear() {
        keyboardBufferSize = 0;
        java.util.Arrays.fill(redstoneOutputs, 0);
        screenNeedsUpdate = false;
    }

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
        if (port >= 0 && port < 8) {
            return redstoneInput(port);
        }

        switch (port) {
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
                return level == null ? 0 : (int) level.getOverworldClockTime();

            default:
                // unknown port
                return 0;
        }
    }

    public void portOut(int port, int value) {
        if (port >= 0 && port < 8) {
            redstoneOutputs[port] = value & 0xF;
            return;
        }

        switch (port) {
            case 0x1000:
                // nothing to do / debug port (printf("0x%x\n", value))
                return;
            case 0x1001:
                // nothing to do / debug port (printf("%d\n", value))
                return;
            case 0x1002:
                // nothing to do / debug port (putchar(value & 0xFF))
                return;
            case 0x1020:
                screenNeedsUpdate = true;
                return;
            case 0x1021:
                cursorPosition = Math.max(0, Math.min(80 * 25 - 1, value));
                return;
            case 0x1031:
                if (value == 0) {
                    toWait = 1;
                } else {
                    toWait = value;
                }
                return;
            default:
                // unknown port
                return;
        }
    }

    public boolean tickPause() {
        if (toWait > 0) {
            toWait--;
            return true;
        }
        return false;
    }

    public boolean isPaused() {
        return toWait > 0;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public int getRedstoneOutput(Direction direction) {
        int output = 0;
        for (int port = 0; port < redstoneOutputs.length; port++) {
            if (directionForPort(port).getOpposite() == direction) {
                output = Math.max(output, redstoneOutputs[port]);
            }
        }
        return output;
    }

    private int redstoneInput(int port) {
        if (level == null || blockPos == null) {
            return 0;
        }

        Direction direction = directionForPort(port);
        return level.getSignal(blockPos.relative(direction), direction) & 0xF;
    }

    private Direction directionForPort(int port) {
        Direction front = facing.getOpposite();
        return switch (port) {
            case 0 -> front;
            case 1 -> front.getClockWise();
            case 2 -> front.getOpposite();
            case 3 -> front.getCounterClockWise();
            case 4 -> Direction.NORTH;
            case 5 -> Direction.EAST;
            case 6 -> Direction.SOUTH;
            case 7 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    public boolean doesScreenNeedUpdate() {
        return screenNeedsUpdate;
    }

    public void forceScreenUpdate() {
        screenNeedsUpdate = true;
    }

    public void save(ValueOutput output) {
        int[] buffer = new int[keyboardBufferSize];
        System.arraycopy(keyboardBuffer, 0, buffer, 0, keyboardBufferSize);
        output.putIntArray("KeyboardBuffer", buffer);
        output.putIntArray("RedstoneOutputs", redstoneOutputs);
        output.putInt("CursorPosition", cursorPosition);
        output.putBoolean("ScreenNeedsUpdate", screenNeedsUpdate);
    }

    public void load(ValueInput input) {
        input.getIntArray("KeyboardBuffer").ifPresent(buffer -> {
            keyboardBufferSize = Math.min(buffer.length, keyboardBuffer.length);
            System.arraycopy(buffer, 0, keyboardBuffer, 0, keyboardBufferSize);
        });
        input.getIntArray("RedstoneOutputs").ifPresent(outputs -> {
            System.arraycopy(outputs, 0, redstoneOutputs, 0, Math.min(outputs.length, redstoneOutputs.length));
        });
        cursorPosition = Math.max(0, Math.min(80 * 25 - 1, input.getIntOr("CursorPosition", 0)));
        screenNeedsUpdate = input.getBooleanOr("ScreenNeedsUpdate", false);
    }
}
