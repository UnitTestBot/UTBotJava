import collections
import heapq
import typing
from typing import *
from dataclasses import dataclass


def empty_():
    pass


def zero_division(x):
    return x / 0


def repr_test(x):
    x *= 100
    return [1, x + 1, collections.UserList([1, 2, 3]), collections.Counter("flkafksdf"), collections.OrderedDict({1: 2, 4: "jflas"})]


def str_test(x):
    x += '1"23'
    x += "flskjd'jfslk"
    if len(x.split('.')) == 1:
        return '1"23'
    else:
        return """100''500"""


def empty():
    return 1


def id_(x):
    return x


def f(x, y, z, a, c, d, e, g, h, i):
    if y % 2 == 0:
        x = 1 + y
    z += "aba"
    a += [2] + list("str") + i
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
    y = len(x)
    return bytes(x, 'utf-8')


def c(x):
    return heapq.heapify(x)


def d(x: Optional[int]):
    return x


def k(x: typing.Any):
    if x == complex(1):
        return x


#  interesting case with sets
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


def list_of_list(x: List[List[InventoryItem]]):
    return x
