from __future__ import annotations
from itertools import product
from typing import List


class MatrixException(Exception):
    def __init__(self, description):
        self.description = description


class Matrix:
    def __init__(self, elements: List[List[float]]):
        self.dim = (
            len(elements),
            max(len(elements[i]) for i in range(len(elements)))
            if len(elements) > 0 else 0
        )
        self.elements = [
            row + [0] * (self.dim[1] - len(row))
            for row in elements
        ]

    def __repr__(self):
        return str(self.elements)

    def __eq__(self, other):
        if isinstance(other, Matrix):
            return self.elements == other.elements

    def __add__(self, other: Matrix):
        if self.dim == other.dim:
            return Matrix([
                [
                    elem + other_elem for elem, other_elem in
                    zip(self.elements[i], other.elements[i])
                ]
                for i in range(self.dim[0])
            ])

    def __mul__(self, other):
        if isinstance(other, (int, float, complex)):
            return Matrix([
                [
                    elem * other for elem in
                    self.elements[i]
                ]
                for i in range(self.dim[0])
            ])
        else:
            raise MatrixException("Wrong Type")

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
        else:
            raise MatrixException("Wrong Type")

    def is_diagonal(self) -> bool:
        if self.dim[0] != self.dim[1]:
            raise MatrixException("Bad matrix")

        for i, row in enumerate(self.elements):
            for j, elem in enumerate(row):
                if i != j and elem != 0:
                    return False
        return True


if __name__ == '__main__':
    a = Matrix([[1., 2.]])
    b = Matrix([[3.], [4.]])
    print(a @ b)
