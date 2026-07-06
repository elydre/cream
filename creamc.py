from compiler.output import output_file
from compiler.compile import compile
import compiler.defs as defs

import argparse


parser = argparse.ArgumentParser(description="Cream Compiler")
parser.add_argument("input_file", help="Input file to compile")
parser.add_argument("-o", "--output", help="Output file name", default="output.bin", dest="output_file")
parser.add_argument("-a", "--dump-asm", help="Dump assembly code to stdout", action="store_true", dest="dump_asm")
args = parser.parse_args()

try:
    with open(args.input_file, "r") as ifile:
        lines = ifile.read()
except FileNotFoundError:
    exit(f"Could not open input file: {args.input_file}")

main_output = compile(lines, args.input_file)

ofile = open(args.output_file, "wb")

if not ofile:
    exit("Could not open output file")

if args.dump_asm:
    main_output.dump()
    print("\n") 

main_output.resolve_labels()
# main_output.dump(hide_labels = True)

output = output_file()
output.add_section(0, output_file.section.TYPE_CODE, main_output.to_bytes())
if defs.STATIC_BYTES:
    output.add_section(defs.STATIC_ADDR, output_file.section.TYPE_DATA, defs.STATIC_BYTES)

output.write(ofile)
