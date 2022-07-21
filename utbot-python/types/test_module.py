from typing import Union
class A:
    def __init__(self, x):
        self.x = x
    def __add__(self, y: Union[int, 'A']):
        return self.x + y

def f(x, y):
    return x + y
