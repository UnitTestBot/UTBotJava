from __future__ import annotations
import dataclasses
import importlib
import logging
import pickle
from typing import NewType

from utbot_executor.deep_serialization.config import PICKLE_PROTO

PythonId = NewType("PythonId", str)


@dataclasses.dataclass
class TypeInfo:
    module: str
    kind: str

    def __init__(self, module: str, kind: str):
        if module is None:
            logging.error("Module is None")
        self.module = module
        self.kind = kind

    @property
    def qualname(self):
        if self.module == "" or self.module == "builtins":
            return self.kind
        return f"{self.module}.{self.kind}"

    @property
    def fullname(self):
        if self.module == "":
            return self.kind
        else:
            return f"{self.module}.{self.kind}"

    @staticmethod
    def from_str(representation: str) -> TypeInfo:
        if "." in representation:
            return TypeInfo(
                representation.rsplit(".", 1)[0], representation.rsplit(".", 1)[1]
            )
        return TypeInfo("", representation)

    def __str__(self):
        return self.qualname


def check_comparability(py_object: object, deserialized_py_object: object) -> bool:
    return py_object == deserialized_py_object


def get_kind(py_object: object) -> TypeInfo:
    """Get module and name of type"""
    if py_object is None:
        return TypeInfo("types", "NoneType")
    if isinstance(py_object, type):
        return TypeInfo(py_object.__module__, py_object.__qualname__)
    if callable(py_object):
        return TypeInfo("typing", "Callable")
    module = type(py_object).__module__
    qualname = type(py_object).__qualname__
    return TypeInfo(module, qualname)


def get_constructor_kind(py_object: object) -> TypeInfo:
    """Get module and name of object"""
    if py_object is None:
        return TypeInfo("types", "NoneType")
    if isinstance(py_object, type):
        return TypeInfo(py_object.__module__, py_object.__qualname__)
    if callable(py_object):
        return TypeInfo(py_object.__module__, py_object.__qualname__)
    module = type(py_object).__module__
    qualname = type(py_object).__qualname__
    return TypeInfo(module, qualname)


def get_constructor_info(constructor: object, obj: object) -> TypeInfo:
    if constructor == object.__init__:
        result = TypeInfo("builtins", "object.__new__")
    elif constructor == object.__new__:
        result = TypeInfo("builtins", "object.__new__")
    elif constructor.__module__ is None:
        result = TypeInfo("builtins", "object.__new__")
    elif constructor is None:
        result = TypeInfo("types", "NoneType")
    else:
        result = TypeInfo(constructor.__module__, constructor.__qualname__)

    if result.kind == "object.__new__" and obj.__new__.__module__ is None:
        result = TypeInfo(obj.__module__, f"{obj.__class__.__name__}.__new__")
    return result


def has_reduce(py_object: object) -> bool:
    reduce = getattr(py_object, "__reduce__", None)
    if reduce is None:
        return False
    try:
        reduce()
        return True
    except TypeError:
        return False
    except pickle.PicklingError:
        return False
    except Exception:
        return False


def has_reduce_ex(py_object: object) -> bool:
    reduce_ex = getattr(py_object, "__reduce_ex__", None)
    if reduce_ex is None:
        return False
    try:
        reduce_ex(PICKLE_PROTO)
        return True
    except TypeError:
        return False
    except pickle.PicklingError:
        return False
    except Exception:
        return False


def get_repr(py_object: object) -> str:
    if isinstance(py_object, type):
        return str(get_kind(py_object))
    if isinstance(py_object, float):
        if repr(py_object) == "nan":
            return "float('nan')"
        if repr(py_object) == "inf":
            return "float('inf')"
        if repr(py_object) == "-inf":
            return "float('-inf')"
        return repr(py_object)
    if isinstance(py_object, complex):
        return (
            f"complex(real={get_repr(py_object.real)}, imag={get_repr(py_object.imag)})"
        )
    return repr(py_object)


def add_imports(module: str):
    for i in range(1, module.count(".") + 2):
        submodule_name = ".".join(module.split(".", maxsplit=i)[:i])
        if submodule_name not in globals():
            try:
                globals()[submodule_name] = importlib.import_module(submodule_name)
            except ModuleNotFoundError:
                pass


def check_eval(py_object: object) -> bool:
    module = get_kind(py_object).module
    add_imports(module)
    try:
        eval(get_repr(py_object))
        return True
    except Exception:
        return False


def has_repr(py_object: object) -> bool:
    reprable_types = [
        type(None),
        int,
        bool,
        float,
        bytes,
        bytearray,
        str,
        # tuple,
        # list,
        # dict,
        # set,
        # frozenset,
        type,
    ]
    if type(py_object) in reprable_types:
        return True

    if check_eval(py_object):
        repr_value = get_repr(py_object)
        evaluated = eval(repr_value)
        if get_repr(evaluated) == repr_value:
            return True

    return False


def getattr_by_path(py_object: object, path: str) -> object:
    current_object = py_object
    for layer in path.split("."):
        current_object = getattr(current_object, layer)
    return current_object
