package com.example.computer;

public class Cpu {
    private final XMem xmem;
    private final RWMem rwmem;
    private int pc = 0;
    private int sp = 0;

    private boolean halted = false;

    public Cpu(XMem xmem, RWMem rwmem) {
        this.rwmem = rwmem;
        this.xmem = xmem;
    }

    public boolean isHalted() {
        return halted;
    }

    public void reset() {
        pc = 0;
        halted = false;
    }

    private int RVAL(int source, int val) {
        switch (source) {
            case 0: return rwmem.read(val);
            case 1: return val;
            case 2: return rwmem.read(rwmem.read(sp) + val);
            // case 3: return rwmem[rwmem[val]];
            default: return 0; // should not happen
        }
    }

    private void WVAL(int addr, int source, int value) {
        switch (source) {
            case 0: rwmem.write(addr, value); break;
            case 1: break; // cannot write to immediate value
            case 2: rwmem.write(rwmem.read(sp) + addr, value); break;
            // case 3: rwmem[rwmem[addr]] = value; break;
            default: return; // should not happen
        }
    }

    private void port_out(int port, int value) {
    }

    private int port_in(int port) {
        return 0;
    }

    public void tick() {
        int instruction = xmem.read(pc++);

        int opcode = instruction & 0xFF00 >> 8;

        int source0 = instruction >> 14 & 0x03;
        int source1 = instruction >> 12 & 0x03;
        int source2 = instruction >> 10 & 0x03;
        int source3 = instruction >> 8 & 0x03;

        switch (opcode) {
            case 0x00: // nop
                break;
            case 0x01: // mov
                WVAL(xmem.read(pc), source0, RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x02: // push
                int v = RVAL(source0, xmem.read(pc));
                rwmem.write(sp, rwmem.read(sp) - 1);
                rwmem.write(rwmem.read(sp), v);
                pc++;
                break;
            case 0x03: // pop
                WVAL(xmem.read(pc), source0, rwmem.read(rwmem.read(sp)));
                rwmem.write(sp, rwmem.read(sp) + 1);
                pc++;
                break;
            case 0x04: // sub
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) - RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x05: // add
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) + RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x06: // mul
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) * RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x07: // div
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) / RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x08: // mod
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) % RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x09: // eq
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) == RVAL(source1, xmem.read(pc + 1)) ? 1 : 0);
                pc += 2;
                break;
            case 0x0A: // neq
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) != RVAL(source1, xmem.read(pc + 1)) ? 1 : 0);
                pc += 2;
                break;
            case 0x0B: // lt
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) < RVAL(source1, xmem.read(pc + 1)) ? 1 : 0);
                pc += 2;
                break;
            case 0x0C: // gt
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) > RVAL(source1, xmem.read(pc + 1)) ? 1 : 0);
                pc += 2;
                break;
            case 0x0D: // and
                WVAL(xmem.read(pc), source0, (RVAL(source0, xmem.read(pc)) != 0) && (RVAL(source1, xmem.read(pc + 1)) != 0) ? 1 : 0);
                pc += 2;
                break;
            case 0x0E: // band
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) & RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x0F: // bor
                WVAL(xmem.read(pc), source0, RVAL(source0, xmem.read(pc)) | RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x10: // jmp
                if (RVAL(source1, xmem.read(pc + 1)) == 0)
                    pc = RVAL(source0, xmem.read(pc));
                else
                    pc += 2;
                break;
            case 0x11: // jmpr
                if (RVAL(source1, xmem.read(pc + 1)) == 0)
                    pc += RVAL(source0, xmem.read(pc));
                else
                    pc += 2;
                break;
            case 0x12: // out
                port_out(RVAL(source0, xmem.read(pc)), RVAL(source1, xmem.read(pc + 1)));
                pc += 2;
                break;
            case 0x13: // in
                WVAL(xmem.read(pc), source0, port_in(RVAL(source1, xmem.read(pc + 1))));
                pc += 2;
                break;
            case 0x14: // sleep
                // todo
                pc++;
                break;
            case 0x15: // ssp
                sp = RVAL(source0, xmem.read(pc));
                pc++;
                break;
            case 0x16: // mss
            {
                int dest = RVAL(source0, xmem.read(pc)) + RVAL(source1, xmem.read(pc + 1));
                int src  = RVAL(source2, xmem.read(pc + 2)) + RVAL(source3, xmem.read(pc + 3));

                rwmem.write(dest, rwmem.read(src));
                pc += 4;
                break;
            }
            case 0x17: // pushs
                rwmem.write(sp, rwmem.read(sp) - 1);
                rwmem.write(rwmem.read(sp), rwmem.read(RVAL(source0, xmem.read(pc)) + RVAL(source1, xmem.read(pc + 1))));
                pc += 2;
                break;
            case 0x18: // pops
                rwmem.write(RVAL(source0, xmem.read(pc)) + RVAL(source1, xmem.read(pc + 1)), rwmem.read(rwmem.read(sp)));
                rwmem.write(sp, rwmem.read(sp) + 1);
                pc += 2;
                break;
            case 0x19: // memset
            {
                int addr = RVAL(source0, xmem.read(pc));
                int val  = RVAL(source1, xmem.read(pc + 1));
                int size = RVAL(source2, xmem.read(pc + 2));

                for (int i = 0; i < size && (addr + i) < XMem.SIZE; i++) {
                    rwmem.write(addr + i, val);
                }
                pc += 3;
                break;
            }

            case 0x1A: // memmov
            {
                int dest = RVAL(source0, xmem.read(pc));
                int src  = RVAL(source1, xmem.read(pc + 1));
                int size = RVAL(source2, xmem.read(pc + 2));

                if (src < dest) {
                    for (int i = size - 1; i >= 0; i--) {
                        if ((src + i) < XMem.SIZE && (dest + i) < XMem.SIZE) {
                            rwmem.write(dest + i, rwmem.read(src + i));
                        }
                    }
                } else {
                    for (int i = 0; i < size; i++) {
                        if ((src + i) < XMem.SIZE && (dest + i) < XMem.SIZE) {
                            rwmem.write(dest + i, rwmem.read(src + i));
                        }
                    }
                }
                pc += 3;
                break;
            }
            case 0xFF: // halt
                return;
            default:
                return;
        }

        if (pc >= XMem.SIZE - 10)
            return;
    }

    public boolean screenNeedsUpdate() {
        return true;
    }
}
