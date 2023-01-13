import collections
import numpy
from typing import *


class P(Protocol):
    def f(self, x: int) -> dict:
        ...


class S:
    def f(self, x: Union[int, str]) -> collections.Counter:
        return collections.Counter([x])


class S1:
    def f(self, x: Union[int, str]) -> object:
        return collections.Counter([x])


def func_for_P(x: P) -> None:
    return None


func_for_P(S())


class R(Protocol):
    def f(self) -> R:
        ...


class RImpl:
    def f(self) -> RImpl:
        return self


def func_for_R(x: R) -> None:
    return None


func_for_R(RImpl())


a: List[int] = []


T = TypeVar('T')


def func_abs(x: SupportsAbs[T]):
    return abs(x)


b: int = 10
