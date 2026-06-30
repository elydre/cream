"""
opcode (8 bit)  sources (2 * 2bit)  4bit padding  val1/mem1 (16 bit)  val2/mem2 (16 bit)
000000          0000                0000          0000000000000000    0000000000000000


opcodes:
nop

mov   [arg] [arg]

push  [arg]
pop   [arg]

sub [arg] [arg]
add [arg] [arg]
mul [arg] [arg]
div [arg] [arg]
mod [arg] [arg]

eq  [arg] [arg]
neq [arg] [arg]
lt  [arg] [arg]
gt  [arg] [arg]

and [arg] [arg]
or  [arg] [arg]
not [arg]

jmp [code addr] [arg] // jump to code addr if arg == 0

out [port] [arg] // output arg to port
in  [port] [mem] // input from port to mem

sleep [arg] // sleep for arg ticks

ssp   [arg] // set stack pointer to arg

dump  [arg] // print arg

hlt
"""

class opcode:
    def __init__(self, name, opcode, argc):
        self.name = name
        self.opcode = opcode
        self.argc = argc

OPCODES = [
    opcode("nop",   0x00, 0),
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
    opcode("ssp",   0x14, 1),
    opcode("dump",  0x15, 1),
    opcode("hlt",   0xFF, 0),
]

MEMORY_SIZE = 65536 - (80 * 25)

NEW_VAR = "$"

SPE = (',', '(', ')', ':', '=', '+', '-', '*', '/', '%', '<', '>', '~~')

class variable:
    def __init__(self, name, ptrlvl, offset):
        self.name = name
        self.ptrlvl = ptrlvl
        self.offset = offset

    def add_to_local_vars(self):
        local_vars["main"].append(self)

class func:
    def __init__(self, name, args, return_type, isasm = False):
        self.name = name
        self.args = args
        self.return_type = return_type
        self.isasm = isasm

ASM_FUNCS = [
    func("out", [0, 0], None, True),
    func("in", [0, 1], None, True),
    func("sleep", [0], None, True),
    func("dump", [0], None, True)
]

global CURRENT_LNO
CURRENT_LNO = 0

def tokenize_line(line):
    tokens = []
    current_token = ""
    for char in line:
        if char.isspace():
            if not current_token:
                continue
            tokens.append(current_token)
            current_token = ""

        elif char == "=" and not current_token and tokens and tokens[-1] in SPE:
            tokens[-1] += char
        
        elif char in SPE:
            if current_token:
                tokens.append(current_token)
                current_token = ""
            tokens.append(char)
        
        else:
            current_token += char

    if current_token:
        tokens.append(current_token)

    return tokens

def say_error(message):
    print(f"Error line {CURRENT_LNO}: {message}")
    exit(1)

def is_number(s):
    try:
        int(s)
        return True
    except ValueError:
        return False

def to_number(s):
    try:
        return int(s)
    except ValueError:
        say_error(f"Bad number: {s}")

def is_variable(s, scope = "main"):
    return s in [e.name for e in local_vars[scope]]

def get_variable(s, scope = "main"):
    if not is_variable(s, scope):
        say_error(f"Unknown variable: {s}")
    
    return next(e for e in local_vars[scope] if e.name == s)

global STACK_PTR, STACK_SIZE
COND_RES = MEMORY_SIZE - 1
STACK_PTR = MEMORY_SIZE - 2
STACK_DEBUT = STACK_PTR
STACK_SIZE = 0

class output_code:
    class instruction:
        def __init__(self, opcode, sr1, val1, sr2, val2, iscomment):
            self.iscomment = iscomment

            if iscomment:
                self.string = opcode
                self.psize = 0
                self.bytes = bytearray()
                return

            if not any(e.name == opcode for e in OPCODES):
                say_error(f"(Internal) Unknown opcode: {opcode}")

            self.string = f"{opcode} "
            argc = 0

            if sr1 != None:
                if sr1 == 0: # val1 is a memory address
                    self.string += f"[{hex(val1)[2:]}] "
                if sr1 == 1: # val1 is a value
                    self.string += f"{hex(val1)[2:]} "
                if sr1 == 2: # val1 is a stack pointer offset
                    self.string += f"[sp+{hex(val1)[2:]}] "
                argc += 1

            if sr2 != None:
                if sr2 == 0: # val2 is a memory address
                    self.string += f"[{hex(val2)[2:]}] "
                if sr2 == 1: # val2 is a value
                    self.string += f"{hex(val2)[2:]} "
                if sr2 == 2: # val2 is a stack pointer offset
                    self.string += f"[sp+{hex(val2)[2:]}] "
                argc += 1

            self.string = self.string.strip()

            op = next(e for e in OPCODES if e.name == opcode)

            if argc != op.argc:
                say_error(f"(Internal) Bad number of arguments for opcode {opcode}\nExpected {op.argc}, got {argc}")

            b = bytearray()
            b.append(op.opcode)
            b.append(((sr1 or 0) << 4) | (sr2 or 0))
            if sr1 != None:
                b += (val1 or 0).to_bytes(2, byteorder='little')
            if sr2 != None:
                b += (val2 or 0).to_bytes(2, byteorder='little')

            self.psize = len(b) // 2
            self.bytes = b

    def __init__(self):
        self.instructions = []
        self.psize = 0

    def add(self, opcode, sr1 = None, val1 = None, sr2 = None, val2 = None):
        instr = self.instruction(opcode, sr1, val1, sr2, val2, iscomment = False)
        self.instructions.append(instr)
        self.psize += instr.psize

    def comment(self, comment):
        instr = self.instruction(comment, None, None, None, None, iscomment = True)
        self.instructions.append(instr)

    def push(self, other):
        self.instructions += other.instructions
        self.psize += other.psize

    def dump(self):
        pc = 0
        for instr in self.instructions:
            if instr.iscomment:
                print(f"\033[32m{instr.string}\033[0m")
            else:
                print(f"\033[34m{hex(pc)[2:].zfill(4)}: {instr.string}\033[0m")
            pc += instr.psize

    def write(self, file):
        for instr in self.instructions:
            file.write(instr.bytes)


def op_calculate_rpn(rpn: list):
    output = output_code()

    global STACK_SIZE

    if not rpn:
        say_error("Empty RPN expression")

    stack_size = 0

    for token in rpn:
        if is_number(token):
            output.add("push",
                   1, to_number(token))
            stack_size += 1
        
        elif is_variable(token):
            v = get_variable(token)
            output.add("push",
                   2, (STACK_SIZE + stack_size) - v.offset)
            stack_size += 1

        elif token in ['+', '-', '*', '/', '%', '==', '!=', '<', '>']:
            stack_size -= 1
            if token == '+':
                output.add("add",
                        2, 1, 2, 0)
            elif token == '-':
                output.add("sub",
                        2, 1, 2, 0)
            elif token == '*':
                output.add("mul",
                        2, 1, 2, 0)
            elif token == '/':
                output.add("div",
                        2, 1, 2, 0)
            elif token == '%':
                output.add("mod",
                        2, 1, 2, 0)
            elif token == '==':
                output.add("eq",
                        2, 1, 2, 0)
            elif token == '!=':
                output.add("neq",
                        2, 1, 2, 0)
            elif token == '<':
                output.add("lt",
                        2, 1, 2, 0)
            elif token == '>':
                output.add("gt",
                        2, 1, 2, 0)

            output.add("pop",
                   1, 0)

    if stack_size != 1:
        say_error("Invalid RPN expression: stack size is not 1 after evaluation")

    STACK_SIZE += 1

    return output


def op_call_asmfunc(f, variables):
    output = output_code()

    if len(variables) != len(f.args):
        say_error(f"Wrong number of arguments for function {f.name}\nSyntax example: {f.name}({', '.join(['var' + str(i + 1) for i in range(len(f.args))])})")

    if len(f.args) == 0:
        output.add(f.name)

    elif len(f.args) == 1:
        output.add(f.name,
                2, STACK_SIZE - variables[0].offset)
        
    elif len(f.args) == 2:
        output.add(f.name,
                2, STACK_SIZE - variables[0].offset,
                2, STACK_SIZE - variables[1].offset)

    else:
        say_error(f"(Internal) Function {f.name} has more than 2 arguments")
    
    return output

def op_init():
    output = output_code()
    output.comment("\nProgram initialization")

    output.add("ssp",
            1, STACK_PTR)
    output.add("mov",
            0, STACK_PTR,
            1, STACK_DEBUT)
    
    return output

def op_fini():
    output = output_code()
    output.comment("\nProgram finalization")

    output.add("hlt")

    return output

prog = """
$ var
var = 7

dump(var)
""".strip()

local_vars = {"main": []}

def compile_line(lines, current_line):
    global CURRENT_LNO

    CURRENT_LNO, tokens = lines[current_line]

    global STACK_SIZE

    print(f"Tokens: {tokens}")
    output = output_code()
    output.comment(f"\nl{CURRENT_LNO:03}  {' '.join(tokens)}")

    if tokens[0] == NEW_VAR:
        ptrlvl = 0
        while tokens[ptrlvl + 1] == '*':
            ptrlvl += 1

        if len(tokens) < 2 + ptrlvl:
            say_error(f"Bad variable declaration\nSyntax example: {NEW_VAR} var_name")
        
        var_name = tokens[1 + ptrlvl]

        old_offset = local_vars["main"][-1].offset if local_vars["main"] else 0
        v = variable(var_name, ptrlvl, old_offset + 1)
        v.add_to_local_vars()

        STACK_SIZE += 1
        output.add("push",
            1, 0)

    elif is_variable(tokens[0]):
        if len(tokens) < 3 or tokens[1] != '=':
            say_error(f"Bad variable assignment\nSyntax example: var_name = 123")

        v = get_variable(tokens[0])

        # reverse polish notation (RPN) expression
        output.push(op_calculate_rpn(tokens[2:]))

        # move the result from the stack to the variable's memory location
        output.add("pop",
            2, STACK_SIZE - v.offset)
        STACK_SIZE -= 1


    elif tokens[0] in [e.name for e in ASM_FUNCS]:
        f = next(e for e in ASM_FUNCS if e.name == tokens[0])

        variables = []
        for i, arg in enumerate(tokens[1:], start=1):
            if arg in [',', '(', ')']:
                continue
            if tokens[i + 1] not in [',', ')']:
                say_error(f"Only variables are allowed as arguments\nSyntax example: {f.name}({', '.join(['var' + str(i + 1) for i in range(len(f.args))])})")

            v = get_variable(arg)
            variables.append(v)

        output.push(op_call_asmfunc(f, variables))

    elif tokens[0] == "if":
        if len(tokens) < 2:
            say_error(f"Bad syntax\nSyntax example: if var == 0")

        # reverse polish notation (RPN) expression
        output.push(op_calculate_rpn(tokens[1:]))

        # pop the result from the stack to the conditional result memory location
        output.add("pop",
            0, COND_RES)
        STACK_SIZE -= 1

    else:
        say_error(f"Bad syntax\nUnknown command or variable: {tokens[0]}")

    return (output, 1)

def compile(lines):
    global CURRENT_LNO

    output = op_init()

    tokens_lines = []

    for lno, line in enumerate(prog.splitlines(), start=1):
        CURRENT_LNO = lno

        line = line.strip()
        tokens = tokenize_line(line)

        if not tokens:
            continue

        tokens_lines.append((lno, tokens))


    current_line = 0
    while current_line < len(tokens_lines):        
        sub_output, to_skip = compile_line(tokens_lines, current_line)

        output.push(sub_output)
        current_line += to_skip

    output.push(op_fini())

    return output

#=======================================


ofile = open("output.bin", "wb")

if not ofile:
    exit("Could not open output file")

main_output = compile(prog)

main_output.dump()
main_output.write(ofile)

ofile.close()
