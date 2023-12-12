from __future__ import annotations
import dataclasses
import enum
import os
import sys
import typing
from contextlib import contextmanager
from types import CodeType


class TraceMode(enum.Enum):
    Lines = 1
    Instructions = 2


@dataclasses.dataclass
class UtInstruction:
    line: int
    offset: int
    from_main_frame: bool

    def serialize(self) -> str:
        return ":".join(map(str, [self.line, self.offset, int(self.from_main_frame)]))

    def __hash__(self):
        return hash((self.line, self.offset, self.from_main_frame))


@contextmanager
def suppress_stdout():
    with open(os.devnull, "w") as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout


def get_instructions(obj: CodeType) -> list[UtInstruction]:
    return [UtInstruction(line, start_offset, True) for start_offset, _, line in obj.co_lines() if None not in {start_offset, line}]


def filter_instructions(
    instructions: typing.Iterable[UtInstruction],
    mode: TraceMode = TraceMode.Instructions,
) -> list[UtInstruction]:
    if mode == TraceMode.Lines:
        return list({UtInstruction(it.line, 0, True) for it in instructions})
    return list(instructions)


def get_lines(instructions: typing.Iterable[UtInstruction]) -> list[int]:
    return [instruction.line for instruction in filter_instructions(instructions)]
