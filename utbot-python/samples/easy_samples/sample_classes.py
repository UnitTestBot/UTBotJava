from dataclasses import dataclass


class A:
    def __init__(self, val: int):
        self.description = val


class B:
    def __init__(self, val: complex):
        self.description = val

    def sqrt(self):
        return self.description ** 0.5


@dataclass
class C:
    counter: int = 0

    def inc(self):
        self.counter += 1


class Outer:
    class Inner:
        a = 1

        def inc(self):
            self.a += 1

    def __init__(self):
        self.inner = Outer.Inner()

    def inc1(self):
        self.inner.inc()
