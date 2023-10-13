from dataclasses import dataclass


@dataclass
class Inner:
    x: int


class A:
    def __init__(self, x: Inner):
        self.x = x

    def f(cls, self, x: Inner):
        self.x += 1
        return cls.x.x, self, x
