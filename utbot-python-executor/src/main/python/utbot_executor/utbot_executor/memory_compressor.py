import typing

from utbot_executor.deep_serialization.memory_objects import MemoryDump
from utbot_executor.deep_serialization.utils import PythonId

try:
    import numpy as np
except ImportError:
    import sys
    print("numpy is not installed", file=sys.stderr)

def compress_memory(
        ids: typing.List[PythonId],
        state_before: MemoryDump,
        state_after: MemoryDump
) -> typing.List[PythonId]:
    diff_ids: typing.List[PythonId] = []
    for id_ in ids:
        if id_ in state_before.objects and id_ in state_after.objects:
            try:
                if isinstance(state_before.objects[id_].obj, np.ndarray) or isinstance(state_after.objects[id_].obj, np.ndarray):
                    if (state_before.objects[id_].obj != state_after.objects[id_].obj).all():
                        diff_ids.append(id_)
                elif state_before.objects[id_].obj != state_after.objects[id_].obj:
                    diff_ids.append(id_)
            except AttributeError as _:
                pass
    return diff_ids
