from __future__ import annotations
import itertools
import typing

T = typing.TypeVar("T")


class IteratorWrapper:
    iterator: typing.Iterator[T]
    content: typing.List[T]

    index = -1

    MAX_SIZE = 1_000

    def __init__(self, iterator: typing.Iterator[T]) -> None:
        self.iterator, iter_copy = itertools.tee(iterator)
        self.content = [it[0] for it in zip(iter_copy, range(IteratorWrapper.MAX_SIZE))]

    def build_from_list(self, content: typing.List[T]) -> None:
        self.content = content
        self.iterator = iter(content)

    @staticmethod
    def from_list(content: typing.List[T]) -> IteratorWrapper:
        obj = IteratorWrapper.__new__(IteratorWrapper)
        obj.build_from_list(content)
        return obj

    def __iter__(self):
        self.index = -1
        return self

    def __next__(self):
        if self.index + 1 >= len(self.content):
            raise StopIteration
        self.index += 1
        return self.content[self.index]

    def __eq__(self, other) -> bool:
        if isinstance(other, IteratorWrapper):
            return self.content == other.content
        return False

    def __str__(self) -> str:
        return f"IteratorWrapper({self.content})"

    def __getstate__(self) -> typing.Dict[str, typing.Any]:
        return {"content": self.content}

    def __setstate__(self, state) -> None:
        self.build_from_list(state["content"])


if __name__ == "__main__":
    import pickle, copy
    wrapper = IteratorWrapper(iter([1, 2, 3]))
    for i in wrapper:
        print(i)
    # copy.deepcopy(wrapper)
    # print(wrapper.__reduce__())
    # a = pickle.dumps(wrapper)
    # print(a)
    # print(str(pickle.loads(a)))
