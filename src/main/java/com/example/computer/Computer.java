package com.example.computer;

public class Computer {
    private final Memory memory;
    private final Cpu cpu;

    public Computer() {
        memory = new Memory();
        cpu = new Cpu(memory);
        memory.clear();
    }

    public void tick() {
        cpu.tick();
    }

    public Memory getMemory() {
        return memory;
    }

    public Cpu getCpu() {
        return cpu;
    }
}
