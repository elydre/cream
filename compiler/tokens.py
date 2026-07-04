import compiler.utils as utl
import compiler.defs as defs

def tokenize_line(line):
    tokens = []
    lines = []

    in_string = False
    current_token = ""

    for char in line:
        if char == '"' and not in_string:
            in_string = True
            current_token += char

        elif char == '"' and in_string:
            in_string = False
            current_token += char
            tokens.append(current_token)
            current_token = ""

        elif in_string:
            current_token += char

        elif char.isspace():
            tokens.append(current_token)
            current_token = ""
        
        elif char == "=" and not current_token and tokens and tokens[-1] in defs.CHARS_SPE:
            tokens[-1] += char

        elif char in defs.CHARS_SPE:
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

def locate_braces(lines: list, current_line: int):
     # find the opening brace '{'
    if current_line + 1 >= len(lines) or lines[current_line + 1][1] != ['{']:
        utl.say_error("Bad syntax\nExpected '{' after 'if' statement")

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
        utl.say_error("Bad syntax\nExpected '}' after 'if' block")

    return closing_line

def split_func_args(tokens: list):
    args = []
    current_arg = []
    opening_brackets = 0

    for token in tokens:
        if token == ',' and opening_brackets == 0:
            args.append(current_arg)
            current_arg = []
        else:
            if token == '[':
                opening_brackets += 1
            elif token == ']':
                opening_brackets -= 1
                if opening_brackets < 0:
                    utl.say_error(f"Unclosed brackets in function arguments\nSyntax example: func(arg1, [ptr:offset_ptr[123]])")
            current_arg.append(token)

    if current_arg:
        args.append(current_arg)

    return args