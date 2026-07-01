import ctypes

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
mss   [arg] [offset] [arg] [offset]

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
    opcode("mss",   0x17, 4),
    opcode("hlt",   0xFF, 0),
]

MEMORY_SIZE = 65536 - (80 * 25)

NEW_VAR = "$"

SPE = (',', '(', ')', ':', '=', '+', '-', '*', '/', '%', '<', '>', '{', '}', '[', ']', '&')

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
            tokens.append(current_token)
            current_token = ""

        elif char == "=" and not current_token and tokens and tokens[-1] in SPE:
            tokens[-1] += char

        elif char in SPE:
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

    tokens.append(current_token)

    if tokens:
        lines.append(tokens.copy())

    for line in lines:
        while "" in line:
            line.remove("")

    while [] in lines:
        lines.remove([])

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
STACK_DEBUT_PTR = MEMORY_SIZE - 2
STACK_PTR = MEMORY_SIZE - 3
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

        def setgoto(self, label, val):
            self.type = self.TYPE_GOTO

            self.string = f"goto {label} if "

            if val[0] == 0: # val1 is a memory address
                self.string += f"[{hex(val[1])[2:]}]"
            elif val[0] == 1: # val1 is a value
                self.string += f"{hex(val[1])[2:]}"
            elif val[0] == 2: # val1 is a stack pointer offset
                self.string += f"[sp+{hex(val[1])[2:]}]"
            elif val[0] == 3: # val1 is a pointer
                self.string += f"[[{hex(val[1])[2:]}]]"

            self.string += f" == 0"

            self.psize = 3
            self.bytes = bytearray()

            self.goto_label = label
            self.goto_val = val

        def setopcode(self, opcode, v1, v2, v3, v4):
            self.type = self.TYPE_OPCODE

            if not any(e.name == opcode for e in OPCODES):
                say_error(f"(Internal) Unknown opcode: {opcode}")

            self.string = f"{opcode} "
            argc = 0

            op = next(e for e in OPCODES if e.name == opcode)

            b = bytearray()
            b.append(op.opcode)
            b.append(0) # placeholder for sources and padding

            for i, v in enumerate([v1, v2, v3, v4]):
                if type(v) != tuple and v != None:
                    say_error(f"(Internal) Invalid argument type for opcode {opcode}\nExpected tuple, got {type(v)}")
                s = (v[0] if v else 0)
                if s not in [0, 1, 2, 3]:
                    say_error(f"(Internal) Invalid source type for opcode {opcode}\nExpected 0, 1, 2 or 3, got {s}")
                b[1] |= (s << (2 * -(i - 3)))

                if v == None:
                    break
                if v[0] == 0: # val1 is a memory address
                    self.string += f"[{hex(v[1])[2:]}] "
                elif v[0] == 1: # val1 is a value
                    self.string += f"{hex(v[1])[2:]} "
                elif v[0] == 2: # val1 is a stack pointer offset
                    self.string += f"[sp+{hex(v[1])[2:]}] "
                elif v[0] == 3: # val1 is a pointer
                    self.string += f"[[{hex(v[1])[2:]}]] "

                argc += 1

            for v in [v1, v2, v3, v4]:
                if v == None:
                    break
                b += v[1].to_bytes(2, byteorder='little')

            self.string = self.string.strip()

            if argc != op.argc:
                say_error(f"(Internal) Bad number of arguments for opcode {opcode}\nExpected {op.argc}, got {argc}")


            self.psize = len(b) // 2
            self.bytes = b

    def __init__(self):
        self.instructions = []

    def add(self, opcode, v1 = None, v2 = None, v3 = None, v4 = None):
        instr = self.instruction()
        instr.setopcode(opcode, v1, v2, v3, v4)
        self.instructions.append(instr)

    def add_comment(self, comment):
        instr = self.instruction()
        instr.setcomment(comment)
        self.instructions.append(instr)

    def add_label(self, label):
        instr = self.instruction()
        instr.setlabel(label)
        self.instructions.append(instr)

    def add_goto(self, label, val = None):
        instr = self.instruction()
        instr.setgoto(label, val)
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
            instr.setopcode("jmp", (1, resolved_address), instr.goto_val, None, None)

    def write(self, file):
        for instr in self.instructions:
            file.write(instr.bytes)

def op_calculate_rpn(rpn: list):
    global STACK_SIZE

    output = output_code()

    if not rpn:
        say_error("Empty RPN expression")

    stack_size = 0
    have_ampersand = False

    for token in rpn:
        if is_variable(token):
            v = get_variable(token)
            if have_ampersand:
                output.add("push",
                       (1, STACK_DEBUT - v.offset))
                have_ampersand = False
            else:
                output.add("push",
                       (2, (STACK_SIZE + stack_size) - v.offset))
                # output.add("push", (1, 0))
                # output.add("mss",
                #         (2, 0),
                #         (1, 0),
                #         (3, STACK_DEBUT_PTR),
                #         (1, ctypes.c_ushort(-v.offset).value))
            stack_size += 1

        elif token == '&':
            have_ampersand = True
            continue

        elif is_number(token):
            output.add("push",
                   (1, to_number(token)))
            stack_size += 1

        elif token in ['+', '-', '*', '/', '%', '==', '!=', '<', '>']:
            stack_size -= 1
            if token == '+':
                output.add("add",
                        (2, 1), (2, 0))
            elif token == '-':
                output.add("sub",
                        (2, 1), (2, 0))
            elif token == '*':
                output.add("mul",
                        (2, 1), (2, 0))
            elif token == '/':
                output.add("div",
                        (2, 1), (2, 0))
            elif token == '%':
                output.add("mod",
                        (2, 1), (2, 0))
            elif token == '==':
                output.add("eq",
                        (2, 1), (2, 0))
            elif token == '!=':
                output.add("neq",
                        (2, 1), (2, 0))
            elif token == '<':
                output.add("lt",
                        (2, 1), (2, 0))
            elif token == '>':
                output.add("gt",
                        (2, 1), (2, 0))

            output.add("pop",
                   (1, 0))

        else:
            say_error(f"Unknown token in RPN expression: {token}")

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
                (2, STACK_SIZE - variables[0].offset))

    elif len(f.args) == 2:
        output.add(f.name,
                (2, STACK_SIZE - variables[0].offset),
                (2, STACK_SIZE - variables[1].offset))

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


prog1 = """
$ var
$ [ptr]

ptr = &var

[ptr] = 123

dump(var)
dump(ptr)
""".strip()

prog2 = """
$ to_test
$ div
$ is_prime

to_test = 1

while to_test 100 < {
    div = 2
    is_prime = 1
    while div to_test < {
        if to_test div % 0 == {
            is_prime = 0
        }
        div = div 1 +
    }
    if is_prime 1 == {
        dump(to_test)
    }
    to_test = to_test 1 +
}
""".strip()

prog = prog2

local_vars = {"main": []}

def locate_braces(lines: list, current_line: int):
     # find the opening brace '{'
    if current_line + 1 >= len(lines) or lines[current_line + 1][1] != ['{']:
        say_error("Bad syntax\nExpected '{' after 'if' statement")

    # find the closing brace '}'
    closing_line = current_line + 2

    opening_braces = 1
    while closing_line < len(lines) :
        if lines[closing_line][1] == ['}']:
            opening_braces -= 1
            if opening_braces == 0:
                break
        elif lines[closing_line][1] == ['{']:
            opening_braces += 1
        closing_line += 1
    else:
        say_error("Bad syntax\nExpected '}' after 'if' block")

    return closing_line

def compile_lines(lines: list, size: int):
    current_line = 0
    output = output_code()

    while current_line < size:
        sub_output, to_skip = compile_line(lines, current_line)
        output.push(sub_output)
        current_line += to_skip

    return output

def compile_line(lines: list, current_line: int):
    global CURRENT_LNO, STACK_SIZE

    CURRENT_LNO, tokens = lines[current_line]

    print(f"Tokens: {tokens}")
    output = output_code()
    output.add_comment(f"\nl{CURRENT_LNO:03}  {' '.join(tokens)}")

    if tokens[0] == NEW_VAR:
        ptrlvl = 0
        while tokens[ptrlvl + 1] == '[':
            ptrlvl += 1

        for i in range(ptrlvl):
            if tokens[i + 3] != ']':
                say_error(f"Bad pointer declaration\nSyntax example: {NEW_VAR} [ptr_name]")

        if len(tokens) != 2 + ptrlvl * 2:
            if ptrlvl:
                say_error(f"Bad pointer declaration\nSyntax example: {NEW_VAR} [ptr_name]")
            else:
                say_error(f"Bad variable declaration\nSyntax example: {NEW_VAR} var_name")

        var_name = tokens[1 + ptrlvl]

        old_offset = local_vars["main"][-1].offset if local_vars["main"] else 0
        v = variable(var_name, ptrlvl, old_offset + 1)
        v.add_to_local_vars()

        STACK_SIZE += 1
        output.add("push",
            (1, 0))

    elif is_variable(tokens[0]):
        if len(tokens) < 3 or tokens[1] != '=':
            say_error(f"Bad variable assignment\nSyntax example: var_name = 123")

        v = get_variable(tokens[0])

        # reverse polish notation (RPN) expression
        output.push(op_calculate_rpn(tokens[2:]))

        # move the result from the stack to the variable's memory location
        output.add("pop",
            (2, STACK_SIZE - v.offset))
        STACK_SIZE -= 1

    elif tokens[0] == "[":
        ptrlvl = 1
        while tokens[ptrlvl] == '[':
            ptrlvl += 1

        offset = None

        if not is_variable(tokens[ptrlvl]):
            say_error(f"Undefined pointer during assignment\nSyntax example: [ptr_name] = 123")

        ptr = get_variable(tokens[ptrlvl])

        if tokens[ptrlvl + 1] == ':':
            offset = []
            opening_brackets = 0
            for i in range(ptrlvl + 2, len(tokens)):
                if tokens[i] == '[':
                    opening_brackets += 1
                elif tokens[i] == ']':
                    opening_brackets -= 1
                    if opening_brackets < 0:
                        break
                offset.append(tokens[i])
            else:
                say_error(f"Bad syntax\nExpected ']' after offset expression")

        if offset is not None:
            say_error(f"(Internal) Pointer offset assignment not implemented yet")

        # check closing brackets
        begin = ptrlvl + 1 + (len(offset) if offset else 0)
        end = begin + ptrlvl

        if len(tokens) < end + 2 or tokens[end] != '=':
            say_error(f"Bad pointer assignment\nSyntax example: [ptr_name] = 123")

        if tokens[begin:end] != [']'] * ptrlvl:
            say_error(f"Wrong number of closing brackets in pointer assignment\nSyntax example: [ptr_name] = 123")

        # reverse polish notation (RPN) expression
        output.push(op_calculate_rpn(tokens[end + 1:]))

        # move the result from the stack to the pointer's memory location
        output.add("pop",
            (3, STACK_SIZE - ptr.offset))

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
            (0, COND_RES))
        STACK_SIZE -= 1

        # compile the lines inside the if block
        closing_line = locate_braces(lines, current_line)
        inner_output = compile_lines(lines[current_line + 2:closing_line], closing_line - current_line - 2)

        fin_label = get_new_label()

        # add a jump instruction to skip the if block if the condition is false
        output.add_goto(
            fin_label,
            (0, COND_RES)) # jump if the condition is false

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
            (0, COND_RES))
        STACK_SIZE -= 1

        # compile the lines inside the while block
        closing_line = locate_braces(lines, current_line)
        inner_output = compile_lines(lines[current_line + 2:closing_line], closing_line - current_line - 2)

        inner_output.add_goto(
            debut_label, (1, 0)) # unconditional jump to the beginning of the while loop

        fin_label = get_new_label()

        output.add_goto(
            fin_label, (0, COND_RES)) # jump if the condition is false

        output.push(inner_output)
        output.add_label(fin_label)

        return (output, closing_line - current_line + 1) # return the number of lines to skip

    else:
        say_error(f"Bad syntax\nUnknown command or variable: {tokens[0]}")

    return (output, 1)

def compile(lines: str):
    global CURRENT_LNO

    tokens_lines = []

    for lno, line in enumerate(lines.splitlines(), start=1):
        CURRENT_LNO = lno

        line = line.strip()
        for t in tokenize_line(line):
            tokens_lines.append((lno, t))

    output = compile_lines(tokens_lines, len(tokens_lines))
    output.push(op_fini())

    return output

#=======================================


ofile = open("output.bin", "wb")

if not ofile:
    exit("Could not open output file")

main_output = compile(prog)

# main_output.dump()
main_output.resolve_gotos()
main_output.dump(hide_labels = True)
main_output.write(ofile)

ofile.close()
