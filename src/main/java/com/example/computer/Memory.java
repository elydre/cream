package com.example.computer;

public class Memory {

    /*
     * Pour commencer, on utilise 65536 mots de 16 bits.
     *
     * 0x0000 -> 0xFFFF
     */
    public static final int SIZE = 65536;

    private final short[] data = new short[SIZE];

    public short read(int address) {
        checkAddress(address);

        return data[address];
    }

    public void write(int address, short value) {
        checkAddress(address);

        data[address] = value;
    }

    private void checkAddress(int address) {
        if (address < 0 || address >= SIZE) {
            throw new IllegalArgumentException(
                    "Invalid memory address: " + address
            );
        }
    }

    public void clear() {
        for (int i = 0; i < data.length; i++) {
            data[i] = 0;
        }
    }
}
