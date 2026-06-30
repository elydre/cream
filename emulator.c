#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

#define RWMEMORY_SIZE 65536

#undef min
#define min(a, b) ((a) < (b) ? (a) : (b))

uint16_t *xmemory, *rwmemory;
uint16_t pc; // program counter
uint16_t sp; // stack pointer

static inline uint16_t RVAL(uint8_t source) {
    switch (source) {
        case 0: return rwmemory[xmemory[pc++]];
        case 1: return xmemory[pc++];
        case 2: return rwmemory[rwmemory[sp] + xmemory[pc++]];
        default: exit(1);
    }
}

static inline void WVAL(uint16_t old_pc, uint8_t source, uint16_t value) {
    printf("WVAL: old_pc=%04X, source=%d, value=%04X\n", old_pc, source, value);
    switch (source) {
        case 0: rwmemory[xmemory[old_pc]] = value; break;
        case 1: exit(1); break;
        case 2: rwmemory[rwmemory[sp] + xmemory[old_pc]] = value; break;
        default: exit(1);
    }
}


void execute_program() {
    pc = sp = 0;

    while (1) {
        uint16_t instruction = xmemory[pc++];

        uint8_t opcode = instruction & 0xFF00 >> 8;

/*
        opcode (8 bit)  sources (2 * 2bit)  4bit padding  val1/mem1 (16 bit)  val2/mem2 (16 bit)
        000000          0000                0000          0000000000000000    0000000000000000
*/

        uint8_t source1 = instruction >> 12 & 0x03;
        uint8_t source2 = instruction >> 8 & 0x03;

        printf("PC: %04X, Opcode: %02X, Source1: %d, Source2: %d\n", pc - 1, opcode, source1, source2);

        switch (opcode) {
            case 0x00: // nop
                break;
            case 0x01: // mov
                WVAL(pc++, source1, RVAL(source2));
                break;
            case 0x02: // push
                rwmemory[rwmemory[sp]] = RVAL(source1);
                rwmemory[sp]--;
                break;
            case 0x03: // pop
                rwmemory[sp]++;
                WVAL(pc++, source1, rwmemory[rwmemory[sp]]);
                break;
            case 0x04: // sub
                WVAL(pc++, source1, RVAL(source1) - RVAL(source2));
                break;
            case 0x05: // add
                WVAL(pc++, source1, RVAL(source1) + RVAL(source2));
                break;
            case 0x06: // mul
                WVAL(pc++, source1, RVAL(source1) * RVAL(source2));
                break;
            case 0x07: // div
                WVAL(pc++, source1, RVAL(source1) / RVAL(source2));
                break;
            case 0x08: // mod
                WVAL(pc++, source1, RVAL(source1) % RVAL(source2));
                break;
            case 0x09: // eq
                WVAL(pc++, source1, RVAL(source1) == RVAL(source2));
                break;
            case 0x0A: // neq
                WVAL(pc++, source1, RVAL(source1) != RVAL(source2));
                break;
            case 0x0B: // lt
                WVAL(pc++, source1, RVAL(source1) < RVAL(source2));
                break;
            case 0x0C: // gt
                WVAL(pc++, source1, RVAL(source1) > RVAL(source2));
                break;
            case 0x0D: // and
                WVAL(pc++, source1, RVAL(source1) & RVAL(source2));
                break;
            case 0x0E: // or
                WVAL(pc++, source1, RVAL(source1) | RVAL(source2));
                break;
            case 0x0F: // not
                WVAL(pc++, source1, ~RVAL(source1));
                break;
            case 0x10: // jmp
                pc = RVAL(source1);
                break;
            case 0x11: // out
                printf("emulator does not support ports yet\n");
                break;
            case 0x12: // in
                printf("emulator does not support ports yet\n");
                break;
            case 0x13: // sleep
                printf("emulator does not support sleep yet\n");
                break;
            case 0x14: // ssp
                sp = RVAL(source1);
                break;
            case 0x15: // dump
                printf("%x\n", RVAL(source1));
                break;
            case 0xFF: // halt
                return;
            default:
                printf("Unknown opcode: %02X\n", opcode);
                return;
        }

        if (pc >= RWMEMORY_SIZE - 10)
            return;
    }

}
            

int main(void) {
    FILE *bytecode = fopen("output.bin", "rb");

    if (bytecode == NULL) {
        perror("Failed to open 'output.bin' you need to run the compiler first");
        return 1;
    }

    // load the bytecode into memory
    fseek(bytecode, 0, SEEK_END);
    long size = ftell(bytecode);
    fseek(bytecode, 0, SEEK_SET);


    xmemory = calloc(RWMEMORY_SIZE, sizeof(uint16_t));
    rwmemory = calloc(RWMEMORY_SIZE, sizeof(uint16_t));

    if (xmemory == NULL || rwmemory == NULL) {
        perror("Failed to allocate memory");
        fclose(bytecode);
        return 1;
    }

    size = min(size, (long)(RWMEMORY_SIZE * sizeof(uint16_t)));
    if ((long) fread(xmemory, 1, size, bytecode) != size) {
        perror("Failed to read bytecode");
        free(xmemory);
        fclose(bytecode);
        return 1;
    }

    fclose(bytecode);

    execute_program();

    free(rwmemory);
    free(xmemory);

    return 0;
}
