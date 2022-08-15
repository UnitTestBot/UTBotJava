import sys
import importlib
import mypy.fastparse
import typing
from typing import *


def main(annotation: str, module_of_annotation: str):
    def walk_mypy_type(mypy_type) -> str:
        try:
            prefix = module_of_annotation + "."
            if mypy_type.name[:len(prefix)] == prefix:
                name = mypy_type.name[len(prefix):]
            else:
                name = mypy_type.name

            if eval(name) is None:
                result = "types.NoneType"
            else:
                modname = eval(name).__module__
                result = f'{modname}.{name}'

        except Exception as e:
            result = 'typing.Any'

        if len(mypy_type.args) != 0:
            arg_strs = [
                walk_mypy_type(arg)
                for arg in mypy_type.args
            ]
            result += f"[{', '.join(arg_strs)}]"

        return result

    mod = importlib.import_module(module_of_annotation)

    for name in dir(mod):
        globals()[name] = getattr(mod, name)

    mypy_type_ = mypy.fastparse.parse_type_string(annotation, annotation, -1, -1)
    print(walk_mypy_type(mypy_type_), end='')


def get_args():
    annotation = sys.argv[1]
    module_of_annotation = sys.argv[2]
    return annotation, module_of_annotation


if __name__ == '__main__':
    main(*get_args())
