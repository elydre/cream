package com.example.computer;

public class Computer {
    private final RWMem rwmem;
    private final XMem xmem;
    private final Cpu cpu;

    public Computer() {
        rwmem = new RWMem();
        xmem = new XMem();
        cpu = new Cpu(xmem, rwmem);
        rwmem.clear();
    }

    public void tick() {
        for (int i = 0; i < 100; i++) {
            cpu.tick();
        }
    }

    public RWMem getMemory() {
        return rwmem;
    }

    public Cpu getCpu() {
        return cpu;
    }
}
