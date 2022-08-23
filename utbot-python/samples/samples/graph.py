from collections import deque


class Node:
    def __init__(self, name: str):
        self.name = name
        self.children: list[Node] = []

    def __str__(self):
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

