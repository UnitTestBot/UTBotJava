from __future__ import annotations

import copyreg
import inspect
import logging
import re
import sys
import typing
from itertools import zip_longest
import pickle
from typing import Any, Callable, Dict, List, Optional, Set, Type, Iterable

from utbot_executor.deep_serialization.config import PICKLE_PROTO
from utbot_executor.deep_serialization.utils import (
    PythonId,
    get_kind,
    has_reduce,
    check_comparability,
    get_repr,
    has_repr,
    TypeInfo,
    get_constructor_kind,
    has_reduce_ex,
    get_constructor_info,
)


class MemoryObject:
    strategy: str
    typeinfo: TypeInfo
    comparable: bool
    is_draft: bool
    deserialized_obj: object
    obj: object

    def __init__(self, obj: object) -> None:
        self.is_draft = True
        self.typeinfo = get_kind(obj)
        self.obj = obj

    def _initialize(
        self, deserialized_obj: object = None, comparable: bool = True
    ) -> None:
        self.deserialized_obj = deserialized_obj
        self.comparable = comparable
        self.is_draft = False

    def initialize(self) -> None:
        self._initialize()

    def id_value(self) -> str:
        return str(id(self.obj))

    def __repr__(self) -> str:
        if hasattr(self, "obj"):
            return str(self.obj)
        return str(self.typeinfo)

    def __str__(self) -> str:
        return str(self.obj)

    @property
    def qualname(self) -> str:
        return self.typeinfo.qualname


class ReprMemoryObject(MemoryObject):
    strategy: str = "repr"
    value: str

    def __init__(self, repr_object: object) -> None:
        super().__init__(repr_object)
        self.value = get_repr(repr_object)

    def initialize(self) -> None:
        try:
            deserialized_obj = pickle.loads(pickle.dumps(self.obj))
            comparable = check_comparability(self.obj, deserialized_obj)
        except Exception:
            deserialized_obj = self.obj
            comparable = False

        super()._initialize(deserialized_obj, comparable)


class ListMemoryObject(MemoryObject):
    strategy: str = "list"
    items: List[PythonId] = []

    def __init__(self, list_object: object) -> None:
        self.items: List[PythonId] = []
        super().__init__(list_object)

    def initialize(self) -> None:
        serializer = PythonSerializer()
        self.deserialized_obj = []  # for recursive collections
        self.comparable = False  # for recursive collections

        for elem in self.obj:
            elem_id = serializer.write_object_to_memory(elem)
            self.items.append(elem_id)
            self.deserialized_obj.append(serializer[elem_id])

        deserialized_obj = self.deserialized_obj
        if self.typeinfo.fullname == "builtins.tuple":
            deserialized_obj = tuple(deserialized_obj)
        elif self.typeinfo.fullname == "builtins.set":
            deserialized_obj = set(deserialized_obj)

        comparable = all(serializer.get_by_id(elem).comparable for elem in self.items)

        super()._initialize(deserialized_obj, comparable)

    def __repr__(self) -> str:
        if hasattr(self, "obj"):
            return str(self.obj)
        return f"{self.typeinfo.kind}{self.items}"


class DictMemoryObject(MemoryObject):
    strategy: str = "dict"
    items: Dict[PythonId, PythonId] = {}

    def __init__(self, dict_object: object) -> None:
        self.items: Dict[PythonId, PythonId] = {}
        super().__init__(dict_object)

    def initialize(self) -> None:
        self.obj: Dict
        serializer = PythonSerializer()
        self.deserialized_obj = {}  # for recursive dicts
        self.comparable = False  # for recursive dicts

        for key, value in self.obj.items():
            key_id = serializer.write_object_to_memory(key)
            value_id = serializer.write_object_to_memory(value)
            self.items[key_id] = value_id
            self.deserialized_obj[serializer[key_id]] = serializer[value_id]

        deserialized_obj = self.deserialized_obj
        equals_len = len(self.obj) == len(deserialized_obj)
        comparable = equals_len and all(
            serializer.get_by_id(value_id).comparable
            for value_id in self.items.values()
        )

        super()._initialize(deserialized_obj, comparable)

    def __repr__(self) -> str:
        if hasattr(self, "obj"):
            return str(self.obj)
        return f"{self.typeinfo.kind}{self.items}"


class ReduceMemoryObject(MemoryObject):
    strategy: str = "reduce"
    constructor: TypeInfo
    args: PythonId
    state: PythonId
    listitems: PythonId
    dictitems: PythonId

    reduce_value: List[Any] = []

    def __init__(self, reduce_object: object) -> None:
        super().__init__(reduce_object)
        serializer = PythonSerializer()

        if has_reduce_ex(reduce_object):
            py_object_reduce = reduce_object.__reduce_ex__(PICKLE_PROTO)
        else:
            py_object_reduce = reduce_object.__reduce__()

        if isinstance(py_object_reduce, str):
            name = getattr(reduce_object, "__qualname__", None)
            if name is None:
                name = reduce_object.__name__
            module_name = pickle.whichmodule(reduce_object, name)
            try:
                __import__(module_name, level=0)
                module = sys.modules[module_name]
                obj2, parent = pickle._getattribute(module, name)
            except (ImportError, KeyError, AttributeError):
                raise pickle.PicklingError(
                    "Can't pickle %r: it's not found as %s.%s"
                    % (reduce_object, module_name, name)
                ) from None
            else:
                if obj2 is not reduce_object:
                    self.comparable = False
            typeinfo = TypeInfo(module_name, name)
            self.constructor = typeinfo
            self.deserialized_obj = obj2
            self.reduce_value = []
        else:
            self.reduce_value = [
                default if obj is None else obj
                for obj, default in zip_longest(
                    py_object_reduce, [None, [], {}, [], {}], fillvalue=None
                )
            ]

            constructor_arguments, callable_constructor = self.constructor_builder()

            self.constructor = get_constructor_info(callable_constructor, self.obj)
            logging.debug("Object: %s", self.obj)
            logging.debug("Type: %s", type(self.obj))
            logging.debug("Constructor: %s", callable_constructor)
            logging.debug("Constructor info: %s", self.constructor)
            logging.debug("Constructor args: %s", constructor_arguments)
            self.args = serializer.write_object_to_memory(constructor_arguments)

            if isinstance(constructor_arguments, Iterable):
                logging.debug("Constructor args: %s", constructor_arguments)
                self.deserialized_obj = callable_constructor(*constructor_arguments)

    def constructor_builder(self) -> typing.Tuple[typing.Any, typing.Callable]:
        constructor_kind = get_constructor_kind(self.reduce_value[0])

        is_reconstructor = constructor_kind.qualname == "copyreg._reconstructor"
        is_reduce_user_type = (
            len(self.reduce_value[1]) == 3
            and isinstance(self.reduce_value[1][0], type(self.obj))
            and self.reduce_value[1][1] is object
            and self.reduce_value[1][2] is None
        )
        is_reduce_ex_user_type = len(self.reduce_value[1]) == 1 and isinstance(
            self.reduce_value[1][0], type(self.obj)
        )
        is_user_type = is_reduce_user_type or is_reduce_ex_user_type
        is_newobj = constructor_kind.qualname in {
            "copyreg.__newobj__",
            "copyreg.__newobj_ex__",
        }

        logging.debug("Params: %s, %s, %s", is_reconstructor, is_user_type, is_newobj)

        obj_type = self.obj if isinstance(self.obj, type) else type(self.obj)

        callable_constructor: Callable
        constructor_arguments: Any

        if is_user_type and hasattr(self.obj, "__init__"):
            init_method = getattr(obj_type, "__init__")
            init_from_object = init_method is object.__init__
            logging.debug(
                "init_from_object = %s, signature_size = %s",
                init_from_object,
                len(inspect.signature(init_method).parameters),
            )
            if (
                not init_from_object
                and len(inspect.signature(init_method).parameters) == 1
            ) or init_from_object:
                logging.debug("init with one argument! %s", init_method)
                constructor_arguments = []
                callable_constructor = obj_type
                return constructor_arguments, callable_constructor

        # Special case
        if isinstance(self.obj, re.Pattern):
            constructor_arguments = (self.obj.pattern, self.obj.flags)
            callable_constructor = re.compile
            return constructor_arguments, callable_constructor
        # ----

        if is_newobj:
            constructor_arguments = self.reduce_value[1]
            callable_constructor = getattr(obj_type, "__new__")
            return constructor_arguments, callable_constructor

        if is_reconstructor and is_user_type:
            constructor_arguments = self.reduce_value[1]
            if (
                len(constructor_arguments) == 3
                and constructor_arguments[-1] is None
                and constructor_arguments[-2] == object
            ):
                del constructor_arguments[1:]
            callable_constructor = object.__new__
            return constructor_arguments, callable_constructor

        callable_constructor = self.reduce_value[0]
        constructor_arguments = self.reduce_value[1]
        return constructor_arguments, callable_constructor

    def initialize(self) -> None:
        serializer = PythonSerializer()

        self.comparable = True  # for recursive objects
        deserialized_obj = self.deserialized_obj
        if len(self.reduce_value) == 0:
            # It is global var
            self.args = serializer.write_object_to_memory(None)
            self.state = serializer.write_object_to_memory(None)
            self.listitems = serializer.write_object_to_memory(None)
            self.dictitems = serializer.write_object_to_memory(None)
        else:
            self.state = serializer.write_object_to_memory(self.reduce_value[2])
            self.listitems = serializer.write_object_to_memory(
                list(self.reduce_value[3])
            )
            self.dictitems = serializer.write_object_to_memory(
                dict(self.reduce_value[4])
            )

            state = serializer[self.state]
            if isinstance(state, dict):
                for key, value in state.items():
                    try:
                        setattr(deserialized_obj, key, value)
                    except AttributeError:
                        pass
            elif hasattr(deserialized_obj, "__setstate__"):
                deserialized_obj.__setstate__(state)
            elif isinstance(state, tuple) and len(state) == 2:
                _, slotstate = state
                if slotstate:
                    for key, value in slotstate.items():
                        try:
                            setattr(deserialized_obj, key, value)
                        except AttributeError:
                            pass

            items = serializer[self.listitems]
            if isinstance(items, Iterable):
                for item in items:
                    deserialized_obj.append(item)

            dictitems = serializer[self.dictitems]
            if isinstance(dictitems, Dict):
                for key, value in dictitems.items():
                    deserialized_obj[key] = value

        comparable = self.obj == deserialized_obj

        super()._initialize(deserialized_obj, comparable)


class MemoryObjectProvider(object):
    @staticmethod
    def get_serializer(obj: object) -> Optional[Type[MemoryObject]]:
        pass


class ListMemoryObjectProvider(MemoryObjectProvider):
    @staticmethod
    def get_serializer(obj: object) -> Optional[Type[MemoryObject]]:
        if any(type(obj) == t for t in (list, set, tuple, frozenset)):
            return ListMemoryObject
        return None


class DictMemoryObjectProvider(MemoryObjectProvider):
    @staticmethod
    def get_serializer(obj: object) -> Optional[Type[MemoryObject]]:
        if type(obj) == dict:
            return DictMemoryObject
        return None


class ReduceMemoryObjectProvider(MemoryObjectProvider):
    @staticmethod
    def get_serializer(obj: object) -> Optional[Type[MemoryObject]]:
        if has_reduce(obj):
            return ReduceMemoryObject
        return None


class ReduceExMemoryObjectProvider(MemoryObjectProvider):
    @staticmethod
    def get_serializer(obj: object) -> Optional[Type[MemoryObject]]:
        if has_reduce_ex(obj):
            return ReduceMemoryObject
        return None


class ReprMemoryObjectProvider(MemoryObjectProvider):
    @staticmethod
    def get_serializer(obj: object) -> Optional[Type[MemoryObject]]:
        if has_repr(obj):
            return ReprMemoryObject
        return None


class MemoryDump:
    objects: Dict[PythonId, MemoryObject]

    def __init__(self, objects: Optional[Dict[PythonId, MemoryObject]] = None):
        if objects is None:
            objects = {}
        self.objects = objects


class PythonSerializer:
    instance: PythonSerializer
    memory: MemoryDump
    created: bool = False

    visited: Set[PythonId] = set()

    providers: List[MemoryObjectProvider] = [
        ListMemoryObjectProvider,
        DictMemoryObjectProvider,
        ReduceMemoryObjectProvider,
        ReprMemoryObjectProvider,
        ReduceExMemoryObjectProvider,
    ]

    def __new__(cls):
        if not cls.created:
            cls.instance = super(PythonSerializer, cls).__new__(cls)
            cls.memory = MemoryDump()
            cls.created = True
        return cls.instance

    def clear(self):
        self.memory = MemoryDump()

    def get_by_id(self, id_: PythonId) -> MemoryObject:
        return self.memory.objects[id_]

    def __getitem__(self, id_: PythonId) -> object:
        return self.get_by_id(id_).deserialized_obj

    def clear_visited(self):
        self.visited.clear()

    def write_object_to_memory(self, py_object: object) -> PythonId:
        """Save serialized py_object to memory and return id."""

        id_ = PythonId(str(id(py_object)))

        if id_ in self.visited:
            return id_

        for provider in self.providers:
            serializer = provider.get_serializer(py_object)
            if serializer is not None:
                self.visited.add(id_)
                mem_obj = serializer(py_object)
                self.memory.objects[id_] = mem_obj
                mem_obj.initialize()
                return id_

        raise ValueError(f"Can not find provider for object {py_object}.")
