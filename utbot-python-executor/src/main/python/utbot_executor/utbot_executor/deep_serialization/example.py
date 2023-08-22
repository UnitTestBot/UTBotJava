import datetime
import json
from pprint import pprint

from utbot_executor.deep_serialization.bad_class import BadField
from utbot_executor.deep_serialization.memory_objects import PythonSerializer
from utbot_executor.deep_serialization.json_converter import MemoryDumpEncoder
from utbot_executor.deep_serialization.deep_serialization import deserialize_objects


class B:
    def __init__(self, b1, b2, b3):
        self.b1 = b1
        self.b2 = b2
        self.b3 = b3
        self.time = datetime.datetime.now()


class Node:
    def __init__(self, name: str):
        self.name = name
        self.children = []

    def __eq__(self, other):
        return self.name == other.name


def serialize_bad_obj():
    a = BadField("1")
    s = PythonSerializer()
    s.write_object_to_memory(a)
    pprint(s.memory.objects)
    with open('test_bad_field.json', 'w') as fout:
        print(json.dumps({'objects': s.memory}, cls=MemoryDumpEncoder, indent=True), file=fout)


def deserialize_bad_obj():
    # run()
    with open('test_bad_field.json', 'r') as fin:
        data = fin.read()
    pprint(data)
    pprint(deserialize_objects(["140543796187856"], data, [
        'copyreg',
        'utbot_executor.deep_serialization.example',
    ]))


def run():
    from pprint import pprint

    # c = ["Alex"]
    # b = B(1, 2, 3)
    # b.b1 = B(4, 5, b)
    # a = [1, 2, float('inf'), "abc", {1: 1}, None, b, c]
    x = Node("x")
    y = Node("y")
    x.children.append(y)
    y.children.append(x)
    serializer_ = PythonSerializer()
    pprint(serializer_.write_object_to_memory(x))
    pprint(serializer_.memory.objects)
    with open('test_json.json', 'w') as fout:
        print(json.dumps({'objects': serializer_.memory}, cls=MemoryDumpEncoder, indent=True), file=fout)


def deserialize():
    # run()
    with open('test_json.json', 'r') as fin:
        data = fin.read()
    print(data)
    pprint(deserialize_objects(["140340324106560"], data, [
        'copyreg',
        'utbot_executor.deep_serialization.example',
        'datetime'
    ]))


if __name__ == '__main__':
    # serialize_bad_obj()
    # deserialize_bad_obj()
    with open('test_bad.json', 'r') as fin:
        data = fin.read()
    pprint(data)
    pprint(deserialize_objects(["1500000374"], data, [
        'copyreg',
        'utbot_executor.deep_serialization.example',
    ]))
