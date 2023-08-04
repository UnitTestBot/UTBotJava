import typing


class MyClass:
    def __init__(self, x: int):
        self.x = x

    def __eq__(self, other):
        return self.x == other.x

    def __hash__(self):
        return hash(self.x)


def f(x: typing.Tuple[int, ...]):
    if len(x) == 0:
        return "Empty!"
    return len(x)


def f1(x: typing.Tuple[int, int, int]):
    if len(x) != 3:
        return "Very bad input!!!"
    return x[0] + x[1] + x[2]


def g(x: typing.Tuple[MyClass, ...]):
    if len(x) == 0:
        return "Empty!"
    return len(x)


def g1(x: typing.Tuple[MyClass, MyClass]):
    if len(x) != 2:
        return "Very bad input!!!"
    return x[0].x + x[1].x
