import compiler.builtin as blt
import compiler.tokens as toks
import compiler.output as out
import compiler.utils as utl
import compiler.defs as defs
import compiler.op as op


def compile_lines(lines: list, size: int, labels: tuple = None):
    current_line = 0
    output = out.output_code()

    while current_line < size:
        sub_output, to_skip = compile_line(lines, current_line, labels)
        output.push(sub_output)
        current_line += to_skip

    return output


def compile_line(lines: list, current_line: int, labels: tuple = None):
    defs.CURRENT_LNO, tokens = lines[current_line]

    output = out.output_code()
    output.add_comment(f"\nl{defs.CURRENT_LNO:03}  {' '.join(tokens)}")

    if tokens[0] == defs.NEW_VAR or tokens[0] == defs.NEW_VAR_STATIC:
        ptrlvl = 0
        while tokens[ptrlvl + 1] == '[':
            ptrlvl += 1

        end_brackets = 2 + ptrlvl * 2
        if len(tokens) < end_brackets:
            if ptrlvl:
                utl.say_error(f"Bad pointer declaration\nSyntax example: {tokens[0]} [ptr_name]")
            else:
                utl.say_error(f"Bad variable declaration\nSyntax example: {tokens[0]} var_name")

        # check for closing brackets
        if ptrlvl and (ptrlvl * ']' != ''.join(tokens[2 + ptrlvl:])):
            utl.say_error(f"Bad pointer declaration\nSyntax example: {tokens[0]} [ptr_name]")

        var_name = tokens[1 + ptrlvl]

        # check if the variable already exists
        if defs.is_variable(var_name):
            utl.say_error(f"Variable already exists: {tokens[1 + ptrlvl]}")

        if tokens[0] == defs.NEW_VAR:
            old_offset = defs.LOCAL_VARS["main"][-1].offset if defs.LOCAL_VARS["main"] else 0
            v = defs.variable(var_name, ptrlvl, old_offset + 1)
        
            if len(tokens) > end_brackets:
                if tokens[end_brackets] != '=':
                    utl.say_error(f"Bad variable declaration\nSyntax example: {tokens[0]} var_name = 123")

                # reverse polish notation (RPN) expression
                output.push(op.calculate_rpn(tokens[end_brackets + 1:]))

            else:
                output.add("push",
                    (1, 0))

        else:
            if len(tokens) > end_brackets:
                if tokens[end_brackets] != '=' or len(tokens) != end_brackets + 2 or not utl.is_number(tokens[end_brackets + 1]):
                    utl.say_error(f"Bad static variable declaration, only const expected\nSyntax example: {tokens[0]} var_name = 123")
                val = int(tokens[end_brackets + 1])
            else:
                val = 0

            addr = out.get_static_addr(1, [val])
            v = defs.variable(var_name, ptrlvl, addr, is_static = True)

    elif defs.is_variable(tokens[0]):
        if len(tokens) < 3 or tokens[1] != '=':
            utl.say_error(f"Bad variable assignment\nSyntax example: var_name = 123")

        v = defs.get_variable(tokens[0])

        fast_assignment = op.fast_assign_var(v, tokens[2:])

        if fast_assignment:
            output.push(fast_assignment)
        else:
            # reverse polish notation (RPN) expression
            output.push(op.calculate_rpn(tokens[2:]))

            # move the result from the stack to the variable's memory location
            if v.is_static:
                output.add("pop", (0, v.addr))
            else:
                output.add("pops",
                        (0, defs.STACK_DEBUT_PTR),
                        (1, utl.to_u16(-v.offset)))


    elif tokens[0] == "[":
        o, end = op.load_ptraddr(tokens)

        if len(tokens) < end + 2 or tokens[end] != '=':
            utl.say_error(f"Bad pointer assignment\nSyntax example: [ptr_name] = 123")

        output.push(o)

        # reverse polish notation (RPN) expression
        output.push(op.calculate_rpn(tokens[end + 1:]))

        # move the result from the stack to the pointer's memory location
        output.add("mss",
                (2, 1), (1, 0),
                (0, defs.STACK_PTR), (1, 0))

        # pop the pointer's address and the value to assign
        output.add("pop", (1, 0))
        output.add("pop", (1, 0))

    elif defs.is_func(tokens[0]):
        f = defs.get_func(tokens[0])
        if len(tokens) < 4 or tokens[1] != '(' or tokens[-1] != ')':
            utl.say_error(f"Bad syntax on function call\nSyntax example: {f.name}({', '.join(['var' + str(i + 1) for i in range(f.argc)])})")

        output.push(op.call_func(f, tokens[2:-1]))

    elif tokens[0] == "if":
        if len(tokens) < 2:
            utl.say_error(f"Bad syntax\nSyntax example: if var == 0")

        # reverse polish notation (RPN) expression
        output.push(op.calculate_rpn(tokens[1:]))

        # pop the result from the stack to the conditional result memory location
        output.add("pop",
            (0, defs.COND_RES_ADDR))

        # compile the lines inside the if block
        closing_line = toks.locate_braces(lines, current_line)
        inner_output = compile_lines(lines[current_line + 2:closing_line], closing_line - current_line - 2, labels)

        fin_label = utl.get_new_label()
        next_label = utl.get_new_label()

        # add a jump instruction to skip the if block if the condition is false
        output.add_goto(
            next_label,
            (0, defs.COND_RES_ADDR)) # jump if the condition is false

        output.push(inner_output)

        # check if there is an elif or else block after the if block
        next_line = None
        while closing_line + 1 < len(lines) and lines[closing_line + 1][1][0] in ("elif", "else"):
            next_line = lines[closing_line + 1]
            next_tokens = next_line[1]

            output.add_goto(
                fin_label, (1, 0)) # unconditional jump to the end of the if block
            output.add_label(next_label)

            next_label = utl.get_new_label()

            if next_tokens[0] == "elif":
                if len(next_tokens) < 2:
                    utl.say_error(f"Bad syntax\nSyntax example: elif var == 0")

                # reverse polish notation (RPN) expression
                output.push(op.calculate_rpn(next_tokens[1:]))

                # pop the result from the stack to the conditional result memory location
                output.add("pop",
                    (0, defs.COND_RES_ADDR))

                # compile the lines inside the elif block
                tmp = toks.locate_braces(lines, closing_line + 1)
                inner_output = compile_lines(lines[closing_line + 3:tmp], tmp - closing_line - 3, labels)
                closing_line = tmp

                # add a jump instruction to skip the elif block if the condition is false
                output.add_goto(
                    next_label,
                    (0, defs.COND_RES_ADDR)) # jump if the condition is false

                output.push(inner_output)

            elif next_tokens[0] == "else":
                if len(next_tokens) != 1:
                    utl.say_error(f"Unexpected token {next_tokens[1]} after else\nSyntax example: else " + "{ ... }")
                # compile the lines inside the else block
                tmp = toks.locate_braces(lines, closing_line + 1)
                inner_output = compile_lines(lines[closing_line + 3:tmp], tmp - closing_line - 3, labels)
                closing_line = tmp
                
                output.push(inner_output)
                break

        if next_line is None:
            output.add_label(next_label)
        else:
            output.add_label(fin_label)

        return (output, closing_line - current_line + 1) # return the number of lines to skip

    elif tokens[0] == "while":
        if len(tokens) < 2:
            utl.say_error(f"Bad syntax\nSyntax example: while var < 10")

        debut_label = utl.get_new_label()
        fin_label   = utl.get_new_label()

        # reverse polish notation (RPN) expression
        output.add_label(debut_label)
        output.push(op.calculate_rpn(tokens[1:]))

        # pop the result from the stack to the conditional result memory location
        output.add("pop",
            (0, defs.COND_RES_ADDR))

        # compile the lines inside the while block
        closing_line = toks.locate_braces(lines, current_line)
        inner_output = compile_lines(lines[current_line + 2:closing_line], closing_line - current_line - 2, (debut_label, fin_label))

        inner_output.add_goto(
            debut_label, (1, 0)) # unconditional jump to the beginning of the while loop


        output.add_goto(
            fin_label, (0, defs.COND_RES_ADDR)) # jump if the condition is false

        output.push(inner_output)
        output.add_label(fin_label)

        return (output, closing_line - current_line + 1) # return the number of lines to skip
    
    elif tokens[0] == "for":
        # Syntax: for var (debut, fin)
        # debut and fin can be any expression that evaluates to an integer
        if len(tokens) < 6 or tokens[2] != '(' or tokens[-1] != ')':
            utl.say_error(f"Bad syntax\nSyntax example: for var (0, 10)")

        if not defs.is_variable(tokens[1]):
            utl.say_error(f"For loop variable must be a declared variable: {tokens[1]}\nSyntax example: :var ; for var (0, 10)")

        v = defs.get_variable(tokens[1])

        if v.is_static:
            utl.say_error(f"For loop variable must be a local variable: {tokens[1]}\nSyntax example: :var ; for var (0, 10)")

        args = toks.split_func_args(tokens[3:-1])

        if len(args) != 2:
            utl.say_error(f"For loop must have exactly two arguments: debut and fin\nSyntax example: for var (0, 10)")

        # init the loop variable with the debut value
        fast_assignment = op.fast_assign_var(v, args[0])

        if fast_assignment:
            output.push(fast_assignment)
        else:
            output.push(op.calculate_rpn(args[0]))

            # move the result from the stack to the variable's memory location
            output.add("pops",
                    (0, defs.STACK_DEBUT_PTR),
                    (1, utl.to_u16(-v.offset)))
            
        debut_label = utl.get_new_label()
        next_label  = utl.get_new_label()
        fin_label   = utl.get_new_label()

        # push the loop fin value onto the stack
        output.push(op.calculate_rpn(args[1]))

        output.add_label(debut_label)

        # compare the loop variable with the fin value
        output.add("pushs",
                (0, defs.STACK_DEBUT_PTR),
                (1, utl.to_u16(-v.offset)))
        output.add("lt",
                (2, 0), (2, 1))
        output.add("pop",
                (0, defs.COND_RES_ADDR))
        
        output.add_goto(
            fin_label, (0, defs.COND_RES_ADDR)) # jump if the condition is false
        
        # compile the lines inside the for block
        closing_line = toks.locate_braces(lines, current_line)
        inner_output = compile_lines(lines[current_line + 2:closing_line], closing_line - current_line - 2, (next_label, fin_label))

        output.push(inner_output)
        output.add_comment(f"\nIncrement the loop variable {v.name}")

        output.add_label(next_label)
        output.add("pushs",
                (0, defs.STACK_DEBUT_PTR),
                (1, utl.to_u16(-v.offset)))
        output.add("add",
                (2, 0), (1, 1))
        output.add("pops",
                (0, defs.STACK_DEBUT_PTR),
                (1, utl.to_u16(-v.offset)))
        
        output.add_goto(
            debut_label, (1, 0)) # unconditional jump to the beginning of the for loop
        
        output.add_label(fin_label)

        return (output, closing_line - current_line + 1) # return the number of lines to skip

    elif tokens[0] == "break":
        if not labels:
            utl.say_error(f"Unexpected break statement outside of a loop")

        # add an unconditional jump to the end of the loop
        output.add_goto(labels[1], (1, 0))

    elif tokens[0] == "continue":
        if not labels:
            utl.say_error(f"Unexpected continue statement outside of a loop")

        # add an unconditional jump to the beginning of the loop
        output.add_goto(labels[0], (1, 0))

    elif tokens[0] in ("else", "elif"):
        utl.say_error(f"Unexpected {tokens[0]} statement outside of an if block\n" +
                        "Syntax example: if var == 0 { ... } elif var == 1 { ... } else { ... }")

    else:
        utl.say_error(f"Bad syntax\nUnknown command or variable: {tokens[0]}")

    return (output, 1)


def compile(lines: str):
    tokens_lines = []

    for lno, line in enumerate(lines.splitlines(), start=1):
        defs.CURRENT_LNO = lno

        line = line.strip()
        for t in toks.tokenize_line(line):
            tokens_lines.append((lno, t))

    blt.add_builtin_functions()

    output = out.output_code()
    output.push(compile_lines(tokens_lines, len(tokens_lines)))
    output.atdebut(op.init())
    output.push(op.fini())

    return output
