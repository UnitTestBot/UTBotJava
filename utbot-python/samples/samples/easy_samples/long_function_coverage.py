import time


def long_function(x: int):
    x += 4
    x /= 2
    x += 100
    x *= 3
    x -= 15
    time.sleep(2000)
    return x
