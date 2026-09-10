package com.example.computer;

public class RWMem {
    public static final int SIZE = 65536;
    public static final int SCREEN_BASE = (SIZE - (80 * 25));

    private final char[] data = new char[SIZE];

    public int read(int address) {
        return data[address & 0xFFFF];
    }

    public void write(int address, int value) {
        data[address & 0xFFFF] = (char) (value & 0xFFFF);
    }

    public void clear() {
        for (int i = 0; i < data.length; i++) {
            data[i] = 0;
        }
    }
}
