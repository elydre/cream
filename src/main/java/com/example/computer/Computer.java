package com.example.computer;

public class Computer {
    private final RWMem rwmem;
    private final XMem xmem;
    private final Cpu cpu;

    // Constants for program loading
    private final int ARCH_VERSION = 1;
    private final int MAGIC_NUMBER = 0xF057;
    private final int MAX_SECTIONS = 16;

    private final int SECTION_TYPE_CODE = 0;
    private final int SECTION_TYPE_DATA = 1;

    public Computer() {
        rwmem = new RWMem();
        xmem = new XMem();
        cpu = new Cpu(xmem, rwmem);
    }

    public void tick() {
        if (cpu.isHalted()) {
            return;
        }

        int cost = 0;
        while (cost < 2000) {
            cost += cpu.tick() + 1;
        }

        System.out.println("CPU ticked 1000 times. PC: " + cpu.getPC());
    }

    public String loadProgram(short[] program) {
        if (program == null) {
            return "block.aled.aledblock.errload_empty_floppy";
        }

        if (program.length < 3) {
            return "block.aled.aledblock.errload_invalid_header";
        }

        int magic       = program[0] & 0xFFFF;
        int arch        = program[1] & 0xFFFF;
        int numSections = program[2] & 0xFFFF;

        rwmem.clear();
        xmem.clear();

        if (program.length < 3 + numSections * 4) {
            return "block.aled.aledblock.errload_invalid_header";
        }

        if (magic != MAGIC_NUMBER) {
            return "block.aled.aledblock.errload_magic_number";
        }

        if (arch != ARCH_VERSION) {
            return "block.aled.aledblock.errload_arch_version";
        }

        if (numSections > MAX_SECTIONS) {
            return "block.aled.aledblock.errload_too_many_sections";
        }

        for (int i = 0; i < numSections; i++) {
            int sectionStartIndex = 3 + i * 4;
            int type     = program[sectionStartIndex]     & 0xFFFF;
            int debut    = program[sectionStartIndex + 1] & 0xFFFF;
            int size     = program[sectionStartIndex + 2] & 0xFFFF;
            int destAddr = program[sectionStartIndex + 3] & 0xFFFF;

            System.out.println("Loading section " + i + ": type=" + type + ", debut=" + debut + ", destAddr=" + destAddr + ", size=" + size);

            if (type != SECTION_TYPE_CODE && type != SECTION_TYPE_DATA) {
                return "block.aled.aledblock.errload_unrecognized_section";
            }

            if (size % 2 != 0 || debut % 2 != 0) {
                return "block.aled.aledblock.errload_section_size1";
            }

            if (type == SECTION_TYPE_CODE) {
                if (destAddr + (size / 2) > XMem.SIZE) {
                    return "block.aled.aledblock.errload_section_size2";
                }

                if ((debut + size) / 2 > program.length) {
                    return "block.aled.aledblock.errload_section_size3";
                }

                for (int j = 0; j < size / 2; j++) {
                    xmem.write(destAddr + j, program[(debut / 2) + j] & 0xFFFF);
                }
            } 
            
            else if (type == SECTION_TYPE_DATA) {
                if (destAddr + (size / 2) > RWMem.SIZE) {
                    return "block.aled.aledblock.errload_section_size2";
                }

                if ((debut + size) / 2 > program.length) {
                    return "block.aled.aledblock.errload_section_size3";
                }

                for (int j = 0; j < size / 2; j++) {
                    rwmem.write(destAddr + j, program[(debut / 2) + j] & 0xFFFF);
                }
            }
        }

        cpu.reset();

        return null; // No error
    }

    public RWMem getMemory() { // for screen updates
        return rwmem;
    }

    public boolean screenNeedsUpdate() {
        return true;
    }
}
