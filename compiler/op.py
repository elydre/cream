import compiler.output as out
import compiler.utils as utl
import compiler.defs as defs
import compiler.tokens as toks
import compiler.op as op

def calculate_rpn(rpn: list):
    output = out.output_code()

    if not rpn:
        utl.say_error("Empty RPN expression")

    stack_size = 0
    have_ampersand = False

    skip_to = 0

    for i, token in enumerate(rpn):
        if i < skip_to:
            continue

        if token == '&':
            have_ampersand = True
            continue

        if defs.is_variable(token):
            v = defs.get_variable(token)
            if have_ampersand:
                output.add("push",
                        (0, defs.STACK_DEBUT_PTR))
                output.add("add",
                        (2, 0), (1, utl.to_u16(-v.offset)))
                have_ampersand = False
            else:
                # output.add("push", (1, 0))
                # output.add("mss",
                #         (0, defs.STACK_PTR),
                #         (1, 0),
                #         (0, defs.STACK_DEBUT_PTR),
                #         (1, utl.to_u16(-v.offset)))
                output.add("pushs",
                       (0, defs.STACK_DEBUT_PTR),
                       (1, utl.to_u16(-v.offset)))
            stack_size += 1
            continue

        if defs.is_func(token):
            utl.say_error(f"Function calls not supported in RPN expressions yet: {token}")
        
        elif have_ampersand:
            utl.say_error(f"Unexpected '&' before token: {token}\nCorrect syntax example: ptr = &var")

        if token == '[':
            o, end = op.load_ptraddr(rpn[i:])
            skip_to = i + end
            output.push(o)

            # load the value from the pointer's address

            output.add("mss",
                    (0, defs.STACK_PTR), (1, 0),
                    (2, 0), (1, 0))

            stack_size += 1


        elif defs.is_number(token):
            output.add("push",
                   (1, defs.to_number(token)))
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
            utl.say_error(f"Unknown token in RPN expression: {token}")

        if stack_size < 1:
            utl.say_error("Invalid RPN expression: not enough values on the stack")

    if stack_size > 1:
        utl.say_error("Invalid RPN expression: too many values on the stack after evaluation")

    return output




def call_asmfunc(f, args: list):
    output = out.output_code()

    # push arguments to the stack in reverse order
    for v in reversed(args):
        output.push(op.calculate_rpn(v))

    if f.argc == 0:
        output.add(f.name)

    elif f.argc == 1:
        output.add(f.name,
                (2, 0))

    elif f.argc == 2:
        output.add(f.name,
                (2, 1), (2, 0))

    else:
        utl.say_error(f"(Internal) Function {f.name} has more than 2 arguments")

    for v in args:
        output.add("pop",
                (1, 0))

    return output


def call_func(f: defs.func, tokens: list):
    args = toks.split_func_args(tokens)

    print(f"Calling function {f.name} with arguments: {args}")

    if len(args) != f.argc:
        utl.say_error(f"Wrong number of arguments for function {f.name}\nSyntax example: {f.name}({', '.join(['var' + str(i + 1) for i in range(len(f.args))])})")

    if f.ftype == defs.func.TYPE_ASM:
        return op.call_asmfunc(f, args)
    elif f.ftype == defs.func.TYPE_BLT:
        return f.blt_handler(args)
    else:
        utl.say_error(f"(Internal) User-defined functions not implemented yet: {f.name}")


def load_ptraddr(tokens: list):
    output = out.output_code()

    if tokens[0] != "[":
        utl.say_error(f"(Internal) ptr resolution on non-pointer")

    offset = None

    is_onstack = False
    end = 0

    if tokens[1] == '[':
        o, e = op.load_ptraddr(tokens[1:])
        output.push(o)
        end += e
        is_onstack = True

    else:
        if not defs.is_variable(tokens[1]):
            utl.say_error(f"Unknown variable in pointer access: {tokens[1]}")

        ptr = defs.get_variable(tokens[1])
        end += 1

    if tokens[end] == ':':
        offset = []
        opening_brackets = 0
        for i in range(end + 1, len(tokens)):
            if tokens[i] == '[':
                opening_brackets += 1
            elif tokens[i] == ']':
                opening_brackets -= 1
                if opening_brackets < 0:
                    break
            offset.append(tokens[i])
            end += 1
        else:
            utl.say_error(f"Unclosed brackets in pointer offset\nSyntax example: [ptr:offset_ptr[123]]")

    if offset is not None:
        utl.say_error(f"(Internal) Pointer offset assignment not implemented yet")
    
    end += 1
    # check closing bracket
    if end >= len(tokens) or tokens[end] != ']':
        utl.say_error(f"Unclosed brackets in pointer access\nSyntax example: [ptr]")

    if is_onstack:
        output.add("mss",
                (0, defs.STACK_PTR), (1, 0),
                (2, 0), (1, 0))
    else:
        # load the pointer's address from memory
        output.add("pushs",
                (0, defs.STACK_DEBUT_PTR),
                (1, utl.to_u16(-ptr.offset)))
    
    return (output, end + 1)

def init():
    output = out.output_code()
    output.add_comment("\nProgram initialization")

    output.add("ssp",
            (1, defs.STACK_PTR))
    output.add("mov",
            (0, defs.STACK_PTR),
            (1, defs.STACK_DEBUT))
    output.add("mov",
            (0, defs.STACK_DEBUT_PTR),
            (1, defs.STACK_DEBUT))

    return output

def fini():
    output = out.output_code()
    output.add_comment("\nProgram finalization")

    output.add("hlt")

    return output
