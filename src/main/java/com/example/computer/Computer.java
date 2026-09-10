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
        int cost = 0;

        while (cost < 2000) {
            cost += cpu.tick();
        }
        System.out.println("CPU ticked 1000 times. PC: " + cpu.getPC());
    }

    public RWMem getMemory() {
        return rwmem;
    }

    public Cpu getCpu() {
        return cpu;
    }
}
