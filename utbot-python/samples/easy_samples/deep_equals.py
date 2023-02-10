from __future__ import annotations
from typing import List


class ComparableClass:
    def __init__(self, x):
        self.x = x

    def __eq__(self, other):
        return self.x == other.x


class BadClass:
    def __init__(self, x):
        self.x = x


def return_bad_class(x: int):
    return BadClass(x)


def return_comparable_class(x: int):
    return ComparableClass(x)


def primitive_list(x: int):
    return [x] * 10


def primitive_set(x: int):
    return set(x+i for i in range(5))


def primitive_dict(x: str, y: int):
    return {x: y}


def comparable_list(length: int):
    return [ComparableClass(x) for x in range(min(length, 10))]


def bad_list(length: int):
    return [BadClass(x) for x in range(min(length, 10))]


class Node:
    name: str
    children: List[Node]

    def __init__(self, name: str):
        self.name = name
        self.children = []

    def __str__(self):
        return f'<Node: {self.name}>'

    def __eq__(self, other):
        if isinstance(other, Node):
            return self.name == other.name
        else:
            return False


def cycle(x: str):
    a = Node(x + '_a')
    b = Node(x + '_b')
    a.children.append(b)
    b.children.append(a)
    return a


def cycle2(x: str):
    a = Node(x + '_a')
    b = Node(x + '_b')
    c = Node(x + '_c')
    a.children.append(b)
    b.children.append(c)
    c.children.append(a)
    return a
