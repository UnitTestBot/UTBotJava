import heapq
from datetime import datetime
from typing import Any, List, Union, NoReturn

"""
Default functions suite: fully annotated.
"""


def id_(x: Any) -> Any:
    return x


def compare_with_5(x: int) -> bool:
    return x > 5


def add(x: int, y: int) -> int:
    return x + y


def add_with_unused_param(x: int, y: int, unused: int) -> int:
    return x + y


def append_exclamation_mark(s: str) -> str:
    return s + "!"


def append_two_ints_to_typing_list(l: List[int]) -> List[int]:
    return l + [1, -1]


def append_two_ints_to_builtin_list(l: list) -> list:
    return l + [1, -1]


def append_ints_and_chars(l: List[Union[int, str]]) -> List[Union[int, str]]:
    return l + [1, -1] + list("ab")


def format_data_labels(dates: List[datetime]) -> List[str]:
    if all(x.hour == 0 and x.minute == 0 for x in dates):
        return [x.strftime('%Y-%m-%d') for x in dates]
    else:
        return [x.strftime('%H:%M') for x in dates]


class ClassWithIntField:
    def __init__(self, int_field_value):
        self.int_field = int_field_value


class ClassWithAnnotatedIntField:
    def __init__(self, int_field: int):
        self.int_field = int_field


def inc_int_field(c: ClassWithIntField) -> int:
    c.int_field += 1
    return c.int_field


def call_heapify(ints: List[int]) -> List[int]:
    heapq.heapify(ints)
    return ints


def concatenate_args(*args: str) -> str:
    return "+".join(args)


def concatenate_args_and_kwargs(*args: str, **kwargs: str) -> str:
    return "+".join(args) + ";" + "?".join(kwargs.values())


def raise_exception(exc: Exception) -> NoReturn:
    raise exc
