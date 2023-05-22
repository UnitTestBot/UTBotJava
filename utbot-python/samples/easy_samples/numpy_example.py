import typing

import numpy as np
import numpy.typing as npt


def transpose(arr: npt.ArrayLike):
    arr = np.array(arr)
    if len(arr.shape) == 0:
        return 239
    if arr.shape[0] == 1:
        return 100
    else:
        return arr.dot(arr.T)


def f(arr: np.ndarray[np.int64, np.dtype[np.float64]]):
    if len(arr.shape) > 0:
        return arr.shape[0], arr * 2
    return arr


def nf(x: npt.DTypeLike[np.int64]):
    return x


class A:
    pass


def f1(x: A):
    x.a = 100
    return str(x)