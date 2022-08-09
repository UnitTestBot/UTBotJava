import heapq
import typing
from typing import *
from dataclasses import dataclass


def f(x, y, z, a, b, c, d, e, g, h):
    if y % 2 == 0:
        x = 1 + y
    z += "aba"
    a += [1] + list("str") + a
    b = (1, [1, 2])
    A = c < "abc"
    B = "abc" == d
    e = {1, 2, 3}
    C = g == {1: 2}
    h += int("777")
    return x + y


def g(x: List[int], y: List):
    y[0] += 1
    return x, y


def i(x: Dict[int, int]):
    return x[0]


def j(x: Set[int]):
    return x


def h(x):
    if x < 123:
        return 1
    return 2


class A:
    def __init__(self, val: int):
        self.description = val


class B:
    def __init__(self, val: complex):
        self.description = val

    def sqrt(self):
        return self.description ** 0.5


def a(x):
    x.description += 1
    return x.description


def sqrt(x):
    return x.sqrt()


@dataclass
class InventoryItem:
    name: str


def inv(x):
    return x.name + "aba"  # interesting case with io.BytesIO


def b(x, y):
    y = len(x)  # for now we don't consider that len returns int
    return bytes(x, 'utf-8')


def c(x):
    return heapq.heapify(x)


def d(x: Optional[int]):
    return x


def k(x: typing.Any):
    if x == complex(1):
        return x


def set_small_data_labels(dates):
    if all(x.hour == 0 and x.minute == 0 for x in dates):
        return [x.strftime('%Y-%m-%d') for x in dates]
    else:
        return [x.strftime('%H:%M') for x in dates]


# bad function
def m(x):
    x = frozenset()
    return len(x + 1)


# very bad function
def n(x, y):
    y = (-x) + 1
    x *= 10
    # z = -x
    print(x)
    x = len([1])
    if y == len([1]):
        y += print()
    return x.description

