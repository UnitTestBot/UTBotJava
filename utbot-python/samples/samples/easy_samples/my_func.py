def my_func(x: int, xs: list[int]):
    if len(xs) == x:
        return x ** 2
    elif not xs:
        return x
    return len(xs)
