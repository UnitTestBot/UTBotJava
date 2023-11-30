from utbot_executor.deep_serialization.deep_serialization import serialize_objects_dump
from utbot_executor.deep_serialization.memory_objects import ReprMemoryObject


def test_serialize_state():
    args = ["\n   123   \n"]
    kwargs = {}
    result = None

    _, state, serialized_state = serialize_objects_dump(args + list(kwargs) + [result])

    serialized_arg = list(state.objects.values())[0]
    assert isinstance(serialized_arg, ReprMemoryObject)
    assert serialized_arg.value == "'\\n   123   \\n'"


def test_serialize_state_1():
    args = ["0\n   123   \n"]
    kwargs = {}
    result = None

    _, state, serialized_state = serialize_objects_dump(args + list(kwargs) + [result])

    serialized_arg = list(state.objects.values())[0]
    assert isinstance(serialized_arg, ReprMemoryObject)
    assert serialized_arg.value == "'0\\n   123   \\n'"


def test_serialize_state_2():
    args = ["\\\n    Adds new strings"]
    kwargs = {}
    result = None

    _, state, serialized_state = serialize_objects_dump(args + list(kwargs) + [result])

    serialized_arg = list(state.objects.values())[0]
    assert isinstance(serialized_arg, ReprMemoryObject)
    assert serialized_arg.value == "'\\\\\\n    Adds new strings'"
