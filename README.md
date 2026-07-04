# cream

custom instruction set, esoteric programming language, compiler, and emulator

## Todo

### Compiler

- [x] variable on stack
- [x] RPN calculator
- [x] if statements
- [x] while loops
- [x] pointers operations
- [x] built-in functions
- [x] alloca
- [x] break and continue
- [x] for loops
- [x] elif/else statements
- [ ] new output format
- [ ] heap arrays and variables
- [ ] heap strings
- [ ] sub stack scope
- [ ] functions
- [ ] structs
- [ ] some optimizations

### Extra

- [x] C emulator
- [x] basic command line interface for compiler
- [ ] langage documentation
- [ ] add text mode screen to the emulator
- [ ] create a basic operating system

## opcode (may be subject to change)

```
opcode (8 bit)  sources (4 * 2bit)  [  arg0 (16 bit)   ] ... [ arg3 (16 bit)    ]
000000          00000000            [ 0000000000000000 ] ... [ 0000000000000000 ]

(arguments quantity is determined by the opcode)
```

| opcode | arguments   | description               |
| ------ | ----------- | ------------------------- |
|  nop   |             | no operation              |
|        |             |                           |
|  mov   | `a` `b`     | `a <= b`                  |
|        |             |                           |
|  push  | `a`         | `sp--`, `[sp] <= a`       |
|  pop   | `a`         | `a <= [sp]`, `sp++`       |
|        |             |                           |
|  sub   | `a` `b`     | `a <= a - b`              |
|  add   | `a` `b`     | `a <= a + b`              |
|  mul   | `a` `b`     | `a <= a * b`              |
|  div   | `a` `b`     | `a <= a / b`              |
|  mod   | `a` `b`     | `a <= a % b`              |
|        |             |                           |
|  eq    | `a` `b`     | `a <= a == b`             |
|  neq   | `a` `b`     | `a <= a != b`             |
|  lt    | `a` `b`     | `a <= a < b`              |
|  gt    | `a` `b`     | `a <= a > b`              |
|        |             |                           |
|  and   | `a` `b`     | `a <= a & b`              |
|  or    | `a` `b`     | `a <= a bor b` (md sorry) |
|  not   | `a`         | `a <= !a`                 |
|        |             |                           |
|  jmp   | `a` `b`     | `pc  = a if b == 0`       |
|  jmpr  | `a` `b`     | `pc += a if b == 0`       |
|        |             |                           |
|  out   | `port` `a`  | output `a` to `port`      |
|  in    | `port` `a`  | input from `port` to `a`  |
|        |             |                           |
|  sleep | `a`         | sleep for `a` ticks       |
|        |             |                           |
|  ssp   | `a`         | `sp <= a`                 |
|        |             |                           |
|  dump  | `a`         | print `a` to stdout       |
|        |             |                           |
|  mss   | `A` `a` `B` `b` | `[A + a] <= [B + b]`  |
|  pushs | `A` `a`     | `sp--`, `[sp] <= [A + a]` |
|  pops  | `A` `a`     | `[A + a] <= [sp]`, `sp++` |
|        |             |                           |
|  hlt   |             | halt the computer         |

each argument can be one of the following:

| source | description | explanation     |
| ------ | ----------- | --------------- |
|  0     | `[a]`       | memory address  |
|  1     | `a`         | value           |
|  2     | `sp+a`      | stack address   |
|  3     | `?`         | unused          |

## Compiled file format (not implemented yet)

```
[HEADER]
magic number         (16 bit)
ARCH version         (16 bit)
section count        (16 bit)

section 0 debut      (16 bit)
section 0 size       (16 bit)
section 0 dest-addr  (16 bit)
section 0 type       (16 bit)

section 1 debut      (16 bit)
section 1 size       (16 bit)
section 1 dest-addr  (16 bit)
section 1 type       (16 bit)
...

[DATA]
section 0 data   (section 0 size * 16 bit)
section 1 data   (section 1 size * 16 bit)
...
```

`section debut` is the offset in the file where the section data starts

section type:
- 0: code (in X memory)
- 1: data (in RW memory)
