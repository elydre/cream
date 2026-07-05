#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

#define MEMORY_SIZE 65536

#undef min
#define min(a, b) ((a) < (b) ? (a) : (b))

#ifdef DEBUG
#define DEBUGF(fmt, ...) printf(fmt, ##__VA_ARGS__)
#else
#define DEBUGF(fmt, ...) do {} while (0)
#endif

uint16_t *xmem, *rwmem;
uint16_t sp; // stack pointer

static inline uint16_t RVAL(uint8_t source, uint16_t val) {
    if (source == 0)
        DEBUGF("RVAL: [%04X] = %04X\n", val, rwmem[val]);
    else if (source == 1)
        DEBUGF("RVAL: %04X = %04X pass\n", val, val);
    else if (source == 2)
        DEBUGF("RVAL: [sp+%X] = %04X\n", val, rwmem[rwmem[sp] + val]);
    else if (source == 3)
        DEBUGF("RVAL: [[%04X]] = %04X\n", val, rwmem[rwmem[val]]);

    switch (source) {
        case 0: return rwmem[val];
        case 1: return val;
        case 2: return rwmem[rwmem[sp] + val];
        // case 3: return rwmem[rwmem[val]];
        default: {
            fprintf(stderr, "Error: Invalid source type %d\n", source);
            exit(1);
        }
    }
}

static inline void WVAL(uint16_t addr, uint8_t source, uint16_t value) {
    if (source == 0)
        DEBUGF("WVAL: [%04X] = %04X\n", addr, value);
    else if (source == 1)
        DEBUGF("WVAL: %04X = %04X pass\n", addr, value);
    else if (source == 2)
        DEBUGF("WVAL: [sp+%X] = %04X\n", addr, value);
    else if (source == 3)
        DEBUGF("WVAL: [[%04X]] = %04X\n", addr, value);

    switch (source) {
        case 0: rwmem[addr] = value; break;
        case 1: break; // cannot write to immediate value
        case 2: rwmem[rwmem[sp] + addr] = value; break;
        // case 3: rwmem[rwmem[addr]] = value; break;
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
        case 0x17: return "mss";
        case 0x18: return "pushs";
        case 0x19: return "pops";
        case 0xFF: return "halt";
        default:
            fprintf(stderr, "Error: Unknown opcode 0x%02X\n", opcode);
            exit(1);
    }
}

#define PC rwmem[0xFFFF]

void execute_program() {
    PC = sp = 0;

    while (1) {
        uint16_t instruction = xmem[PC++];

        uint8_t opcode = instruction & 0xFF00 >> 8;

        uint8_t source0 = instruction >> 14 & 0x03;
        uint8_t source1 = instruction >> 12 & 0x03;
        uint8_t source2 = instruction >> 10 & 0x03;
        uint8_t source3 = instruction >> 8 & 0x03;

        DEBUGF("PC: %04X \033[34m%s\033[0m\n", PC - 1, opcode_to_string(opcode));

        switch (opcode) {
            case 0x00: // nop
                break;
            case 0x01: // mov
                WVAL(xmem[PC], source0, RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x02: // push
                uint16_t v = RVAL(source0, xmem[PC]);
                rwmem[sp]--;
                rwmem[rwmem[sp]] = v;
                PC++;
                break;
            case 0x03: // pop
                WVAL(xmem[PC], source0, rwmem[rwmem[sp]]);
                rwmem[sp]++;
                PC++;
                break;
            case 0x04: // sub
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) - RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x05: // add
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) + RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x06: // mul
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) * RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x07: // div
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) / RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x08: // mod
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) % RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x09: // eq
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) == RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x0A: // neq
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) != RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x0B: // lt
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) < RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x0C: // gt
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) > RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x0D: // and
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) & RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x0E: // or
                WVAL(xmem[PC], source0, RVAL(source0, xmem[PC]) | RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x0F: // not
                WVAL(xmem[PC], source0, ~RVAL(source0, xmem[PC]));
                PC++;
                break;
            case 0x10: // jmp
                if (RVAL(source1, xmem[PC + 1]) == 0)
                    PC = RVAL(source0, xmem[PC]);
                else
                    PC += 2;
                break;
            case 0x11: // jmpr
                if (RVAL(source1, xmem[PC + 1]) == 0)
                    PC += RVAL(source0, xmem[PC]);
                else
                    PC += 2;
                break;
            case 0x12: // out
                printf("emulator does not support ports yet\n");
                PC += 2;
                break;
            case 0x13: // in
                // simply return 2*port for now
                WVAL(xmem[PC], source0, 2 * RVAL(source1, xmem[PC + 1]));
                PC += 2;
                break;
            case 0x14: // sleep
                printf("emulator does not support sleep yet\n");
                PC++;
                break;
            case 0x15: // ssp
                sp = RVAL(source0, xmem[PC]);
                PC++;
                break;
            case 0x16: // dump
                fprintf(stderr, "0x%x (%d)\n", RVAL(source0, xmem[PC]), RVAL(source0, xmem[PC]));
                PC++;
                break;
            case 0x17: // mss
            {
                uint16_t dest = RVAL(source0, xmem[PC])     + RVAL(source1, xmem[PC + 1]);
                uint16_t src  = RVAL(source2, xmem[PC + 2]) + RVAL(source3, xmem[PC + 3]);

                DEBUGF("mss: [%04X] = [%04X] = %04X\n", dest, src, rwmem[src]);
                rwmem[dest] = rwmem[src];
                PC += 4;
                break;
            }
            case 0x18: // pushs
                // push but like mss
                rwmem[sp]--;
                rwmem[rwmem[sp]] = rwmem[(uint16_t)(RVAL(source0, xmem[PC]) + RVAL(source1, xmem[PC + 1]))];
                PC += 2;
                break;
            case 0x19: // pops
                // pop but like mss
                rwmem[(uint16_t)(RVAL(source0, xmem[PC]) + RVAL(source1, xmem[PC + 1]))] = rwmem[rwmem[sp]];
                rwmem[sp]++;
                PC += 2;
                break;
            case 0xFF: // halt
                return;
            default:
                DEBUGF("Unknown opcode: 0x%02X\n", opcode);
                return;
        }

        // print the beginning of the stack
        DEBUGF("\033[90m[ ");
        for (int i = 0; i < 5; i++) {
            if (i == 0)
                DEBUGF("%04X=", rwmem[sp] + i);
            DEBUGF("%04X ", rwmem[rwmem[sp] + i]);
        }
        DEBUGF("]\033[0m\n");

        if (PC >= MEMORY_SIZE - 10)
            return;
    }
}

typedef struct {
    uint16_t magic;
    uint16_t version;
    uint16_t section_count;
} file_header_t;

typedef struct {
    uint16_t debut;
    uint16_t size;
    uint16_t dest_addr;
    uint16_t type;
} section_header_t;

#define MAGIC_NUMBER 0xF057
#define ARCH_VERSION 1
#define MAX_SECTIONS 16

#define SECTION_TYPE_CODE 0
#define SECTION_TYPE_DATA 1

int main(void) {

    FILE *bytecode = fopen("output.bin", "rb");

    if (bytecode == NULL) {
        perror("Failed to open 'output.bin' you need to run the compiler first");
        return 1;
    }

    file_header_t header;
    
    if (fread(&header, sizeof(file_header_t), 1, bytecode) != 1) {
        perror("Failed to read file header");
        fclose(bytecode);
        return 1;
    }

    if (header.magic != MAGIC_NUMBER) {
        fprintf(stderr, "Error: Invalid magic number in bytecode file\n");
        fclose(bytecode);
        return 1;
    }

    if (header.version != ARCH_VERSION) {
        fprintf(stderr, "Error: Unsupported architecture version in bytecode file\n");
        fclose(bytecode);
        return 1;
    }

    if (header.section_count > MAX_SECTIONS) {
        fprintf(stderr, "Error: Too many sections in bytecode file\n");
        fclose(bytecode);
        return 1;
    }

    section_header_t sections[MAX_SECTIONS];

    if (fread(sections, sizeof(section_header_t), header.section_count, bytecode) != header.section_count) {
        perror("Failed to read section headers");
        fclose(bytecode);
        return 1;
    }

    xmem = calloc(MEMORY_SIZE, sizeof(uint16_t));
    rwmem = calloc(MEMORY_SIZE, sizeof(uint16_t));

    if (xmem == NULL || rwmem == NULL) {
        perror("Failed to allocate memory");
        fclose(bytecode);
        return 1;
    }

    for (int i = 0; i < header.section_count; i++) {
        if (sections[i].type != SECTION_TYPE_CODE && sections[i].type != SECTION_TYPE_DATA) {
            fprintf(stderr, "Error: Invalid section type %d in section %d\n", sections[i].type, i);
            fclose(bytecode);
            return 1;
        }
    
        if (sections[i].size % sizeof(uint16_t) != 0) {
            fprintf(stderr, "Error: Section %d size is not a multiple of 2\n", i);
            fclose(bytecode);
            return 1;
        }

        if (sections[i].dest_addr + (sections[i].size / sizeof(uint16_t)) > MEMORY_SIZE) {
            fprintf(stderr, "Error: Section %d exceeds memory bounds\n", i);
            fclose(bytecode);
            return 1;
        }
    
        if (sections[i].type == SECTION_TYPE_CODE) {
            if (fseek(bytecode, sections[i].debut, SEEK_SET) || fread(&xmem[sections[i].dest_addr], 1, sections[i].size, bytecode) != sections[i].size) {
                perror("Failed to read code section");
                fclose(bytecode);
                return 1;
            }
            printf("Loaded code section %d: %d bytes to address 0x%04X\n", i, sections[i].size, sections[i].dest_addr);
        } else if (sections[i].type == SECTION_TYPE_DATA) {
            if (fseek(bytecode, sections[i].debut, SEEK_SET) || fread(&rwmem[sections[i].dest_addr], 1, sections[i].size, bytecode) != sections[i].size) {
                perror("Failed to read data section");
                fclose(bytecode);
                return 1;
            }
            printf("Loaded data section %d: %d bytes to address 0x%04X\n", i, sections[i].size, sections[i].dest_addr);
        }
    }

    fclose(bytecode);

    printf("Starting emulation\n");

    execute_program();

    free(rwmem);
    free(xmem);

    return 0;
}
