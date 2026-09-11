package com.example.computer;

public class XMem {
    public static final int SIZE = 65536;
    private final short[] data = new short[SIZE];

    public int read(int address) {
        return data[address & 0xFFFF] & 0xFFFF;
    }

    public void write(int address, int value) {
        data[address & 0xFFFF] = (short) (value & 0xFFFF);
    }

    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            data[i] = 0;
        }
    }
}
