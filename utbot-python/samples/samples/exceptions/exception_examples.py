import typing

from samples.exceptions.my_checked_exception import MyCheckedException


def init_array(n: int):
    try:
        a: typing.List[typing.Optional[int]] = [None] * n
        a[n-1] = n + 1
        a[n-2] = n + 2
        return a[n-1] + a[n-2]
    except ImportError:
        return -1
    except IndexError:
        return -2


def nested_exceptions(i: int):
    try:
        return check_all(i)
    except IndexError:
        return 100
    except ValueError:
        return -100


def check_positive(i: int):
    if i > 0:
        raise IndexError("Positive")
    return 0


def check_all(i: int):
    if i < 0:
        raise ValueError("Negative")
    return check_positive(i)


def throw_exception(i: int):
    r = 1
    if i > 0:
       r += 10
       r -= (i + r) / 0
    else:
        r += 100
    return r


def throw_my_exception(i: int):
    if i > 0:
        raise MyCheckedException("i > 0")
    return i ** 2

