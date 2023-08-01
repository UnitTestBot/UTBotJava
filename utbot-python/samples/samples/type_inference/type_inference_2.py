def g(x):

    def f(y):
        if y in [0, 100, 200, 500]:
            return y // 100

    if f(x) > 10:
        return x ** 2

