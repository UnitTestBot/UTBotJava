import typing


class MyClass:
    def __init__(self, x: int):
        self.x = x

    def __eq__(self, other):
        return self.x == other.x

    def __hash__(self):
        return hash(self.x)


def f(x: typing.Set[int]):
    if len(x) == 0:
        return "Empty!"
    return len(x)


def g(x: typing.Set[MyClass]):
    if len(x) == 0:
        return "Empty!"
    return len(x)
