from typing import TypeVar, Generic
from logging import Logger

T = TypeVar('T', bound=dict)


class LoggedVar(Generic[T]):
    def __init__(self, value: T, name: str) -> None:
        self.name = name
        self.value = value

    def set(self, new: T) -> None:
        self.value = new

    def get(self) -> T:
        return self.value
