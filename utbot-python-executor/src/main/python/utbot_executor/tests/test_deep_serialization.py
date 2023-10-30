import collections
import dataclasses
import datetime
import importlib.metadata
import json
import pickle
import re
import sys
import typing

import pytest

from utbot_executor.deep_serialization import json_converter
from utbot_executor.deep_serialization.deep_serialization import (
    serialize_objects_dump,
    deserialize_objects,
)


def get_deserialized_obj(obj: typing.Any, imports: typing.List[str]):
    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    deserialized_objs = deserialize_objects(
        serialized_obj_ids, serialized_memory_dump, imports
    )
    return deserialized_objs[serialized_obj_ids[0]]


def template_test_assert(obj: typing.Any, imports: typing.List[str]):
    assert obj == get_deserialized_obj(obj, imports)


@pytest.mark.parametrize(
    "obj",
    [
        (1,),
        ("abc",),
        (1.23,),
        (False,),
        (True,),
        (b"123",),
        (r"1\n23",),
        ([1, 2, 3],),
        (["a", 2, 3],),
        ([],),
        ({1, 2},),
        (set(),),
        ({1: 2, "3": "4"},),
        ({},),
        ((1, 2, 3),),
        (tuple(),),
        (pickle.dumps(((2, [1, 2]), {})),),
    ],
)
def test_primitives(obj: typing.Any):
    template_test_assert(obj, [])


@pytest.mark.parametrize(
    "obj,imports",
    [
        (datetime.datetime(2023, 6, 23), ["datetime"]),
        (collections.deque([1, 2, 3]), ["collections"]),
        (collections.Counter("jflaskdfjslkdruovnerjf:a"), ["collections"]),
        (collections.defaultdict(int, [(1, 2)]), ["collections"]),
    ],
)
def test_with_imports(obj: typing.Any, imports: typing.List[str]):
    template_test_assert(obj, imports)


@dataclasses.dataclass
class MyDataClass:
    a: int
    b: str
    c: typing.List[int]
    d: typing.Dict[str, bytes]


@pytest.mark.parametrize(
    "obj,imports",
    [
        (
                MyDataClass(1, "a", [1, 2], {"a": b"c"}),
                ["tests.test_deep_serialization"],
        ),
        (
                MyDataClass(1, "a--------------\n\t", [], {}),
                ["tests.test_deep_serialization"],
        ),
    ],
)
def test_dataclasses(obj: typing.Any, imports: typing.List[str]):
    template_test_assert(obj, imports)


class MyClass:
    def __init__(self, a: int, b: str, c: typing.List[int], d: typing.Dict[str, bytes]):
        self.a = a
        self.b = b
        self.c = c
        self.d = d

    def __eq__(self, other):
        if not isinstance(other, MyClass):
            return False
        return (
                self.a == other.a
                and self.b == other.b
                and self.c == other.c
                and self.d == other.d
        )


class EmptyClass:
    def __eq__(self, other):
        return isinstance(other, EmptyClass)


class EmptyInitClass:
    def __init__(self):
        pass

    def __eq__(self, other):
        return isinstance(other, EmptyInitClass)


@pytest.mark.parametrize(
    "obj,imports",
    [
        (
                MyClass(1, "a", [1, 2], {"a": b"c"}),
                ["tests.test_deep_serialization"],
        ),
        (
                MyClass(1, "a--------------\n\t", [], {}),
                ["tests.test_deep_serialization"],
        ),
        (EmptyClass(), ["tests.test_deep_serialization"]),
        (EmptyInitClass(), ["tests.test_deep_serialization"]),
    ],
)
def test_classes(obj: typing.Any, imports: typing.List[str]):
    template_test_assert(obj, imports)


class MyClassWithSlots:
    __slots__ = ["a", "b", "c", "d"]

    def __init__(self, a: int, b: str, c: typing.List[int], d: typing.Dict[str, bytes]):
        self.a = a
        self.b = b
        self.c = c
        self.d = d

    def __eq__(self, other):
        if not isinstance(other, MyClassWithSlots):
            return False
        return (
                self.a == other.a
                and self.b == other.b
                and self.c == other.c
                and self.d == other.d
        )

    def __str__(self):
        return f"<MyClassWithSlots: a={self.a}, b={self.b}, c={self.c}, d={self.d}>"

    def __setstate__(self, state):
        for key, value in state[1].items():
            self.__setattr__(key, value)


@pytest.mark.parametrize(
    "obj,imports",
    [
        (
                MyClassWithSlots(1, "a", [1, 2], {"a": b"c"}),
                ["tests.test_deep_serialization", "copyreg"],
        ),
        (
                MyClassWithSlots(1, "a--------------\n\t", [], {}),
                ["tests.test_deep_serialization", "copyreg"],
        ),
    ],
)
def test_classes_with_slots(obj: typing.Any, imports: typing.List[str]):
    template_test_assert(obj, imports)


def test_comparable():
    obj = EmptyClass()
    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    memory_dump = json_converter.deserialize_memory_objects(serialized_memory_dump)
    assert memory_dump.objects[serialized_obj_ids[0]].comparable


def test_complex():
    obj = complex(real=float('-inf'), imag=float('nan'))
    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    memory_dump = json_converter.deserialize_memory_objects(serialized_memory_dump)
    assert not memory_dump.objects[serialized_obj_ids[0]].comparable


def test_complex_state():
    class A:
        def __init__(self, c):
            self.c = c

    obj = A(complex(real=float('-inf'), imag=float('nan')))
    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    memory_dump = json_converter.deserialize_memory_objects(serialized_memory_dump)
    deserialized_obj = memory_dump.objects[serialized_obj_ids[0]]
    assert not deserialized_obj.comparable
    state = memory_dump.objects[deserialized_obj.state].items
    field_value = memory_dump.objects[list(state.values())[0]]
    assert not field_value.comparable


class IncomparableClass:
    pass


def test_incomparable():
    obj = IncomparableClass()
    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    memory_dump = json_converter.deserialize_memory_objects(serialized_memory_dump)
    assert not memory_dump.objects[serialized_obj_ids[0]].comparable


def test_recursive_list():
    obj = [1, 2, 3]
    obj.append(obj)

    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    memory_dump = json_converter.deserialize_memory_objects(serialized_memory_dump)
    assert not memory_dump.objects[serialized_obj_ids[0]].comparable

    deserialized_objs = deserialize_objects(
        serialized_obj_ids,
        serialized_memory_dump,
        ["tests.test_deep_serialization", "copyreg"],
    )
    deserialized_obj = deserialized_objs[serialized_obj_ids[0]]
    assert deserialized_obj[0] == deserialized_obj[-1][0]


def test_recursive_list_in_tuple():
    ls = []
    ts = (ls, 1)
    ls.append(ts)

    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([ts], True)
    memory_dump = json_converter.deserialize_memory_objects(serialized_memory_dump)
    assert not memory_dump.objects[serialized_obj_ids[0]].comparable

    deserialized_objs = deserialize_objects(
        serialized_obj_ids,
        serialized_memory_dump,
        ["tests.test_deep_serialization", "copyreg"],
    )
    deserialized_obj = deserialized_objs[serialized_obj_ids[0]]
    assert isinstance(deserialized_obj, tuple)
    assert isinstance(deserialized_obj[0], list)
    assert deserialized_obj == deserialized_obj[0][0]
    assert deserialized_obj[1] == 1


def test_recursive_dict():
    obj = {1: 2}
    obj[3] = obj

    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    memory_dump = json_converter.deserialize_memory_objects(serialized_memory_dump)
    assert not memory_dump.objects[serialized_obj_ids[0]].comparable

    deserialized_objs = deserialize_objects(
        serialized_obj_ids,
        serialized_memory_dump,
        ["tests.test_deep_serialization", "copyreg"],
    )
    deserialized_obj = deserialized_objs[serialized_obj_ids[0]]
    assert deserialized_obj[1] == deserialized_obj[3][1]


def test_deep_recursive_list():
    inner_inner = [7, 8, 9]
    inner = [4, 5, 6]
    obj = [1, 2, 3]
    obj.append(inner)
    inner.append(inner_inner)
    inner_inner.append(obj)

    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    memory_dump = json_converter.deserialize_memory_objects(serialized_memory_dump)
    assert not memory_dump.objects[serialized_obj_ids[0]].comparable

    deserialized_objs = deserialize_objects(
        serialized_obj_ids,
        serialized_memory_dump,
        ["tests.test_deep_serialization", "copyreg"],
    )
    deserialized_obj = deserialized_objs[serialized_obj_ids[0]]
    assert deserialized_obj == deserialized_obj[-1][-1][-1]


class Node:
    def __init__(self, name: str):
        self.name = name
        self.children: typing.List[Node] = []

    def __eq__(self, other):
        return self.name == other.name


def test_recursive_object():
    node1 = Node("1")
    node2 = Node("2")
    node3 = Node("3")
    node1.children.append(node2)
    node2.children.append(node3)
    node3.children.append(node1)

    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump(
        [node1], True
    )
    deserialized_objs = deserialize_objects(
        serialized_obj_ids,
        serialized_memory_dump,
        ["tests.test_deep_serialization", "copyreg"],
    )
    deserialized_obj = deserialized_objs[serialized_obj_ids[0]]
    assert deserialized_obj == deserialized_obj.children[0].children[0].children[0]


@pytest.mark.parametrize(
    "obj,imports",
    [
        (
                collections.Counter("abcababa"),
                ["tests.test_deep_serialization", "collections"],
        ),
        (
                collections.UserDict({1: "a"}),
                ["tests.test_deep_serialization", "collections"],
        ),
        (
                collections.deque([1, 2, 3]),
                ["tests.test_deep_serialization", "collections"],
        ),
    ],
)
def test_collections(obj: typing.Any, imports: typing.List[str]):
    template_test_assert(obj, imports)


@pytest.mark.parametrize(
    "obj,strategy",
    [
        (1, "repr"),
        ("1", "repr"),
        ([1, 2], "list"),
        ({1, 2}, "list"),
        ((1, 2), "list"),
        ({1: 2}, "dict"),
        (collections.Counter("faksjdf"), "reduce"),
    ],
)
def test_strategy(obj: typing.Any, strategy: str):
    serialized_obj_ids, _, serialized_memory_dump = serialize_objects_dump([obj], True)
    deserialized_data = json.loads(serialized_memory_dump)
    assert deserialized_data["objects"][serialized_obj_ids[0]]["strategy"] == strategy


@pytest.mark.parametrize(
    "obj,imports",
    [
        (re.compile(r"\d+jflsf"), ["tests.test_deep_serialization", "re"]),
        (
                collections.abc.KeysView,
                ["tests.test_deep_serialization", "collections"],
        ),
        (
                collections.abc.KeysView({}),
                [
                    "tests.test_deep_serialization",
                    "collections",
                    "collections.abc",
                ],
        ),
        (
                importlib.metadata.SelectableGroups([["1", "2"]]),
                ["tests.test_deep_serialization", "importlib.metadata"],
        ),
    ],
)
def test_corner_cases(obj: typing.Any, imports: typing.List[str]):
    template_test_assert(obj, imports)


T = typing.TypeVar("T")


@pytest.mark.skipif(
    sys.version_info.major <= 3 and sys.version_info.minor < 11,
    reason="typing.TypeVarTuple (PEP 646) has been added in Python 3.11",
    )
def test_type_var_tuple():
    globals()["T2"] = typing.TypeVarTuple("T2")
    obj = typing.TypeVarTuple("T2")
    imports = ["tests.test_deep_serialization", "typing"]

    deserialized_obj = get_deserialized_obj(obj, imports)
    assert deserialized_obj.__name__ == obj.__name__


@pytest.mark.parametrize(
    "obj,imports",
    [
        (typing.TypeVar("T"), ["tests.test_deep_serialization", "typing"]),
        (T, ["tests.test_deep_serialization", "typing"]),
    ],
)
def test_type_var(obj: typing.Any, imports: typing.List[str]):
    deserialized_obj = get_deserialized_obj(obj, imports)
    assert deserialized_obj.__name__ == obj.__name__
