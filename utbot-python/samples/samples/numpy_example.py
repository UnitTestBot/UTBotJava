import numpy


def numpy_operations(array):
    return [
        numpy.argmax(array),
        numpy.argmax(array, axis=1),
        numpy.argmax(array, axis=0),
        numpy.log2(array),
        numpy.dot(array, [[list(range(array.ndim))]])
    ]
