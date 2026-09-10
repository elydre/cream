package com.example.computer;

public class Cpu {

    private final Memory memory;
    private int pc = 0;

    private boolean halted = false;

    public Cpu(Memory memory) {
        this.memory = memory;
    }

    public boolean isHalted() {
        return halted;
    }

    public void reset() {
        pc = 0;
        halted = false;
    }

    public void tick() {
        if (halted) {
            return;
        }

        pc++;

        // write the pc value to the screen memory in ascii
        for (int i = 0; i < 4; i++) {
            int digit = (pc >> (12 - i * 4)) & 0xF;
            char ascii = (char) (digit < 10 ? '0' + digit : 'A' + (digit - 10));
            memory.write(Memory.SCREEN_BASE + i, (short) ascii);
        }
    }

    public boolean screenNeedsUpdate() {
        return true;
    }
}
