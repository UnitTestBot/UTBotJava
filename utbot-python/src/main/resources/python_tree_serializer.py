import types
from itertools import zip_longest


class _PythonTreeSerializer:
    @staticmethod
    def get_type(py_object):
        if py_object is None:
            return 'types.NoneType'
        module = type(py_object).__module__
        return '{module}.{name}'.format(
            module=module,
            name=type(py_object).__name__,
        )

    @staticmethod
    def get_type_name(type_):
        if type_ is None:
            return 'types.NoneType'
        return '{module}.{name}'.format(
            module=type_.__module__,
            name=type_.__name__,
        )

    @staticmethod
    def has_reduce(py_object) -> bool:
        if getattr(py_object, '__reduce__', None) is None:
            return False
        else:
            try:
                py_object.__reduce__()
                return True
            except TypeError:
                return False

    @staticmethod
    def get_reduce(py_object):
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
            'constructor': _PythonTreeSerializer.get_type_name(reduce_value[0]),
            'args': [
                _PythonTreeSerializer.serialize(arg)
                for arg in reduce_value[1]
            ],
            'state': [
                (attr, _PythonTreeSerializer.serialize(value))
                for attr, value in reduce_value[2].items()
            ],
            'listitems': [
                _PythonTreeSerializer.serialize(item)
                for item in reduce_value[3]
            ],
            'dictitems': [
                (_PythonTreeSerializer.serialize(key), _PythonTreeSerializer.serialize(value))
                for key, value in reduce_value[4]
            ],
        }

    @staticmethod
    def serialize(py_object):
        type_ = _PythonTreeSerializer.get_type(py_object)

        if isinstance(py_object, type):
            value = _PythonTreeSerializer.get_type_name(py_object)
            strategy = "repr"
        elif any(type(py_object) == t for t in (list, set, tuple)):
            value = [
                _PythonTreeSerializer.serialize(element) for element in py_object
            ]
            strategy = "generic"
        elif any(type(py_object) == t for t in (dict,)):
            value = [
                [_PythonTreeSerializer.serialize(key), _PythonTreeSerializer.serialize(value)]
                for key, value in py_object.items()
            ]
            strategy = "generic"
        elif _PythonTreeSerializer.has_reduce(py_object):
            value = _PythonTreeSerializer.get_reduce(py_object)
            strategy = "reduce"
        else:
            value = repr(py_object)
            strategy = "repr"

        return {
            "type": type_,
            "value": value,
            "strategy": strategy,
        }
