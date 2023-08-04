import dataclasses
import typing


@dataclasses.dataclass
class IntPair:
    fst: int
    snd: int


def concat(a: str, b: str):
    return a + b


def concat_pair(pair: IntPair):
    return pair.fst + pair.snd


def string_constants(s: str):
    return "String('" + s + "')"


def contains(s: str, t: str):
    return t in s


def const_contains(s: str):
    return "ab" in s


def to_str(a: int, b: int):
    if a > b:
        return str(a)
    else:
        return str(b)


def starts_with(s: str):
    if s.startswith("1234567890"):
        s = s.replace("3", "A")
    else:
        s = s.strip()

    if s[0] == "x":
        return s
    else:
        return s.upper()


def join_str(strings: typing.List[str]):
    return "--".join(strings)