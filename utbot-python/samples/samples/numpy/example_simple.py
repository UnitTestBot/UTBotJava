import numpy as np


def function_to_test_default(a: np.ndarray):
    if a.shape == (2, 1,):
        return 2
    return a.shape
