import dataclasses
import dis
import enum
import os
import sys
import typing
from contextlib import contextmanager


class TraceMode(enum.Enum):
    Lines = 1
    Instructions = 2


@dataclasses.dataclass
class UtInstruction:
    line: int
    offset: int
    global_offset: int

    def serialize(self) -> str:
        return ":".join(map(str, [self.line, self.offset, self.global_offset]))

    def __hash__(self):
        return hash((self.line, self.offset, self.global_offset))


@contextmanager
def suppress_stdout():
    with open(os.devnull, "w") as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout


def get_instructions(obj: object, start_line: int) -> typing.Iterator[UtInstruction]:
    def inner_get_instructions(x, current_line, offset):
        for i, el in enumerate(dis.get_instructions(x)):
            if el.starts_line is not None:
                current_line = el.starts_line
            yield UtInstruction(current_line, el.offset, el.offset + offset)
            if any(t in str(type(el.argval)) for t in ["<class 'code'>"]):
                inner_get_instructions(el.argval, current_line, el.offset + offset)
    return inner_get_instructions(obj, start_line, 0)


def filter_instructions(
    instructions: typing.Iterable[UtInstruction],
    mode: TraceMode = TraceMode.Instructions,
) -> list[UtInstruction]:
    if mode == TraceMode.Lines:
        return list({UtInstruction(it.line, 0, 0) for it in instructions})
    return list(instructions)
