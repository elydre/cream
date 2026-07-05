import compiler.utils as utl

class opcode:
    def __init__(self, name, opcode, argc):
        self.name = name
        self.opcode = opcode
        self.argc = argc


class variable:
    def __init__(self, name, ptrlvl, offset_or_addr, scope = None, is_static = False):
        self.name = name
        self.ptrlvl = ptrlvl # number of [] at declaration (actually unused)
        self.is_static = is_static

        if scope is None:
            scope = CURRENT_SCOPE

        if is_static:
            self.scope = None
            self.addr = offset_or_addr
            self.offset = None
        else:
            self.scope = scope
            self.offset = offset_or_addr
            self.addr = None

    def add(self):
        if self.is_static:
            STATIC_VARS.append(self)
        else:
            if self.scope not in LOCAL_VARS:
                LOCAL_VARS[self.scope] = []
            LOCAL_VARS[self.scope].append(self)

class func:
    def __init__(self, name, argc, does_return = True, is_builtin = False, blt_handler = None, no_rpn = False, opcodes = None):
        self.name = name
        self.argc = argc
        self.does_return = does_return
        self.is_builtin = is_builtin

        self.blt_handler = blt_handler  # function to call if builtin
        self.no_rpn = no_rpn            # illegal use in RPN (alloca)
        self.opcodes = opcodes          # compiled user code if not a builtin

    def add(self):
        ALL_FUNCS.append(self)


def is_variable(s, scope = None):
    if scope is None:
        scope = CURRENT_SCOPE

    if s in [e.name for e in STATIC_VARS]:
        return True
    return s in [e.name for e in LOCAL_VARS[scope]]

def get_variable(s, scope = None):
    if scope is None:
        scope = CURRENT_SCOPE

    if not is_variable(s, scope):
        utl.say_error(f"Unknown variable: {s}")

    if s in [e.name for e in STATIC_VARS]:
        return next(e for e in STATIC_VARS if e.name == s)

    return next(e for e in LOCAL_VARS[scope] if e.name == s)

def is_func(s):
    return s in [e.name for e in ALL_FUNCS]

def get_func(s):
    if not is_func(s):
        utl.say_error(f"Unknown function: {s}")

    return next(e for e in ALL_FUNCS if e.name == s)

def is_valid_name(s):
    if not s:
        return False
    if not (s[0].isalpha() or s[0] == "_"):
        return False
    for c in s:
        if not (c.isalnum() or c == "_"):
            return False
    if s in KEYWORDS:
        return False
    return True

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

MAGIC_NUMBER = 0xF057
ARCH_VERSION = 0x0001

CHARS_SPE = [",", "(", ")", ":", "=", "{", "}", "[", "]", "&", "$", "!", "//"]
CHARS_OPR = ["+", "-", "*", "/", "%", "==", "!=", "<", ">"]
CHARS_SPE += CHARS_OPR

KEYWORDS = ["if", "elif", "else", "while", "func", "return", "break", "continue", "for"]

NEW_VAR = ":"
NEW_VAR_STATIC = "$"

COND_RES_ADDR   = MEMORY_SIZE - 1
FUNC_RET_ADDR   = MEMORY_SIZE - 2
STACK_DEBUT_PTR = MEMORY_SIZE - 3
STACK_PTR       = MEMORY_SIZE - 4

STATIC_ADDR     = MEMORY_SIZE - 4 # will be decremented as static variables / strings are added
STATIC_BYTES    = bytearray()

CURRENT_LNO = 0
CURRENT_SCOPE = "global"

LOCAL_VARS = {"global": []}
STATIC_VARS = []

ALL_FUNCS = []

DATA_SEQ = []
