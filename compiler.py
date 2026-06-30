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

jmp  [code addr] [arg] // jump to (code addr) if arg == 0
jmpr [code addr] [arg] // jump +(code addr) if arg == 0

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
    opcode("jmpr",  0x11, 2),
    opcode("out",   0x12, 2),
    opcode("in",    0x13, 2),
    opcode("sleep", 0x14, 1),
    opcode("ssp",   0x15, 1),
    opcode("dump",  0x16, 1),
    opcode("hlt",   0xFF, 0),
]

MEMORY_SIZE = 65536 - (80 * 25)

NEW_VAR = "$"

SPE = (',', '(', ')', ':', '=', '+', '-', '*', '/', '%', '<', '>', '{', '}')

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
    lines = []

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
            if char == "{" or char == "}":
                if tokens:
                    lines.append(tokens.copy())
                tokens = [char]
                lines.append(tokens.copy())
                tokens = []
            else:
                tokens.append(char)

        else:
            current_token += char

    if current_token:
        tokens.append(current_token)

    if tokens:
        lines.append(tokens.copy())

    return lines

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

CURRENT_LABEL = 0

def get_new_label():
    global CURRENT_LABEL
    label = f"label_{CURRENT_LABEL}"
    CURRENT_LABEL += 1
    return label

class output_code:
    class instruction:
        TYPE_COMMENT = 0
        TYPE_LABEL = 1
        TYPE_OPCODE = 2
        TYPE_GOTO = 3
        TYPE_NOTSET = 4

        def __init__(self):
            self.string = ""
            self.bytes = bytearray()
            self.psize = 0
            self.type = self.TYPE_NOTSET

            self.goto_label = None
            self.goto_sr = None
            self.goto_val = None

        def setcomment(self, comment):
            self.type = self.TYPE_COMMENT
            self.string = comment
            self.psize = 0
            self.bytes = bytearray()

        def setlabel(self, label):
            self.type = self.TYPE_LABEL
            self.string = label
            self.psize = 0
            self.bytes = bytearray()

        def setgoto(self, label, sr = None, val = None):
            self.type = self.TYPE_GOTO

            self.string = f"goto {label} if "

            if sr == 0: # val1 is a memory address
                self.string += f"[{hex(val)[2:]}]"
            if sr == 1: # val1 is a value
                self.string += f"{hex(val)[2:]}"
            if sr == 2: # val1 is a stack pointer offset
                self.string += f"[sp+{hex(val)[2:]}]"

            self.string += f" == 0"            

            self.psize = 3
            self.bytes = bytearray()

            self.goto_label = label
            self.goto_sr = sr
            self.goto_val = val

        def setopcode(self, opcode, sr1 = None, val1 = None, sr2 = None, val2 = None):
            self.type = self.TYPE_OPCODE

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

    def add(self, opcode, sr1 = None, val1 = None, sr2 = None, val2 = None):
        instr = self.instruction()
        instr.setopcode(opcode, sr1, val1, sr2, val2)
        self.instructions.append(instr)

    def add_comment(self, comment):
        instr = self.instruction()
        instr.setcomment(comment)
        self.instructions.append(instr)

    def add_label(self, label):
        instr = self.instruction()
        instr.setlabel(label)
        self.instructions.append(instr)

    def add_goto(self, label, sr = None, val = None):
        instr = self.instruction()
        instr.setgoto(label, sr, val)
        self.instructions.append(instr)

    def push(self, other):
        self.instructions += other.instructions

    def dump(self, hide_labels = False):
        pc = 0
        for instr in self.instructions:
            if instr.type == self.instruction.TYPE_COMMENT:
                print(f"\033[32m{instr.string}\033[0m")
            elif instr.type == self.instruction.TYPE_LABEL:
                if not hide_labels: print(f"\033[33m{instr.string}::\033[0m")
            elif instr.type == self.instruction.TYPE_GOTO:
                print(f"\033[33m{hex(pc)[2:].zfill(4)}: {instr.string}\033[0m")
            elif instr.type == self.instruction.TYPE_OPCODE:
                print(f"\033[34m{hex(pc)[2:].zfill(4)}: {instr.string}\033[0m")
            else:
                say_error(f"(Internal) Unknown instruction type: {instr.type}") 
            pc += instr.psize

    def resolve_gotos(self):
        pc = 0
        label_addresses = {}

        # First pass: record label addresses
        for instr in self.instructions:
            if instr.type == self.instruction.TYPE_LABEL:
                label_addresses[instr.string] = pc
            pc += instr.psize

        # Second pass: resolve goto instructions
        for instr in self.instructions:
            if instr.type != self.instruction.TYPE_GOTO:
                continue
            resolved_address = label_addresses.get(instr.goto_label)
            if resolved_address is None:
                say_error(f"(Internal) Unknown label: {instr.goto_label}")
            # Replace the goto instruction with a jmp instruction
            instr.setopcode("jmp", 1, resolved_address, instr.goto_sr, instr.goto_val)

    def write(self, file):
        for instr in self.instructions:
            file.write(instr.bytes)

def op_calculate_rpn(rpn: list):
    global STACK_SIZE

    output = output_code()

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

        if stack_size < 1:
            say_error("Invalid RPN expression: not enough values on the stack")

    if stack_size > 1:
        say_error("Invalid RPN expression: too many values on the stack after evaluation")

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
    output.add_comment("\nProgram initialization")

    output.add("ssp",
            1, STACK_PTR)
    output.add("mov",
            0, STACK_PTR,
            1, STACK_DEBUT)

    return output

def op_fini():
    output = output_code()
    output.add_comment("\nProgram finalization")

    output.add("hlt")

    return output

prog = """
$ var
var = 0

while var 10 < {
    dump(var)
    var = var 1 +
}
""".strip()

local_vars = {"main": []}

def compile_line(lines: list, current_line: int):
    global CURRENT_LNO, STACK_SIZE

    CURRENT_LNO, tokens = lines[current_line]

    print(f"Tokens: {tokens}")
    output = output_code()
    output.add_comment(f"\nl{CURRENT_LNO:03}  {' '.join(tokens)}")

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

        # find the opening brace '{'
        if current_line + 1 >= len(lines) or lines[current_line + 1][1] != ['{']:
            say_error("Bad syntax\nExpected '{' after 'if' statement")

        # find the closing brace '}'
        closing_line = current_line + 1

        while closing_line < len(lines) :
            if lines[closing_line][1] == ['}']:
                break
            closing_line += 1
        else:
            say_error("Bad syntax\nExpected '}' after 'if' block")

        # compile the lines inside the if block
        inner_output = output_code()
        inner_line = current_line + 2
        while inner_line < closing_line:
            sub_output, to_skip = compile_line(lines, inner_line)
            inner_output.push(sub_output)
            inner_line += to_skip

        fin_label = get_new_label()

        # add a jump instruction to skip the if block if the condition is false
        output.add_goto(
            fin_label,            
            0, COND_RES) # jump if the condition is false

        output.push(inner_output)

        output.add_label(fin_label)

        return (output, closing_line - current_line + 1) # return the number of lines to skip
    
    elif tokens[0] == "while":
        if len(tokens) < 2:
            say_error(f"Bad syntax\nSyntax example: while var < 10")

        debut_label = get_new_label()

        # reverse polish notation (RPN) expression
        output.add_label(debut_label)
        output.push(op_calculate_rpn(tokens[1:]))

        # pop the result from the stack to the conditional result memory location
        output.add("pop",
            0, COND_RES)
        STACK_SIZE -= 1

        # find the opening brace '{'
        if current_line + 1 >= len(lines) or lines[current_line + 1][1] != ['{']:
            say_error("Bad syntax\nExpected '{' after 'while' statement")

        # find the closing brace '}'
        closing_line = current_line + 1

        while closing_line < len(lines) :
            if lines[closing_line][1] == ['}']:
                break
            closing_line += 1

        inner_output = output_code()
        inner_line = current_line + 2
        while inner_line < closing_line:
            sub_output, to_skip = compile_line(lines, inner_line)
            inner_output.push(sub_output)
            inner_line += to_skip

        inner_output.add_goto(
            debut_label,
            1, 0) # unconditional jump to the beginning of the while loop
        
        fin_label = get_new_label()

        output.add_goto(
            fin_label,
            0, COND_RES) # jump if the condition is false
        
        output.push(inner_output)
        output.add_label(fin_label)

        return (output, closing_line - current_line + 1) # return the number of lines to skip

    else:
        say_error(f"Bad syntax\nUnknown command or variable: {tokens[0]}")

    return (output, 1)

def compile(lines: str):
    global CURRENT_LNO

    output = op_init()

    tokens_lines = []

    for lno, line in enumerate(lines.splitlines(), start=1):
        CURRENT_LNO = lno

        line = line.strip()
        for t in tokenize_line(line):
            tokens_lines.append((lno, t))

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
main_output.resolve_gotos()
# main_output.dump(hide_labels = True)
main_output.write(ofile)

ofile.close()
