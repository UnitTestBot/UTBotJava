class A:
    def __init__(self, val: int):
        self.description = val


class B:
    def __init__(self, val: complex):
        self.description = val

    def sqrt(self):
        return self.description ** 0.5
