import json
from typing import Any, Dict, Tuple, List

from utbot_executor.deep_serialization.memory_objects import PythonSerializer, MemoryDump
from utbot_executor.deep_serialization.json_converter import MemoryDumpEncoder, deserialize_memory_objects, DumpLoader
from utbot_executor.deep_serialization.utils import PythonId


def serialize_memory_dump(dump: MemoryDump):
    return json.dumps({'objects': dump}, cls=MemoryDumpEncoder)


def serialize_object(obj: Any) -> Tuple[str, str]:
    """
    Serialize one object.
    Returns the object id and memory dump.
    """

    serializer = PythonSerializer()
    id_ = serializer.write_object_to_memory(obj)
    return id_, serialize_memory_dump(serializer.memory)


def serialize_objects(objs: List[Any], clear_visited: bool = False) -> Tuple[List[PythonId], str]:
    """
    Serialize objects with shared memory.
    Returns list of object ids and memory dump.
    """

    serializer = PythonSerializer()
    if clear_visited:
        serializer.clear_visited()
    ids = [
        serializer.write_object_to_memory(obj)
        for obj in objs
    ]
    return ids, serialize_memory_dump(serializer.memory)


def serialize_objects_dump(objs: List[Any], clear_visited: bool = False) -> Tuple[List[PythonId], MemoryDump, str]:
    """
    Serialize objects with shared memory.
    Returns list of object ids and memory dump.
    """

    serializer = PythonSerializer()
    if clear_visited:
        serializer.clear_visited()
    ids = [
        serializer.write_object_to_memory(obj)
        for obj in objs
    ]
    return ids, serializer.memory, serialize_memory_dump(serializer.memory)


def deserialize_objects(ids: List[str], memory: str, imports: List[str]) -> Dict[str, object]:
    """
    Deserialize objects from shared memory.
    Returns dictionary where keys are ID and values are deserialized objects.
    """

    memory_dump = deserialize_memory_objects(memory)
    loader = DumpLoader(memory_dump)
    loader.add_imports(imports)
    return {python_id: loader.load_object(PythonId(python_id)) for python_id in ids}
