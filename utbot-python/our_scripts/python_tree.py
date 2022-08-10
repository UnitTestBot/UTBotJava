import inspect
import types
import typing
from itertools import zip_longest

JSON: typing.TypeAlias = typing.Union[int, float, str, bytes, list, dict]


def get_type(py_object: typing.Any) -> str:
    module = inspect.getmodule(type(py_object))
    return '{module}.{name}'.format(
        module='' if module is None else module.__name__,
        name=type(py_object).__name__,
    )


def get_type_name(type_: type) -> str:
    return '{module}.{name}'.format(
        module=type_.__module__,
        name=type_.__name__,
    )


def has_reduce(py_object: typing.Any) -> bool:
    if getattr(py_object, '__reduce__', None) is None:
        return False
    else:
        try:
            py_object.__reduce__()
            return True
        except TypeError:
            return False


def get_reduce(py_object: typing.Any) -> typing.Optional[JSON]:
    py_object_reduce = py_object.__reduce__()
    reduce_value = [
        default if obj is None else obj
        for obj, default in zip_longest(
            py_object_reduce,
            [None, [], {}, [], []],
            fillvalue=None
        )
    ]
    return {
        'constructor': get_type_name(reduce_value[0]),
        'args': [
            serialize(arg)
            for arg in reduce_value[1]
        ],
        'attrs': {
            attr: serialize(value)
            for attr, value in reduce_value[2].items()
        },
        'listitems': [
            serialize(item)
            for item in reduce_value[3]
        ],
        'dictitems': [
            (serialize(key), serialize(value))
            for key, value in reduce_value[4]
        ],
    }


def serialize(py_object: typing.Any) -> typing.Optional[JSON]:
    type_ = get_type(py_object)
    value: typing.Optional[JSON]

    if isinstance(py_object, types.NoneType):
        value = None
    elif isinstance(py_object, type):
        value = get_type_name(py_object)
    elif any(type(py_object) == t for t in (list, set, tuple)):
        value = [
            serialize(element) for element in py_object
        ]
    elif any(type(py_object) == t for t in (dict,)):
        value = {
            key: serialize(value) for key, value in py_object.items()
        }
    elif has_reduce(py_object):
        value = get_reduce(py_object)
    else:
        value = repr(py_object)

    return {
        'type': type_,
        'value': value,
    }
