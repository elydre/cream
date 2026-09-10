package com.example.computer;

public class Computer {

    public static final int INSTRUCTIONS_PER_TICK = 1000;

    private final Memory memory;
    private final Cpu cpu;

    public Computer() {

        memory = new Memory();

        cpu = new Cpu(memory);
    }

    /**
     * Appelé une fois par tick Minecraft.
     */
    public void tick() {

        if (cpu.isHalted()) {
            return;
        }

        /*
         * On limite le nombre d'instructions
         * exécutées par tick Minecraft.
         *
         * Cela empêche un programme infini
         * de bloquer le serveur.
         */
        for (int i = 0; i < INSTRUCTIONS_PER_TICK; i++) {

            cpu.tick();

            if (cpu.isHalted()) {
                break;
            }
        }
    }

    public Memory getMemory() {
        return memory;
    }

    public Cpu getCpu() {
        return cpu;
    }
}
