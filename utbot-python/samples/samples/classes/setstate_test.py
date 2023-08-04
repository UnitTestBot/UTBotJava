import typing


class MyClass:
    __slots__ = ['a', 'b']

    def __init__(self, a: int, b: typing.Dict[int, str]):
        self.a = a
        self.b = b

    def __eq__(self, other):
        return self.a == other.a and self.b == other.b

    def __setstate__(self, state):
        self.a = state[1]['a']
        self.b = state[1]['b']


def make_my_class(a: int, b: typing.Dict[int, str]) -> MyClass:
    return MyClass(a, b)
