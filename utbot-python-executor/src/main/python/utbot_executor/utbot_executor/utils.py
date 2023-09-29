import dis
import os
import sys
import typing
from contextlib import contextmanager


@contextmanager
def suppress_stdout():
    with open(os.devnull, "w") as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout


def get_instructions(obj: object) -> typing.Iterator[tuple[int, int]]:
    def inner_get_instructions(x, current_line):
        for i, el in enumerate(dis.get_instructions(x)):
            if el.starts_line is not None:
                current_line = el.starts_line
            yield current_line, el.offset
            if "<class 'code'>" in str(type(el.argval)):
                inner_get_instructions(el.argval, current_line)
    return inner_get_instructions(obj, None)
