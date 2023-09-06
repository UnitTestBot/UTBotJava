import typing


def make_recursive_list(x: int):
    xs = [1, 2]
    xs.append(xs)
    xs.append(x)
    return xs


def make_recursive_dict(x: int, y: int):
    d = {1: 2}
    d[x] = d
    d[y] = x
    return d


if __name__ == '__main__':
    make_recursive_dict(3, 4)