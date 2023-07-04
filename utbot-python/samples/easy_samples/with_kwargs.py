
class A:
    def __init__(self, x: int):
        self.x = x

    def __eq__(self, other):
        return self.x == other

    def f1(self, x, y=1, *, z, t=2):
        if y == 0:
            return 100 * x + t
        else:
            x *= y
            if x % 2 == 0:
                y = g(x) + z
            return x + y + 100 / y


def f(x, y=1, *, z, t=2):
    if y == 0:
        return 100 * x
    else:
        x *= y
        if x % 2 == 0:
            y = g(x) + z
        return x + y + 100 / y


def g(x):
    return x ** 2


if __name__ == '__main__':
    print(f(1, y=2, z=3))
