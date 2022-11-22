from typing import *

T = TypeVar('T', int, str, dict)  #TypeVar('T', contravariant=True)
U = TypeVar('U')  #TypeVar('T', contravariant=True)

#T = TypeVar('T', bound=int)


#def f(x: T, y: T) -> T:
#    x = 1
#    return x + y


#f(1, '')


#def f() -> T:
#    return object()


#a: Set[int] = set()
#b: Set[T] = set(['a'])
#c: Set[Union[int, str]] = a.union(b)

#a: List[object] = []
#b: List[int] = []
#a = b


class P(Protocol):
    def f(self, x: int) -> int:
        ...


def f(x: P):
    pass


class C:
    def f(self, x: T) -> T:
        return x

class D:
    def f(self, x: Union[int, str]) -> None:
        return None

#f(D())

a: List[int] = [1]
b: List[Any] = ['a']
a += b
