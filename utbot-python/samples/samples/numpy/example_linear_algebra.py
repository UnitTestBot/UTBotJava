import numpy as np


def lower_upper_decomposition(table: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    rows, columns = np.shape(table)
    if rows != columns:
        msg = (
            "'table' has to be of square shaped array but got a "
            f"{rows}x{columns} array:\n{table}"
        )
        raise ValueError(msg)
    lower = np.zeros((rows, columns))
    upper = np.zeros((rows, columns))
    for i in range(columns):
        for j in range(i):
            total = np.sum(lower[i, :i] * upper[:i, j])
            if upper[j][j] == 0:
                raise ArithmeticError("No LU decomposition exists")
            lower[i][j] = (table[i][j] - total) / upper[j][j]
        lower[i][i] = 1
        for j in range(i, columns):
            total = np.sum(lower[i, :i] * upper[:i, j])
            upper[i][j] = table[i][j] - total
    return lower, upper
