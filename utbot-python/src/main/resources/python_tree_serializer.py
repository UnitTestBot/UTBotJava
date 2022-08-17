import types
from itertools import zip_longest
import copyreg
import importlib
from collections import *


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
        constructor = _PythonTreeSerializer.get_type_name(reduce_value[0])
        args, deserialized_args = _PythonTreeSerializer.unzip_list([
            _PythonTreeSerializer.serialize(arg)
            for arg in reduce_value[1]
        ])
        state, deserialized_state = _PythonTreeSerializer.unzip_dict([
            (attr, _PythonTreeSerializer.serialize(value))
            for attr, value in reduce_value[2].items()
        ], skip_first=True)
        listitems, deserialized_listitems = _PythonTreeSerializer.unzip_list([
            _PythonTreeSerializer.serialize(item)
            for item in reduce_value[3]
        ])
        dictitems, deserialized_dictitems = _PythonTreeSerializer.unzip_dict([
            (_PythonTreeSerializer.serialize(key), _PythonTreeSerializer.serialize(value))
            for key, value in reduce_value[4]
        ])

        json_obj = {
            'constructor': constructor,
            'args': args,
            'state': state,
            'listitems': listitems,
            'dictitems': dictitems,
        }
        deserialized_obj = reduce_value[0](*deserialized_args)
        for key, value in deserialized_state.items():
            setattr(deserialized_obj, key, value)
        for item in deserialized_listitems:
            deserialized_obj.append(item)
        for key, value in deserialized_dictitems.items():
            deserialized_obj[key] = value

        return json_obj, deserialized_obj

    @staticmethod
    def get_module(name):
        return '.'.join(name.split('.')[:-1])

    # @staticmethod
    # def import_module(name):
    #     module_spec = importlib.util.find_spec(name)
    #     if module_spec is not None:
    #         module = importlib.util.module_from_spec(module_spec)
    #         module_spec.loader.exec_module(module)

    # @staticmethod
    # def deserialize_object(py_object):
    #     _PythonTreeSerializer.import_module(_PythonTreeSerializer.get_module(py_json['type']))
    #
    #     strategy = py_json['strategy']
    #     if strategy == 'repr':
    #         return
    #     elif strategy == 'generic':
    #         type_ = py_json['type']
    #         if type_ in 'builtins.list':
    #             return list(_PythonTreeSerializer.deserialize(element) for element in py_json['value'])
    #         elif type_ == 'builtins.tuple':
    #             return tuple(_PythonTreeSerializer.deserialize(element) for element in py_json['value'])
    #         elif type_ == 'builtins.set':
    #             return set(_PythonTreeSerializer.deserialize(element) for element in py_json['value'])
    #         elif type_ == 'builtins.dict':
    #             return dict(
    #                 (_PythonTreeSerializer.deserialize(element[0]), _PythonTreeSerializer.deserialize(element[0]))
    #                 for element in py_json['value']
    #             )
    #         else:
    #             return None
    #     else:
    #         obj_json = py_json['value']
    #
    #         _PythonTreeSerializer.import_module(_PythonTreeSerializer.get_module(obj_json['constructor']))
    #
    #         constuctor = eval(obj_json['constructor'])
    #         init_args = [_PythonTreeSerializer.deserialize(arg) for arg in obj_json['args']]
    #         state = {
    #             key: _PythonTreeSerializer.deserialize(value)
    #             for (key, value) in obj_json['state']
    #         }
    #         listitems = [_PythonTreeSerializer.deserialize(item) for item in obj_json['listitems']]
    #         dictitems = {
    #             _PythonTreeSerializer.deserialize(key): _PythonTreeSerializer.deserialize(value)
    #             for (key, value) in obj_json['dictitems']
    #         }
    #
    #         obj = constuctor(init_args)
    #
    #         for key, value in state.items():
    #             setattr(obj, key, value)
    #         for item in listitems:
    #             obj.append(item)
    #         for key, value in dictitems.items():
    #             obj[key] = value
    #
    #         return obj

    @staticmethod
    def serialize(py_object):
        type_ = _PythonTreeSerializer.get_type(py_object)

        if isinstance(py_object, type):
            value = _PythonTreeSerializer.get_type_name(py_object)
            strategy = 'repr'
            deserialized_obj = py_object
        elif any(type(py_object) == t for t in (list, set, tuple)):
            elements = [
                _PythonTreeSerializer.serialize(element) for element in py_object
            ]
            value, deserialized_obj = _PythonTreeSerializer.unzip_list(elements, type(py_object))
            strategy = 'generic'
        elif type(py_object) == dict:
            elements = [
                [_PythonTreeSerializer.serialize(key), _PythonTreeSerializer.serialize(value)]
                for key, value in py_object.items()
            ]
            value, deserialized_obj = _PythonTreeSerializer.unzip_dict(elements)
            strategy = 'generic'
        elif _PythonTreeSerializer.has_reduce(py_object):
            value, deserialized_obj = _PythonTreeSerializer.get_reduce(py_object)
            strategy = 'reduce'
        else:
            value = repr(py_object)
            deserialized_obj = py_object
            strategy = 'repr'

        comparable = py_object == deserialized_obj

        return {
            'type': type_,
            'value': value,
            'strategy': strategy,
            'comparable': comparable,
        }, deserialized_obj

    @staticmethod
    def unzip_list(elements, cast_second=list):
        if len(elements) == 0:
            first, second = [], []
        else:
            first, second = list(zip(*elements))
        return first, cast_second(second)

    @staticmethod
    def unzip_dict(elements, cast_second=dict, skip_first=False):
        if len(elements) == 0:
            first, second = [], []
        else:
            if skip_first:
                first = [[element[0], element[1][0]] for element in elements]
                second = [[element[0], element[1][1]] for element in elements]
            else:
                first = [[element[0][0], element[1][0]] for element in elements]
                second = [[element[0][1], element[1][1]] for element in elements]
        return first, cast_second(second)


if __name__ == '__main__':
    x = _PythonTreeSerializer.serialize([float('inf'), 101, UserList([1, 2, 3]), Counter("flkafksdf"), OrderedDict({1: 2, 4: "jflas"})])
    print(x[0]['comparable'])
