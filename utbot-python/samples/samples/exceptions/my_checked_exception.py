class MyCheckedException(Exception):
    def __init__(self, x: str):
        self.x = x

    def method(self, y: str):
        return self.x == y
