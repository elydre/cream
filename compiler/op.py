import compiler.output as out
import compiler.utils as utl
import compiler.defs as defs
import compiler.tokens as toks
import compiler.op as op


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

        elif defs.is_func(token):
            open_parens = 1
            for j, t in enumerate(rpn[i + 2:]):
                if t == '(':
                    open_parens += 1
                elif t == ')':
                    open_parens -= 1
                    if open_parens == 0:
                        skip_to = i + 2 + j
                        break
            else:
                utl.say_error(f"Unclosed parenthesis in function call\nSyntax example: func_name(var1, var2)")

            f = defs.get_func(token)
            if not f.does_return:
                utl.say_error(f"Function {f.name} does not return a value, cannot use in RPN expression")

            output.push(op.call_func(f, rpn[i + 2:skip_to]))
            skip_to += 1

            # push the return value to the stack if the function returns a value
            output.add("push",
                    (0, defs.FUNC_RET_ADDR))
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


def fast_assign_var(v: defs.variable, tokens: list):
    output = out.output_code()

    if len(tokens) == 1 and defs.is_variable(tokens[0]):
        v2 = defs.get_variable(tokens[0])
        output.add("mss",
                (0, defs.STACK_DEBUT_PTR), (1, utl.to_u16(-v.offset)),
                (0, defs.STACK_DEBUT_PTR), (1, utl.to_u16(-v2.offset)))
        return output

    if defs.is_func(tokens[0]):
        f = defs.get_func(tokens[0])

        if len(tokens) < 4 or tokens[1] != '(' or tokens[-1] != ')':
            utl.say_error(f"Bad syntax on function call\nSyntax example: {f.name}({', '.join(['var' + str(i + 1) for i in range(f.argc)])})")

        if not f.does_return:
            utl.say_error(f"Function {f.name} does not return a value, cannot assign to variable {v.name}")

        output.push(op.call_func(f, tokens[2:-1]))

        # move the result from FUNC_RET_ADDR to the variable's memory location
        output.add("mss",
                (0, defs.STACK_DEBUT_PTR), (1, utl.to_u16(-v.offset)),
                (1, defs.FUNC_RET_ADDR), (1, 0))
        
        return output

    return None

def load_ptraddr(tokens: list):
    output = out.output_code()

    if tokens[0] != "[":
        utl.say_error(f"(Internal) ptr resolution on non-pointer")

    is_onstack = False
    end = 0

    if tokens[1] == '[':
        o, e = op.load_ptraddr(tokens[1:])
        output.push(o)
        end += e
        is_onstack = True

    elif defs.is_variable(tokens[1]) and tokens[2] == ']':
        ptr = defs.get_variable(tokens[1])
        end += 1

    else:
        # find the closing bracket and send to RPN calculator
        open_brackets = 1
        for i, token in enumerate(tokens[1:]):
            if token == '[':
                open_brackets += 1
            elif token == ']':
                open_brackets -= 1
                if open_brackets == 0:
                    end += i
                    break
        else:
            utl.say_error(f"Unclosed brackets in pointer access\nSyntax example: [ptr]")

        output.push(op.calculate_rpn(tokens[1:end + 1]))
        is_onstack = True

    
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


def call_func(f: defs.func, tokens: list, ):
    args = toks.split_func_args(tokens)

    print(f"Calling function {f.name} with arguments: {args}")

    if len(args) != f.argc:
        utl.say_error(f"Wrong number of arguments for function {f.name}\nSyntax example: {f.name}({', '.join(['var' + str(i + 1) for i in range(f.argc)])})")

    if f.is_builtin:
        return f.blt_handler(args)
    else:
        utl.say_error(f"(Internal) User-defined functions not implemented yet: {f.name}")
