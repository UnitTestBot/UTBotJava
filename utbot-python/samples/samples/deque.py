from collections import deque


def generate_people_deque(people_count: int):
    names = ['Alex', 'Bob', 'Cate', 'Daisy', 'Ed']
    if people_count > 5:
        people_count = 5
    return deque(sorted(names[:people_count]))