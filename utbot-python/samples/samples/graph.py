from __future__ import annotations
from collections import deque
from typing import List


class Node:
    def __init__(self, name: str, children: List[Node]):
        self.name = name
        self.children = children

    def __repr__(self):
        return f'<Node: {self.name}>'

    def __eq__(self, other):
        if isinstance(other, Node):
            return self.name == other.name
        else:
            return False


def bfs(nodes: list[Node]):
    if len(nodes) == 0:
        return []

    visited = []
    queue = deque(nodes)
    while len(queue) > 0:
        node = queue.pop()
        if node not in visited:
            visited.append(node)
            for child in node.children:
                queue.append(child)
    return visited


if __name__ == '__main__':
    a = Node('a', [])
    b = Node('b', [])
    c = Node('c', [])
    a.children.append(b)
    b.children.append(c)
    print(bfs([a, b, c]))
