import copy
import math
import typing


def summ(a: typing.SupportsInt, b: typing.SupportsInt):
    return int(a) + int(b)


def create_table(a: int):
    table = []
    for i in range(a):
        row = []
        for j in range(a):
            row.append(i * j)
        table.append(copy.deepcopy(row))
    return table


def operations(a: int):
    if a > 1024:
        return math.log2(a)
    if a > 512:
        return math.exp(a)
    if a > 256:
        return math.isinf(a)
    if a > 128:
        return math.e * a
    return math.sqrt(a)


def check_order(a: int, b: int, c: int):
    return a < b < c

