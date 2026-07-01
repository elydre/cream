import compiler.defs as defs
import compiler.output as out
import compiler.op as op

def blt_alloca(args: list):
    output = out.output_code()
    # calculate the requested size in bytes
    output.push(op.calculate_rpn(args[0]))

    # add the requested size to the stack pointer
    output.add("sub",
            (0, defs.STACK_PTR),
            (2, 0))
    
    # pop calculated size from the stack
    # (this is not the right address, but we just have to remove 1 value from the stack)
    output.add("pop",
            (1, 0))
    
    # copy the current stack pointer value to return memory location
    output.add("mov",
            (0, defs.FUNC_RET_ADDR),
            (0, defs.STACK_PTR))

    return output


def blt_out(args: list):
    output = out.output_code()

    # push arguments to the stack in reverse order
    output.push(op.calculate_rpn(args[1]))
    output.push(op.calculate_rpn(args[0]))

    output.add("out", (2, 1), (2, 0))

    output.add("pop", (1, 0))
    output.add("pop", (1, 0))

    return output

def blt_in(args: list):
    output = out.output_code()

    output.push(op.calculate_rpn(args[0]))
    output.add("in", (0, defs.FUNC_RET_ADDR), (2, 0))
    output.add("pop", (1, 0))

    return output

def blt_sleep(args: list):
    output = out.output_code()

    output.push(op.calculate_rpn(args[0]))
    output.add("sleep", (2, 0))
    output.add("pop", (1, 0))

    return output

def blt_dump(args: list):
    output = out.output_code()

    output.push(op.calculate_rpn(args[0]))
    output.add("dump", (2, 0))
    output.add("pop", (1, 0))

    return output

def blt_get_sp(args: list):
    output = out.output_code()

    # copy the current stack pointer value to return memory location
    output.add("mov",
            (0, defs.FUNC_RET_ADDR),
            (0, defs.STACK_PTR))

    return output

def add_builtin_functions():
    defs.ALL_FUNCS.append(defs.func("alloca",  1, True,  is_builtin=True, blt_handler = blt_alloca))
    defs.ALL_FUNCS.append(defs.func("out",     2, False, is_builtin=True, blt_handler = blt_out))
    defs.ALL_FUNCS.append(defs.func("in",      1, True,  is_builtin=True, blt_handler = blt_in))
    defs.ALL_FUNCS.append(defs.func("sleep",   1, False, is_builtin=True, blt_handler = blt_sleep))
    defs.ALL_FUNCS.append(defs.func("dump",    1, False, is_builtin=True, blt_handler = blt_dump))
    defs.ALL_FUNCS.append(defs.func("_get_sp", 0, True,  is_builtin=True, blt_handler = blt_get_sp))
