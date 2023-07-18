class WithEqual:
    def __init__(self, x: int):
        self.x = x

    def __eq__(self, other):
        return self.x == other.x


class WithoutEqual:
    def __init__(self, x: int):
        self.x = x


class WithoutEqualChild(WithoutEqual):
    def __init__(self, x: int):
        super().__init__(x)

    def __eq__(self, other):
        return self.x == other.x


def f1(a: WithEqual, b: WithoutEqual, c: WithoutEqualChild):
    return a.x + b.x + c.x
