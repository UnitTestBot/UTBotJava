from collections import deque


def generate_people_queue(people_count: int):
    names = ['Alex', 'Bob', 'Cate', 'Daisy', 'Ed']
    return deque(sorted(names[:min(len(names), people_count)]))