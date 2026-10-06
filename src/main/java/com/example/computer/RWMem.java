package com.example.computer;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RWMem {
    public static final int SIZE = 65536;
    public static final int SCREEN_BASE = (SIZE - (80 * 25));

    private final short[] data = new short[SIZE];

    public int read(int address) {
        return data[address & 0xFFFF] & 0xFFFF;
    }

    public void write(int address, int value) {
        data[address & 0xFFFF] = (short) (value & 0xFFFF);
    }

    public void clear() {
        for (int i = 0; i < data.length; i++) {
            data[i] = 0;
        }
    }

    public void save(ValueOutput output) {
        int[] values = new int[SIZE];
        for (int i = 0; i < SIZE; i++) {
            values[i] = data[i];
        }
        output.putIntArray("Data", values);
    }

    public void load(ValueInput input) {
        input.getIntArray("Data").ifPresent(values -> {
            if (values.length != SIZE) {
                return;
            }
            for (int i = 0; i < SIZE; i++) {
                data[i] = (short) values[i];
            }
        });
    }
}
