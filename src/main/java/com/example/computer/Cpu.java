package com.example.computer;

public class Cpu {

    private final Memory memory;

    /*
     * Program Counter
     *
     * Adresse de la prochaine instruction.
     */
    private int pc = 0;

    /*
     * Stack Pointer
     *
     * On l'utilisera plus tard pour push/pop.
     */
    private int sp = 0;

    /*
     * Permet de stopper le CPU avec l'instruction HLT.
     */
    private boolean halted = false;

    public Cpu(Memory memory) {
        this.memory = memory;
    }

    /**
     * Exécute UNE instruction.
     */
    public void tick() {

        if (halted) {
            return;
        }

        /*
         * Pour l'instant :
         * on récupère simplement l'instruction.
         */
        short instruction = memory.read(pc);

        /*
         * On avance vers l'instruction suivante.
         */
        pc++;

        execute(instruction);
    }

    /**
     * Exécute une instruction Ancolie.
     */
    private void execute(short instruction) {

        /*
         * TODO :
         *
         * Décoder l'opcode ici.
         *
         * Pour commencer, on peut par exemple
         * décider que :
         *
         * 0 = NOP
         * 1 = HLT
         *
         */

        switch (instruction) {

            case 0 -> {
                // NOP
            }

            case 1 -> {
                // HLT
                halted = true;
            }

            default -> {
                throw new IllegalStateException(
                        "Unknown instruction: " + instruction
                );
            }
        }
    }

    public boolean isHalted() {
        return halted;
    }

    public void reset() {
        pc = 0;
        sp = 0;
        halted = false;
    }

    public int getPc() {
        return pc;
    }

    public int getSp() {
        return sp;
    }
}
