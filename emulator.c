#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

#define RWMEMORY_SIZE 65536

#undef min
#define min(a, b) ((a) < (b) ? (a) : (b))

uint16_t *xmem, *rwmem;
uint16_t sp; // stack pointer

static inline uint16_t RVAL(uint8_t source, uint16_t val) {
    switch (source) {
        case 0: return rwmem[val];
        case 1: return val;
        case 2: return rwmem[rwmem[sp] + val];
        case 3: return rwmem[rwmem[val]];
        default: {
            fprintf(stderr, "Error: Invalid source type %d\n", source);
            exit(1);
        }
    }
}

static inline void WVAL(uint16_t addr, uint8_t source, uint16_t value) {
    if (source == 0)
        printf("WVAL: [%04X] = %04X\n", addr, value);
    else if (source == 1)
        printf("WVAL: %04X = %04X pass\n", addr, value);
    else if (source == 2)
        printf("WVAL: [sp+%X] = %04X\n", addr, value);
    else if (source == 3)
        printf("WVAL: [[%04X]] = %04X\n", addr, value);

    switch (source) {
        case 0: rwmem[addr] = value; break;
        case 1: break; // cannot write to immediate value
        case 2: rwmem[rwmem[sp] + addr] = value; break;
        case 3: rwmem[rwmem[addr]] = value; break;
        default: {
            fprintf(stderr, "Error: Invalid source type %d\n", source);
            exit(1);
        }
    }
}

char *opcode_to_string(uint8_t opcode) {
    switch (opcode) {
        case 0x00: return "nop";
        case 0x01: return "mov";
        case 0x02: return "push";
        case 0x03: return "pop";
        case 0x04: return "sub";
        case 0x05: return "add";
        case 0x06: return "mul";
        case 0x07: return "div";
        case 0x08: return "mod";
        case 0x09: return "eq";
        case 0x0A: return "neq";
        case 0x0B: return "lt";
        case 0x0C: return "gt";
        case 0x0D: return "and";
        case 0x0E: return "or";
        case 0x0F: return "not";
        case 0x10: return "jmp";
        case 0x11: return "jmpr";
        case 0x12: return "out";
        case 0x13: return "in";
        case 0x14: return "sleep";
        case 0x15: return "ssp";
        case 0x16: return "dump";
        case 0xFF: return "halt";
        default:
            fprintf(stderr, "Error: Unknown opcode %02X\n", opcode);
            exit(1);
    }
}

void execute_program() {
    uint16_t pc; // program counter
    pc = sp = 0;

    while (1) {
        uint16_t instruction = xmem[pc++];

        uint8_t opcode = instruction & 0xFF00 >> 8;

        uint8_t source0 = instruction >> 14 & 0x03;
        uint8_t source1 = instruction >> 12 & 0x03;
        uint8_t source2 = instruction >> 10 & 0x03;
        uint8_t source3 = instruction >> 8 & 0x03;

        printf("PC: %04X \033[34m%s\033[0m\n", pc - 1, opcode_to_string(opcode));

        switch (opcode) {
            case 0x00: // nop
                break;
            case 0x01: // mov
                WVAL(xmem[pc], source0, RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x02: // push
                uint16_t v = RVAL(source0, xmem[pc]);
                rwmem[sp]--;
                rwmem[rwmem[sp]] = v;
                pc++;
                break;
            case 0x03: // pop
                WVAL(xmem[pc], source0, rwmem[rwmem[sp]]);
                rwmem[sp]++;
                pc++;
                break;
            case 0x04: // sub
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) - RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x05: // add
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) + RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x06: // mul
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) * RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x07: // div
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) / RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x08: // mod
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) % RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x09: // eq
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) == RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x0A: // neq
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) != RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x0B: // lt
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) < RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x0C: // gt
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) > RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x0D: // and
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) & RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x0E: // or
                WVAL(xmem[pc], source0, RVAL(source0, xmem[pc]) | RVAL(source1, xmem[pc + 1]));
                pc += 2;
                break;
            case 0x0F: // not
                WVAL(xmem[pc], source0, ~RVAL(source0, xmem[pc]));
                pc++;
                break;
            case 0x10: // jmp
                if (RVAL(source1, xmem[pc + 1]) == 0)
                    pc = RVAL(source0, xmem[pc]);
                else
                    pc += 2;
                break;
            case 0x11: // jmpr
                if (RVAL(source1, xmem[pc + 1]) == 0)
                    pc += RVAL(source0, xmem[pc]);
                else
                    pc += 2;
                break;
            case 0x12: // out
                printf("emulator does not support ports yet\n");
                pc += 2;
                break;
            case 0x13: // in
                printf("emulator does not support ports yet\n");
                pc += 2;
                break;
            case 0x14: // sleep
                printf("emulator does not support sleep yet\n");
                pc++;
                break;
            case 0x15: // ssp
                sp = RVAL(source0, xmem[pc]);
                pc++;
                break;
            case 0x16: // dump
                fprintf(stderr, "%d\n", RVAL(source0, xmem[pc]));
                pc++;
                break;
            case 0x17: // mss
                WVAL(xmem[pc] + RVAL(source1, xmem[pc + 1]), source0, RVAL(source2, xmem[pc + 2] + RVAL(source3, xmem[pc + 3])));
            case 0xFF: // halt
                return;
            default:
                printf("Unknown opcode: %02X\n", opcode);
                return;
        }

        // print the beginning of the stack
        printf("\033[90m[ ");
        for (int i = 0; i < 5; i++) {
            printf("%04X ", rwmem[rwmem[sp] + i]);
        }
        printf("]\033[0m\n");

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


    xmem = calloc(RWMEMORY_SIZE, sizeof(uint16_t));
    rwmem = calloc(RWMEMORY_SIZE, sizeof(uint16_t));

    if (xmem == NULL || rwmem == NULL) {
        perror("Failed to allocate memory");
        fclose(bytecode);
        return 1;
    }

    size = min(size, (long)(RWMEMORY_SIZE * sizeof(uint16_t)));
    if ((long) fread(xmem, 1, size, bytecode) != size) {
        perror("Failed to read bytecode");
        free(xmem);
        fclose(bytecode);
        return 1;
    }

    fclose(bytecode);

    execute_program();

    free(rwmem);
    free(xmem);

    return 0;
}
