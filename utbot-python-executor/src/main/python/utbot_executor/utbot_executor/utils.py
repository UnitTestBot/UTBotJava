import dis
import enum
import os
import sys
import typing
from contextlib import contextmanager


class TraceMode(enum.Enum):
    Lines = 1
    Instructions = 2


@contextmanager
def suppress_stdout():
    with open(os.devnull, "w") as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout


def get_instructions(obj: object, start_line: int) -> typing.Iterator[tuple[int, int]]:
    def inner_get_instructions(x, current_line):
        for i, el in enumerate(dis.get_instructions(x)):
            if el.starts_line is not None:
                current_line = el.starts_line
            yield current_line, el.offset
            if any(t in str(type(el.argval)) for t in ["<class 'code'>"]):
                inner_get_instructions(el.argval, current_line)
    return inner_get_instructions(obj, start_line)


def filter_instructions(
    instructions: typing.Iterable[tuple[int, int]],
    mode: TraceMode = TraceMode.Instructions,
) -> list[tuple[int, int]]:
    if mode == TraceMode.Lines:
        return [(it, 2 * it) for it in {line for line, op in instructions}]
    return list(instructions)
