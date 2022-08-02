import sys
import importlib.machinery
import types
import ast
import mypy.fastparse
import inspect


annotation = sys.argv[1]
project_root = sys.argv[2]
path = sys.argv[3]
for extra_path in sys.argv[4:]:
    sys.path.append(extra_path)


def walk_mypy_type(mypy_type):
    try:
        source = inspect.getfile(eval(mypy_type.name))
        in_project = source.startswith(project_root)
    except:
        in_project = False

    modname = eval(mypy_type.name).__module__
    if modname != "" and (not in_project):
        name = mypy_type.name.split('.')[-1]
        fullname = modname + '.' + name
    else:
        fullname = mypy_type.name

    result = fullname
    if len(mypy_type.args) != 0:
        arg_strs = []
        for arg in mypy_type.args:
            arg_strs.append(walk_mypy_type(arg))
        result += '[' + ','.join(arg_strs) + ']'
    return result


try:
    loader = importlib.machinery.SourceFileLoader("", path)
    mod = types.ModuleType(loader.name)
    loader.exec_module(mod)

    for name in dir(mod):
        globals()[name] = getattr(mod, name)

    mypy_type = mypy.fastparse.parse_type_string(annotation, annotation, -1, -1)
    print(walk_mypy_type(mypy_type))

except:
    print(annotation)
