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
        self.content = []
        self.stop_exception = StopIteration

        pair_iter = zip(iter_copy, range(IteratorWrapper.MAX_SIZE))
        while True:
            try:
                it = next(pair_iter)
                self.content.append(it[0])
            except Exception as exc:
                self.stop_exception = exc
                break

    def build_from_list(self, content: typing.List[T], stop_exception: Exception = StopIteration) -> None:
        self.content = content
        self.iterator = iter(content)
        self.stop_exception = stop_exception

    @staticmethod
    def from_list(content: typing.List[T], stop_iteration: Exception = StopIteration) -> IteratorWrapper:
        obj = IteratorWrapper.__new__(IteratorWrapper)
        obj.build_from_list(content, stop_iteration)
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
        return {"content": self.content, "stop_exception": self.stop_exception}

    def __setstate__(self, state) -> None:
        self.build_from_list(state["content"], state["stop_exception"])


if __name__ == "__main__":
    wrapper = IteratorWrapper(iter([1, 2, 3]))
    for i in wrapper:
        print(i)
    # copy.deepcopy(wrapper)
    # print(wrapper.__reduce__())
    # a = pickle.dumps(wrapper)
    # print(a)
    # print(str(pickle.loads(a)))
