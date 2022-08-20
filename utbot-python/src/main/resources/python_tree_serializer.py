import copy
import types
from itertools import zip_longest
import copyreg
import importlib


class _PythonTreeSerializer:
    class MemoryObj:
        def __init__(self, json):
            self.json = json
            self.deserialized_obj = None
            self.comparable = False
            self.is_draft = True

    def __init__(self):
        self.memory = {}

    def memory_view(self):
        return ' | '.join(f'{id_}: {obj.deserialized_obj}' for id_, obj in self.memory.items())

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

    def save_to_memory(self, id_, py_json, deserialized_obj):
        mem_obj = _PythonTreeSerializer.MemoryObj(py_json)
        mem_obj.deserialized_obj = deserialized_obj
        self.memory[id_] = mem_obj
        return mem_obj

    def get_reduce(self, py_object):
        id_ = id(py_object)

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
            self.serialize(arg)
            for arg in reduce_value[1]
        ])
        json_obj = {
            'id': id_,
            'type': _PythonTreeSerializer.get_type(py_object),
            'constructor': constructor,
            'args': args,
            'state': [],
            'listitems': [],
            'dictitems': [],
        }
        deserialized_obj = reduce_value[0](*deserialized_args)
        memory_obj = self.save_to_memory(id_, json_obj, deserialized_obj)

        state, deserialized_state = self.unzip_dict([
            (attr, self.serialize(value))
            for attr, value in reduce_value[2].items()
        ], skip_first=True)
        listitems, deserialized_listitems = self.unzip_list([
            self.serialize(item)
            for item in reduce_value[3]
        ])
        dictitems, deserialized_dictitems = _PythonTreeSerializer.unzip_dict([
            (self.serialize(key), self.serialize(value))
            for key, value in reduce_value[4]
        ])

        memory_obj.json['state'] = state
        memory_obj.json['listitems'] = listitems
        memory_obj.json['dictitems'] = dictitems

        for key, value in deserialized_state.items():
            setattr(deserialized_obj, key, value)
        for item in deserialized_listitems:
            deserialized_obj.append(item)
        for key, value in deserialized_dictitems.items():
            deserialized_obj[key] = value

        memory_obj.deserialized_obj = deserialized_obj
        memory_obj.is_draft = False

        return id_, deserialized_obj

    def serialize(self, py_object):
        type_ = _PythonTreeSerializer.get_type(py_object)
        id_ = id(py_object)
        skip_comparable = False

        if id_ in self.memory:
            value = id_
            strategy = 'memory'
            skip_comparable = True
            comparable = False
            deserialized_obj = self.memory[id_].deserialized_obj
            if not self.memory[id_].is_draft:
                self.memory[id_].comparable = py_object == deserialized_obj
        elif isinstance(py_object, type):
            value = _PythonTreeSerializer.get_type_name(py_object)
            strategy = 'repr'
            deserialized_obj = py_object
        elif any(type(py_object) == t for t in (list, set, tuple)):
            elements = [
                self.serialize(element) for element in py_object
            ]
            value, deserialized_obj = _PythonTreeSerializer.unzip_list(elements, type(py_object))
            strategy = 'generic'
        elif type(py_object) == dict:
            elements = [
                [self.serialize(key), self.serialize(value)]
                for key, value in py_object.items()
            ]
            value, deserialized_obj = _PythonTreeSerializer.unzip_dict(elements)
            strategy = 'generic'
        elif _PythonTreeSerializer.has_reduce(py_object):
            value, deserialized_obj = self.get_reduce(py_object)
            strategy = 'memory'
        else:
            value = repr(py_object)
            try:
                deserialized_obj = copy.deepcopy(py_object)
            except Exception:
                deserialized_obj = py_object
                skip_comparable = True
                comparable = False
            strategy = 'repr'

        if not skip_comparable:
            try:
                comparable = py_object == deserialized_obj
            except Exception:
                comparable = False

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

    def dumps(self, obj):
        return {
            'json': self.serialize(obj)[0],
            'memory': {
                key: value.json
                for key, value in self.memory.items()
            }
        }
