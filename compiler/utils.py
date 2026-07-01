import compiler.defs as defs

import ctypes

def say_error(message):
    print(f"Error line {defs.CURRENT_LNO}: {message}")
    exit(1)

CURRENT_LABEL = 0

def get_new_label():
    global CURRENT_LABEL
    label = f"label_{CURRENT_LABEL}"
    CURRENT_LABEL += 1
    return label

def to_u16(value):
    return ctypes.c_ushort(value).value
