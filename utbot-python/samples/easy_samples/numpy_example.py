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


def f(arr: npt.NDArray[np.float64]):
    if len(arr.shape) > 0:
        return arr.shape[0], arr * 2
    return arr
