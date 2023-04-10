import typing

import numpy as np


def transpose(arr: np.array):
    if arr.shape[0] == 1:
        return 100
    else:
        return arr.dot(arr.T)


def f(arr: np.array):
    if len(arr.shape) > 0:
        return arr.shape[0], arr * 2
    return arr
