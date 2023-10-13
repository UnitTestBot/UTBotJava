def factorial(n: int):
    if n < 0:
        raise ValueError()
    if n == 0:
        return 1
    return n * factorial(n-1)


def fib(n: int):
    if n < 0:
        raise ValueError
    if n == 0:
        return 0
    if n == 1:
        return 1
    return fib(n-1) + fib(n-2)


def summ(fst: int, snd: int):
    def signum(x: int):
        return 0 if x == 0 else 1 if x > 0 else -1
    if snd == 0:
        return fst
    return summ(fst + signum(snd), snd - signum(snd))


def recursion_with_exception(n: int):
    if n < 42:
        recursion_with_exception(n+1)
    if n > 42:
        recursion_with_exception(n-1)
    raise ValueError


def first(n: int):
    def second(n: int):
        first(n)
    if n < 4:
        return
    second(n)

