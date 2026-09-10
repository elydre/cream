package com.example.computer;

public class Memory {
    public static final int SIZE = 65536;
    public static final int SCREEN_BASE = (SIZE - (80 * 25));

    private final short[] data = new short[SIZE];

    private void checkAddress(int address) {
        if (address < 0 || address >= SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
    }

    public short read(int address) {
        checkAddress(address);

        return data[address];
    }

    public void write(int address, short value) {
        checkAddress(address);

        data[address] = value;
    }

    public void clear() {
        for (int i = 0; i < data.length; i++) {
            data[i] = 'f';
        }
    }
}
