import math


def calculate_function_value(x, y):
    """
    Calculate value `f`
              | sqrt(x - 2y)                 , x > 100
    f(x, y) = | (3x^2 - 2xy + y^2) / sin(x)  , -100 < x <= 100
              | (0.01 * x) ^ log2(y)         , x < -100
    """

    if x > 100:
        return math.sqrt(x - 2 * y)
    elif -100 < x <= 100:
        return (3*x**2 - 2*x*y + y**2) / math.sin(x)
    else:
        return (0.01 * x) ** math.log2(y)


def f(x):
    if x == 10:
        return 100
    if x == 20:
        return 200
    if x == 30:
        return 300
    if x == 40:
        return 400
    if x == 50:
        return 500
    if x == 60:
        return 600
    if x < 0:
        return x ** 2
    if x > 0:
        return 239
    return 100500