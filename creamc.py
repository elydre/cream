import compiler.tokens as toks
import compiler.output as out
import compiler.utils as utl
import compiler.defs as defs
import compiler.op as op

import argparse

def compile_lines(lines: list, size: int):
    current_line = 0
    output = out.output_code()

    while current_line < size:
        sub_output, to_skip = compile_line(lines, current_line)
        output.push(sub_output)
        current_line += to_skip

    return output

def compile_line(lines: list, current_line: int):
    global CURRENT_LNO

    CURRENT_LNO, tokens = lines[current_line]

    print(f"Tokens: {tokens}")
    output = out.output_code()
    output.add_comment(f"\nl{CURRENT_LNO:03}  {' '.join(tokens)}")

    if tokens[0] == defs.NEW_VAR:
        ptrlvl = 0
        while tokens[ptrlvl + 1] == '[':
            ptrlvl += 1

        if len(tokens) != 2 + ptrlvl * 2:
            if ptrlvl:
                utl.say_error(f"Bad pointer declaration\nSyntax example: {defs.NEW_VAR} [ptr_name]")
            else:
                utl.say_error(f"Bad variable declaration\nSyntax example: {defs.NEW_VAR} var_name")

        # check for closing brackets
        if ptrlvl and (ptrlvl * ']' != ''.join(tokens[2 + ptrlvl:])):
            utl.say_error(f"Bad pointer declaration\nSyntax example: {defs.NEW_VAR} [ptr_name]")

        var_name = tokens[1 + ptrlvl]
    
        # check if the variable already exists
        if defs.is_variable(var_name):
            utl.say_error(f"Variable already exists: {tokens[1 + ptrlvl]}")

        old_offset = defs.LOCAL_VARS["main"][-1].offset if defs.LOCAL_VARS["main"] else 0
        v = defs.variable(var_name, ptrlvl, old_offset + 1)
        v.add_to_local_variables()

        output.add("push",
            (1, 0))

    elif defs.is_variable(tokens[0]):
        if len(tokens) < 3 or tokens[1] != '=':
            utl.say_error(f"Bad variable assignment\nSyntax example: var_name = 123")

        v = defs.get_variable(tokens[0])

        # reverse polish notation (RPN) expression
        output.push(op.calculate_rpn(tokens[2:]))

        # move the result from the stack to the variable's memory location
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
        
        output.add("pop",
                (1, 0))

    elif defs.is_func(tokens[0]):
        f = defs.get_func(tokens[0])
        if len(tokens) < 4 or tokens[1] != '(' or tokens[-1] != ')':
            utl.say_error(f"Bad syntax\nSyntax example: {f.name}({', '.join(['var' + str(i + 1) for i in range(len(f.args))])})")

        output.push(op.call_func(f, tokens[2:-1]))

    elif tokens[0] == "if":
        if len(tokens) < 2:
            utl.say_error(f"Bad syntax\nSyntax example: if var == 0")

        # reverse polish notation (RPN) expression
        output.push(op.calculate_rpn(tokens[1:]))

        # pop the result from the stack to the conditional result memory location
        output.add("pop",
            (0, defs.COND_RES))

        # compile the lines inside the if block
        closing_line = toks.locate_braces(lines, current_line)
        inner_output = compile_lines(lines[current_line + 2:closing_line], closing_line - current_line - 2)

        fin_label = utl.get_new_label()

        # add a jump instruction to skip the if block if the condition is false
        output.add_goto(
            fin_label,
            (0, defs.COND_RES)) # jump if the condition is false

        output.push(inner_output)
        output.add_label(fin_label)

        return (output, closing_line - current_line + 1) # return the number of lines to skip

    elif tokens[0] == "while":
        if len(tokens) < 2:
            utl.say_error(f"Bad syntax\nSyntax example: while var < 10")

        debut_label = utl.get_new_label()

        # reverse polish notation (RPN) expression
        output.add_label(debut_label)
        output.push(op.calculate_rpn(tokens[1:]))

        # pop the result from the stack to the conditional result memory location
        output.add("pop",
            (0, defs.COND_RES))

        # compile the lines inside the while block
        closing_line = toks.locate_braces(lines, current_line)
        inner_output = compile_lines(lines[current_line + 2:closing_line], closing_line - current_line - 2)

        inner_output.add_goto(
            debut_label, (1, 0)) # unconditional jump to the beginning of the while loop

        fin_label = utl.get_new_label()

        output.add_goto(
            fin_label, (0, defs.COND_RES)) # jump if the condition is false

        output.push(inner_output)
        output.add_label(fin_label)

        return (output, closing_line - current_line + 1) # return the number of lines to skip

    else:
        utl.say_error(f"Bad syntax\nUnknown command or variable: {tokens[0]}")

    return (output, 1)


def compile(lines: str):
    global CURRENT_LNO

    tokens_lines = []

    for lno, line in enumerate(lines.splitlines(), start=1):
        CURRENT_LNO = lno

        line = line.strip()
        for t in toks.tokenize_line(line):
            tokens_lines.append((lno, t))

    output = out.output_code()
    output.push(op.init())
    output.push(compile_lines(tokens_lines, len(tokens_lines)))
    output.push(op.fini())

    return output

#=======================================

parser = argparse.ArgumentParser(description="Cream Compiler")
parser.add_argument("input_file", help="Input file to compile")
parser.add_argument("-o", "--output", help="Output file name", default="output.bin", dest="output_file")
parser.add_argument("-a", "--dump-asm", help="Dump assembly code to stdout", action="store_true", dest="dump_asm")
args = parser.parse_args()

with open(args.input_file, "r") as ifile:
    if not ifile:
        exit("Could not open input file")
    lines = ifile.read()

main_output = compile(lines)

ofile = open(args.output_file, "wb")

if not ofile:
    exit("Could not open output file")

if args.dump_asm:
    main_output.dump()

main_output.resolve_gotos()
# main_output.dump(hide_labels = True)
main_output.write(ofile)

ofile.close()
