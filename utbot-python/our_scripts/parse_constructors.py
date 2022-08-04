import ast as AST123
import astor.code_gen
import sys as SYS123
import os as OS123

search_for = SYS123.argv[1]
uses = set()

for (dirpath, dirnames, filenames) in  OS123.walk('.'):
    for filename in filenames:
        if not filename.endswith('.py'):
            continue

        try:
            with open(filename, "r") as F123:
                code = "".join(F123.readlines())
            root = AST123.parse(code)
        except:
            continue

        class MyVisitor(AST123.NodeVisitor):

            def visit(SELF123, node):
                if isinstance(node, AST123.Call) \
                   and isinstance(node.func, AST123.Name) \
                   and node.func.id == search_for:

                    uses.add(astor.code_gen.to_source(node))
                    
                return super().visit(node)


        MyVisitor().visit(root)

evaled = set()

for X123 in uses:
    to_print = True
    if '--check' in SYS123.argv:
        try:
            oldsz = len(evaled)
            evaled.add(eval(X123))
            if oldsz == len(evaled):
                to_print = False
        except:
            continue
    if to_print:
        print("      \"" + X123[:-1] + '",')

# print(evaled)
