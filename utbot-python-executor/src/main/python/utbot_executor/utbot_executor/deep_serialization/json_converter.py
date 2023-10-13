import copy
import importlib
import json
import sys
from typing import Dict, Iterable, Union
from utbot_executor.deep_serialization.memory_objects import (
    MemoryObject,
    ReprMemoryObject,
    ListMemoryObject,
    DictMemoryObject,
    ReduceMemoryObject,
    MemoryDump,
)
from utbot_executor.deep_serialization.utils import PythonId, TypeInfo


class MemoryObjectEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, MemoryObject):
            base_json = {
                "strategy": o.strategy,
                "id": o.id_value(),
                "typeinfo": o.typeinfo,
                "comparable": o.comparable,
            }
            if isinstance(o, ReprMemoryObject):
                base_json["value"] = o.value
            elif isinstance(o, (ListMemoryObject, DictMemoryObject)):
                base_json["items"] = o.items
            elif isinstance(o, ReduceMemoryObject):
                base_json["constructor"] = o.constructor
                base_json["args"] = o.args
                base_json["state"] = o.state
                base_json["listitems"] = o.listitems
                base_json["dictitems"] = o.dictitems
            return base_json
        return json.JSONEncoder.default(self, o)


class MemoryDumpEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, MemoryDump):
            return {
                id_: MemoryObjectEncoder().default(o) for id_, o in o.objects.items()
            }
        if isinstance(o, TypeInfo):
            return {
                "kind": o.kind,
                "module": o.module,
            }
        return json.JSONEncoder.default(self, o)


def as_repr_object(dct: Dict) -> Union[MemoryObject, Dict]:
    if "strategy" in dct:
        obj: MemoryObject
        if dct["strategy"] == "repr":
            obj = ReprMemoryObject.__new__(ReprMemoryObject)
            obj.value = dct["value"]
            obj.typeinfo = TypeInfo(
                kind=dct["typeinfo"]["kind"], module=dct["typeinfo"]["module"]
            )
            obj.comparable = dct["comparable"]
            return obj
        if dct["strategy"] == "list":
            obj = ListMemoryObject.__new__(ListMemoryObject)
            obj.items = dct["items"]
            obj.typeinfo = TypeInfo(
                kind=dct["typeinfo"]["kind"], module=dct["typeinfo"]["module"]
            )
            obj.comparable = dct["comparable"]
            return obj
        if dct["strategy"] == "dict":
            obj = DictMemoryObject.__new__(DictMemoryObject)
            obj.items = dct["items"]
            obj.typeinfo = TypeInfo(
                kind=dct["typeinfo"]["kind"], module=dct["typeinfo"]["module"]
            )
            obj.comparable = dct["comparable"]
            return obj
        if dct["strategy"] == "reduce":
            obj = ReduceMemoryObject.__new__(ReduceMemoryObject)
            obj.constructor = TypeInfo(
                kind=dct["constructor"]["kind"],
                module=dct["constructor"]["module"],
            )
            obj.args = dct["args"]
            obj.state = dct["state"]
            obj.listitems = dct["listitems"]
            obj.dictitems = dct["dictitems"]
            obj.typeinfo = TypeInfo(
                kind=dct["typeinfo"]["kind"], module=dct["typeinfo"]["module"]
            )
            obj.comparable = dct["comparable"]
            return obj
    return dct


def deserialize_memory_objects(memory_dump: str) -> MemoryDump:
    parsed_data = json.loads(memory_dump, object_hook=as_repr_object)
    return MemoryDump(parsed_data["objects"])


class DumpLoader:
    def __init__(self, memory_dump: MemoryDump):
        self.memory_dump = memory_dump
        self.memory: Dict[PythonId, object] = {}  # key is new id, value is real object
        self.dump_id_to_real_id: Dict[PythonId, PythonId] = {}

    def reload_id(self) -> MemoryDump:
        new_memory_objects: Dict[PythonId, MemoryObject] = {}
        for id_, obj in self.memory_dump.objects.items():
            new_memory_object = copy.deepcopy(obj)
            read_id = self.dump_id_to_real_id[id_]
            new_memory_object.obj = self.memory[read_id]
            if isinstance(new_memory_object, ReprMemoryObject):
                pass
            elif isinstance(new_memory_object, ListMemoryObject):
                new_memory_object.items = [
                    self.dump_id_to_real_id[id_] for id_ in new_memory_object.items
                ]
            elif isinstance(new_memory_object, DictMemoryObject):
                new_memory_object.items = {
                    self.dump_id_to_real_id[id_key]: self.dump_id_to_real_id[id_value]
                    for id_key, id_value in new_memory_object.items.items()
                }
            elif isinstance(new_memory_object, ReduceMemoryObject):
                new_memory_object.args = self.dump_id_to_real_id[new_memory_object.args]
                new_memory_object.state = self.dump_id_to_real_id[
                    new_memory_object.state
                ]
                new_memory_object.listitems = self.dump_id_to_real_id[
                    new_memory_object.listitems
                ]
                new_memory_object.dictitems = self.dump_id_to_real_id[
                    new_memory_object.dictitems
                ]
            new_memory_objects[self.dump_id_to_real_id[id_]] = new_memory_object
        return MemoryDump(new_memory_objects)

    @staticmethod
    def add_syspaths(syspaths: Iterable[str]):
        for path in syspaths:
            if path not in sys.path:
                sys.path.insert(0, path)

    @staticmethod
    def add_imports(imports: Iterable[str]):
        for module in imports:
            for i in range(1, module.count(".") + 2):
                submodule_name = ".".join(module.split(".", maxsplit=i)[:i])
                globals()[submodule_name] = importlib.import_module(submodule_name)

    def load_object(self, python_id: PythonId) -> object:
        if python_id in self.dump_id_to_real_id:
            return self.memory[self.dump_id_to_real_id[python_id]]

        dump_object = self.memory_dump.objects[python_id]
        real_object: object
        if isinstance(dump_object, ReprMemoryObject):
            real_object = eval(dump_object.value)
        elif isinstance(dump_object, ListMemoryObject):
            if dump_object.typeinfo.fullname == "builtins.set":
                real_object = set(self.load_object(item) for item in dump_object.items)
            elif dump_object.typeinfo.fullname == "builtins.tuple":
                real_object = tuple(
                    self.load_object(item) for item in dump_object.items
                )
            else:
                real_object = []

                id_ = PythonId(str(id(real_object)))
                self.dump_id_to_real_id[python_id] = id_
                self.memory[id_] = real_object

                for item in dump_object.items:
                    real_object.append(self.load_object(item))
        elif isinstance(dump_object, DictMemoryObject):
            real_object = {}

            id_ = PythonId(str(id(real_object)))
            self.dump_id_to_real_id[python_id] = id_
            self.memory[id_] = real_object

            for key, value in dump_object.items.items():
                real_object[self.load_object(key)] = self.load_object(value)
        elif isinstance(dump_object, ReduceMemoryObject):
            constructor = eval(dump_object.constructor.qualname)
            args = self.load_object(dump_object.args)
            if args is None:  # It is a global var
                real_object = constructor
            else:
                real_object = constructor(*args)

            id_ = PythonId(str(id(real_object)))
            self.dump_id_to_real_id[python_id] = id_
            self.memory[id_] = real_object

            if args is not None:
                state = self.load_object(dump_object.state)
                if isinstance(state, dict):
                    for field, value in state.items():
                        try:
                            setattr(real_object, field, value)
                        except AttributeError:
                            pass
                elif hasattr(real_object, "__setstate__"):
                    real_object.__setstate__(state)
                if isinstance(state, tuple) and len(state) == 2:
                    _, slotstate = state
                    if slotstate:
                        for key, value in slotstate.items():
                            try:
                                setattr(real_object, key, value)
                            except AttributeError:
                                pass

                listitems = self.load_object(dump_object.listitems)
                if isinstance(listitems, Iterable):
                    for listitem in listitems:
                        real_object.append(listitem)

                dictitems = self.load_object(dump_object.dictitems)
                if isinstance(dictitems, Dict):
                    for key, dictitem in dictitems.items():
                        real_object[key] = dictitem
        else:
            raise TypeError(f"Invalid type {dump_object}")

        id_ = PythonId(str(id(real_object)))
        self.dump_id_to_real_id[python_id] = id_
        self.memory[id_] = real_object

        return real_object


def main():
    with open("test_json.json", "r") as fin:
        data = fin.read()
    memory_dump = deserialize_memory_objects(data)
    loader = DumpLoader(memory_dump)
    loader.add_imports(
        [
            "copyreg._reconstructor",
            "deep_serialization.example.B",
            "datetime.datetime",
            "builtins.int",
            "builtins.float",
            "builtins.bool",
            "types.NoneType",
            "builtins.list",
            "builtins.dict",
            "builtins.str",
            "builtins.tuple",
            "builtins.bytes",
            "builtins.type",
        ]
    )
    print(loader.load_object("140239390887040"))
