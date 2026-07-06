# cream

custom instruction set, esoteric programming language, single pass compiler, and emulator

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
- [x] output file format
- [x] heap variables (static)
- [x] heap strings
- [x] comments
- [x] functions
- [ ] preprocessor
- [ ] heap arrays
- [ ] sub stack scope
- [ ] structs
- [ ] some optimizations
- [ ] multiple source files

### Extra

- [x] C emulator
- [x] basic command line interface for compiler
- [x] add screen to the emulator
- [ ] langage documentation
- [ ] create a basic operating system

## programing language

```c
// this is a comment

/* everything is a u16, the language is not typed
*/
: var             // variable declaration
: ptr str i       // multiple variable declaration

$ hello           // static variable declaration (set to 0)
$ var = 3         // static variable declaration with initialization

/* the language use RPN (Reverse Polish Notation)
*/
var = 3           // variable assignment
var = 3 4 +       // assignment from RPN expression (3 + 4)
var = i           // assignment from another variable

/* strings are stored in the heap
** the variable is a pointer to the string
*/
str = "hello"     // string assignment

/* pointers are memory accesses to an address
*/
var = [0]         // variable assignment from memory address 0
[0] = var         // memory address 0 assignment from variable
var = [str]       // variable assignment from string pointer
var = [str 1 +]   // assignment from string pointer + 1 (next character)

/* the built-in function alloca() allocates memory
** on the stack and returns a pointer to it
*/
ptr = alloca(10)  // allocate 10 bytes on the stack
[ptr] = 3         // assign 3 to the first byte of the allocation
```

## opcode (may be subject to change)

```
opcode (8 bit)  sources (4 * 2bit)  [  arg0 (16 bit)   ] ... [ arg3 (16 bit)    ]
000000          00000000            [ 0000000000000000 ] ... [ 0000000000000000 ]

(arguments quantity is determined by the opcode)
```

| opcode | arguments   | description                |
| ------ | ----------- | ---------------------------|
|  nop   |             | no operation               |
|        |             |                            |
|  mov   | `a` `b`     | `a <- b`                   |
|        |             |                            |
|  push  | `a`         | `sp--`, `[sp] <- a`        |
|  pop   | `a`         | `a <- [sp]`, `sp++`        |
|        |             |                            |
|  sub   | `a` `b`     | `a <- a - b`               |
|  add   | `a` `b`     | `a <- a + b`               |
|  mul   | `a` `b`     | `a <- a * b`               |
|  div   | `a` `b`     | `a <- a / b`               |
|  mod   | `a` `b`     | `a <- a % b`               |
|        |             |                            |
|  eq    | `a` `b`     | `a <- a == b`              |
|  neq   | `a` `b`     | `a <- a != b`              |
|  lt    | `a` `b`     | `a <- a < b`               |
|  gt    | `a` `b`     | `a <- a > b`               |
|        |             |                            |
|  and   | `a` `b`     | `a <- a && b`              |
|  band  | `a` `b`     | `a <- a & b`               |
|  bor   | `a` `b`     | `a <- a bor b` (md sorry)  |
|        |             |                            |
|  jmp   | `a` `b`     | `pc  = a if b == 0`        |
|  jmpr  | `a` `b`     | `pc += a if b == 0`        |
|        |             |                            |
|  out   | `port` `a`  | output `a` to `port`       |
|  in    | `a` `port`  | input from `port` to `a`   |
|        |             |                            |
|  sleep | `a`         | sleep for `a` ticks        |
|        |             |                            |
|  ssp   | `a`         | `sp <- a`                  |
|        |             |                            |
|  mss   | `A` `a` `B` `b` | `[A + a] <- [B + b]`   |
|  pushs | `A` `a`     | `sp--`, `[sp] <- [A + a]`  |
|  pops  | `A` `a`     | `[A + a] <- [sp]`, `sp++`  |
|        |             |                            |
| memset | `a` `b` `c` | `memset(addr=a val=b s=c)` |
| memmov | `a` `b` `c` | `memmov(dest=a src=b s=c)` |
|        |             |                            |
|  hlt   |             | halt the computer          |

each argument can be one of the following:

| source | description | explanation     |
| ------ | ----------- | --------------- |
|  0     | `[a]`       | memory address  |
|  1     | `a`         | value           |
|  2     | `sp+a`      | stack address   |
|  3     | `?`         | unused          |

## Compiled file format

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
