

class ComparableClass:
    def __init__(self, x):
        self.x = x

    def __eq__(self, other):
        return self.x == other.x


class BadClass:
    def __init__(self, x):
        self.x = x


def return_bad_class(x: int):
    return BadClass(x)


def return_comparable_class(x: int):
    return ComparableClass(x)


def primitive_list(x: int):
    return [x] * 10


def primitive_set(x: int):
    return set(x+i for i in range(5))


def primitive_dict(x: str, y: int):
    return {x: y}


def comparable_list(length: int):
    return [ComparableClass(x) for x in range(max(length, 10))]


def bad_list(length: int):
    return [BadClass(x) for x in range(max(length, 10))]
