class B:
    def __init__(self, val: complex):
        self.description = val

    def __eq__(self, other):
        return self.description == other.description

    def sqrt(self):
        return self.description ** 0.5