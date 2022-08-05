import sys
import importlib
import mypy.fastparse
import typing
from typing import *


def main(annotation: str, module_of_annotation: str):
    def walk_mypy_type(mypy_type) -> str:
        try:
            modname = eval(mypy_type.name).__module__
            result = f'{modname}.{mypy_type.name}'
        except:
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
