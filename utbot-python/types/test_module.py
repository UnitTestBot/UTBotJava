from typing import Union
class A:
    def __init__(self, x):
        self.x = x
    def __add__(self, y: Union[int, 'A']):
        return self.x + y

def f(x, y, z, a, b, c, d, e, f):
    if y % 2 == 0:
        x = 1
    z += "aba"
    a += [1]
    b = (1, [1, 2])
    A = c < "abc"
    B = "abc" == d
    e = {1, 2, 3}
    C = f == {1: 2}
    return x + y
