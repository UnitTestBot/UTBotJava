from typing import *
import collections
from enum import Enum
import datetime


XXX = TypeVar("XXX", "A", int)


class A(Generic[XXX]):
    self_: XXX

    def f(self, a, b: A[int]):
        self.y = b
        self_.x = b
        pass

    def g(self):
        self.x = 1


def square(collection: Iterable[int], x: XXX):
    result = set()
    for elem in collection:
        result.add(elem ** 2)
    return result


def not_annotated(x):
    return x


def same_annotations(x: int, y: int, a: List[Any], b: List[Any], c: List[int]):
    return x + y


def optional(x: Optional[int]):
    return x


def literal(x: Literal["w", "r"]):
    return x


class Color(Enum):
     RED = 1
     GREEN = 2
     BLUE = 3


def enum_literal(x: Literal[Color.RED, Color.GREEN]):
    return x


def abstract_set(x: AbstractSet[int]):
    return x


def mapping(x: Mapping[int, int]):
    return x


def sequence(x: Sequence[object]):
    return x


def supports_abs(x: SupportsAbs):
    return abs(x)


def tuple_(x: Tuple[int, str]):
    return x[1] + str(x[0])


if __name__ == "__main__":
    # print(square(collections.defaultdict(int)))
    # enum_literal(Color.BLUE)
    pass
