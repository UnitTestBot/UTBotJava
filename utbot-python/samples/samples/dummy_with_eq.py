class Dummy:
    def __init__(self, value: int):
        self.field = value

    def __eq__(self, other):
        return self.field == other.field

    def propagate(self):
        return [self, self]
