from typing import TypeVar, Generic
from logging import Logger

T = TypeVar('T', bound=bool)
U = TypeVar('U', str, object)


class LoggedVar(Generic[T]):
    def __init__(self, value: T, name: str) -> None:
        self.name = name
        self.value = value

    def set(self, new: T) -> None:
        self.value = new

    def get(self, x: U):
        return self.value, x


def func(x: T, y: U):
    if x:
        return y
    else:
        return 100
