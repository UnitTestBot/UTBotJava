from samples.named_arguments.named_arguments import g


class A:
    def __init__(self, x: int):
        self.x = x

    def __eq__(self, other):
        return self.x == other

    def __round__(self, n=None):
        if n is not None:
            return round(self.x, n)
        return round(self.x)

    def __pow__(self, power, modulo=None):
        if modulo is None:
            return self.x ** power
        return pow(self.x, power, modulo)

    def f1(self, x, y=1, *, z, t=2):
        if y == 0:
            return 100 * x + t
        else:
            x *= y
            if x % 2 == 0:
                y = g(x) + z
            return x + y + 100 / y