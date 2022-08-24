from __future__ import annotations
from itertools import product


class Matrix:
    def __init__(self, elements: list[list[float]]):
        self.elements = elements
        self.dim = (len(elements), len(elements[0]))

    def __repr__(self):
        return str(self.elements)

    def __add__(self, other: Matrix):
        if self.dim == other.dim:
            return Matrix([
                [
                    elem + other_elem for elem, other_elem in
                    zip(self.elements[i], other.elements[i])
                ]
                for i in range(self.dim[1])
            ])

    def __mul__(self, other):
        if isinstance(other, (int, float, complex)):
            return Matrix([
                [
                    elem * other for elem in
                    self.elements[i]
                ]
                for i in range(self.dim[1])
            ])

    def __matmul__(self, other):
        if isinstance(other, Matrix):
            if self.dim[1] == other.dim[0]:
                result = [[0 for _ in range(self.dim[0])] * other.dim[1]]
                for i, j in product(range(self.dim[0]), range(other.dim[1])):
                    result[i][j] = sum(
                        self.elements[i][k] * other.elements[k][j]
                        for k in range(self.dim[1])
                    )
                return Matrix(result)


if __name__ == '__main__':
    a = Matrix([[1, 2]])
    b = Matrix([[3], [4]])
    print(a @ b)
