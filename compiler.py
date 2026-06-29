"""
opcode (8 bit)  sources (2 * 2bit)  4bit padding  val1/mem1 (16 bit)  val2/mem2 (16 bit)
000000          0000                0000          0000000000000000    0000000000000000


opcodes:
ssp   [arg] // set stack pointer to arg


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

jmp [code addr] [arg] // jump to code addr if arg != 0

out [port] [arg] // output arg to port
in  [port] [mem] // input from port to mem

sleep [arg] // sleep for arg ticks

"""

class opcode:
    def __init__(self, name, opcode, argc):
        self.name = name
        self.opcode = opcode
        self.argc = argc

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

MEMORY_SIZE = 65536 - (80 * 25)

NEW_VAR = "$"

SPE = (',', '(', ')', ':', '=', '+', '-', '*', '/', '%', '<', '>')

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

def count_args(tokens):
    count = 0
    
    if len(tokens) < 2 or tokens[0] != '(' or tokens[-1] != ')':
        return -1
    
    for token in tokens[1:-1]:
        if token == ',':
            count += 1

    return count + 1

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
STACK_PTR = MEMORY_SIZE - 1
STACK_DEBUT = STACK_PTR
STACK_SIZE = 0

ofile = open("output.bin", "wb")

if not ofile:
    exit("Could not open output file")

def output(opcode, sr1 = None, val1 = None, sr2 = None, val2 = None):
    print(end = "\033[34m")
    print(end = f"{opcode} ")
    if sr1 != None:
        if sr1 == 0: # val1 is a memory address
            print(end = f"[{hex(val1)[2:]}] ")
        if sr1 == 1: # val1 is a value
            print(end = f"{hex(val1)[2:]} ")
        if sr1 == 2: # val1 is a stack pointer offset
            print(end = f"[sp+{hex(val1)[2:]}] ")

    if sr2 != None:
        if sr2 == 0: # val2 is a memory address
            print(end = f"[{hex(val2)[2:]}] ")
        if sr2 == 1: # val2 is a value
            print(end = f"{hex(val2)[2:]} ")
        if sr2 == 2: # val2 is a stack pointer offset
            print(end = f"[sp+{hex(val2)[2:]}] ")

    print("\033[0m")

    op = next(e for e in OPCODES if e.name == opcode)
    b = bytearray()
    b.append(op.opcode)
    b.append(((sr1 or 0) << 4) | (sr2 or 0))
    b += (val1 or 0).to_bytes(2, byteorder='little')
    b += (val2 or 0).to_bytes(2, byteorder='little')
    ofile.write(b)


def op_grow_stack():
    global STACK_SIZE
    STACK_SIZE += 1
    output("push",
           1, 0)

def op_calculate_rpn(rpn: list):
    global STACK_SIZE

    if not rpn:
        say_error("Empty RPN expression")

    stack_size = 0

    for token in rpn:
        if is_number(token):
            stack_size += 1
            output("push",
                   1, to_number(token))
        
        elif is_variable(token):
            stack_size += 1
            v = get_variable(token)
            output("push",
                   2, STACK_SIZE - v.offset)

        elif token in ['+', '-', '*', '/', '%', '==', '!=', '<', '>']:
            stack_size -= 1
            if token == '+':
                output("add",
                        2, 1, 2, 0)
            elif token == '-':
                output("sub",
                        2, 1, 2, 0)
            elif token == '*':
                output("mul",
                        2, 1, 2, 0)
            elif token == '/':
                output("div",
                        2, 1, 2, 0)
            elif token == '%':
                output("mod",
                        2, 1, 2, 0)
            elif token == '==':
                output("eq",
                        2, 1, 2, 0)
            elif token == '!=':
                output("neq",
                        2, 1, 2, 0)
            elif token == '<':
                output("lt",
                        2, 1, 2, 0)
            elif token == '>':
                output("gt",
                        2, 1, 2, 0)

            output("pop")

    if stack_size != 1:
        say_error("Invalid RPN expression: stack size is not 1 after evaluation")

    STACK_SIZE += 1

def op_init_stack():
    output("ssp",
            1, STACK_PTR)
    output("mov",
            0, STACK_PTR,
            1, STACK_DEBUT)

prog = """
$ var
$ coucou
var = 1 6 +
coucou = var
out(0, var)
""".strip()

local_vars = {"main": []}

op_init_stack()

for lno, line in enumerate(prog.splitlines(), start=1):
    CURRENT_LNO = lno

    line = line.strip()
    tokens = tokenize_line(line)
    if not tokens:
        continue
    print(f"Tokens: {tokens}")

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

        op_grow_stack()

    elif is_variable(tokens[0]):
        if len(tokens) < 3 or tokens[1] != '=':
            say_error(f"Bad variable assignment\nSyntax example: var_name = 123")

        v = get_variable(tokens[0])

        # reverse polish notation (RPN) expression
        op_calculate_rpn(tokens[2:])

        # move the result from the stack to the variable's memory location
        output("pop",
               2, STACK_SIZE - v.offset)


    elif tokens[0] in [e.name for e in ASM_FUNCS]:
        f = next(e for e in ASM_FUNCS if e.name == tokens[0])

        if count_args(tokens[1:]) != len(f.args):
            say_error(f"Bad function call\nSyntax example: {f.name}({', '.join(['var' + str(i + 1) for i in range(len(f.args))])})")

    else:
        say_error(f"Bad syntax\nUnknown command or variable: {tokens[0]}")
