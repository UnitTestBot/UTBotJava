from typing import *


def simple_annotations1(x: int, y: int):
    if x > 1:
        return x + y
    else:
        return y * 2


def simple_annotations2(x: str, y: str):
    if len(x) > 1:
        return x + y
    else:
        return y * 2


def simple_annotations3(x: str, y: bool):
    if y:
        return x
    else:
        return x * 2


def simple_annotations4(x: float, y: int):
    return x + y


def union(x: Union[int, str]):
    if isinstance(x, int):
        return 10 * x
    else:
        return x + "!"


def list_(x: List[int]):
    return len(x)


def tuple_1(x: Tuple[int, str]):
    return len(x)


def tuple_2(x: Tuple[int, ...]):
    return len(x)


