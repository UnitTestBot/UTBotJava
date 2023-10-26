class InnerClass:
    x: int

    def __init__(self, x: int):
        self.x = x

    def f(self, y: int):
        return y**2 + self.x*y + 1
