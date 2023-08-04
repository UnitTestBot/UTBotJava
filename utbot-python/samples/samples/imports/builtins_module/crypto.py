import hashlib
from collections import Counter


def f(word: str):
    m = hashlib.sha256()
    m.update(word.encode())
    code = m.hexdigest()
    if len(code) > len(word):
        return Counter(code)
    return Counter(word)


if __name__ == '__main__':
    print(f("fjasld"))
