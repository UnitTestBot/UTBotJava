import math


class A:
    def __init__(self, x: int):
        self.x = x


def f(a: A):
    if a.x > 0:
        return 100500
    if math.pi * a.x == 0:
        return 2
    return a.x
