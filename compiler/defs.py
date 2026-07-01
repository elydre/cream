import compiler.utils as utl

class opcode:
    def __init__(self, name, opcode, argc):
        self.name = name
        self.opcode = opcode
        self.argc = argc


class variable:
    def __init__(self, name, ptrlvl, offset):
        self.name = name
        self.ptrlvl = ptrlvl
        self.offset = offset

    def add_to_local_variables(self):
        LOCAL_VARS["main"].append(self)

class func:
    def __init__(self, name, argc, does_return, is_builtin = False, blt_handler = None):
        self.name = name
        self.argc = argc
        self.does_return = does_return
        self.is_builtin = is_builtin
        self.blt_handler = blt_handler

def is_number(s):
    try:
        if s.startswith("0x"):
            int(s, 16)
        else:
            int(s)
        return True
    except ValueError:
        return False

def to_number(s):
    if s.startswith("0x"):
        return int(s, 16)
    else:
        return int(s)

def is_variable(s, scope = "main"):
    return s in [e.name for e in LOCAL_VARS[scope]]

def get_variable(s, scope = "main"):
    if not is_variable(s, scope):
        utl.say_error(f"Unknown variable: {s}")

    return next(e for e in LOCAL_VARS[scope] if e.name == s)

def is_func(s):
    return s in [e.name for e in ALL_FUNCS]

def get_func(s):
    if not is_func(s):
        utl.say_error(f"Unknown function: {s}")

    return next(e for e in ALL_FUNCS if e.name == s)


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
    opcode("pushs", 0x18, 2),
    opcode("pops",  0x19, 2),
    opcode("hlt",   0xFF, 0),
]

MEMORY_SIZE = 65536 - (80 * 25)

CHARS_SPE = (',', '(', ')', ':', '=', '+', '-', '*', '/', '%', '<', '>', '{', '}', '[', ']', '&')
NEW_VAR = '$'

COND_RES_ADDR   = MEMORY_SIZE - 1
FUNC_RET_ADDR   = MEMORY_SIZE - 2
STACK_DEBUT_PTR = MEMORY_SIZE - 3
STACK_PTR       = MEMORY_SIZE - 4

STACK_DEBUT = STACK_PTR

CURRENT_LNO = 0

LOCAL_VARS = {"main": []}
ALL_FUNCS = []
