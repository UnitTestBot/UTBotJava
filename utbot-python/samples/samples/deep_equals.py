class ComparableClass:
    def __init__(self, x):
        self.x = x

    def __eq__(self, other):
        return self.x == other.x


class IncomparableClass:
    def __init__(self, x):
        self.x = x

    def __eq__(self, other):
        return id(self) == id(other)


def comparable_list(length: int):
    return [ComparableClass(x) for x in range(min(length, 10))]


def incomparable_list(length: int):
    return [IncomparableClass(x) for x in range(min(length, 10))]
