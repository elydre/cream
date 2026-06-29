#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

#define RWMEMORY_SIZE 65536

uint16_t *xmemory, *rwmemory;
uint16_t pc; // program counter
uint16_t sp; // stack pointer

static inline uint16_t RVAL(uint8_t source) {
    switch (source) {
        case 0: return rwmemory[xmemory[pc++]];
        case 1: return xmemory[pc++];
        case 2: return rwmemory[sp - xmemory[pc++]];
        default: exit(1);
    }
}

static inline void WVAL(uint16_t old_pc, uint8_t source, uint16_t value) {
    switch (source) {
        case 0: rwmemory[xmemory[old_pc]] = value; break;
        case 1: exit(1); break;
        case 2: rwmemory[sp - xmemory[old_pc]] = value; break;
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
            /*
            OPCODES = [
                opcode("ssp",   0x00, 1),
                opcode("mov",   0x01, 2),
                opcode("push",  0x02, 1),
                opcode("pop",   0x03, 1),
                opcode("sub",   0x04, 2),
                opcode("add",   0x05, 2),
                opcode("mul",   0x06, 2),
                opcode("div",   0x07, 2),
                opcode("mod",   0x08, 2),
                opcode("eq",    0x09, 2),
                opcode("neq",   0x0A, 2),
                opcode("lt",    0x0B, 2),
                opcode("gt",    0x0C, 2),
                opcode("and",   0x0D, 2),
                opcode("or",    0x0E, 2),
                opcode("not",   0x0F, 1),
                opcode("jmp",   0x10, 2),
                opcode("out",   0x11, 2),
                opcode("in",    0x12, 2),
                opcode("sleep", 0x13, 1),
            ]
            */
            case 0x00: // ssp
                sp = RVAL(source1);
                break;
            case 0x01: // mov
                WVAL(pc++, source1, RVAL(source2));
                break;
            case 0x02: // push
                rwmemory[sp++] = RVAL(source1);
                break;
            default:
                printf("Unknown opcode: %02X\n", opcode);
                return;
        }
    }

    if (pc >= RWMEMORY_SIZE - 10)
        return;
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

    xmemory = malloc(size);
    rwmemory = calloc(RWMEMORY_SIZE, sizeof(uint16_t));

    if (xmemory == NULL || rwmemory == NULL) {
        perror("Failed to allocate memory");
        fclose(bytecode);
        return 1;
    }

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
