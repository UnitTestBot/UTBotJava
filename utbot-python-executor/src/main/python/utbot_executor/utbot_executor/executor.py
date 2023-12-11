"""Python code executor for UnitTestBot"""
import copy
import importlib
import inspect
import logging
import pathlib
import sys
import traceback
import types
from typing import Any, Callable, Dict, Iterable, List, Tuple

from utbot_executor.deep_serialization.deep_serialization import serialize_memory_dump, \
    serialize_objects_dump
from utbot_executor.deep_serialization.json_converter import DumpLoader, deserialize_memory_objects
from utbot_executor.deep_serialization.memory_objects import MemoryDump, PythonSerializer
from utbot_executor.deep_serialization.utils import PythonId, getattr_by_path
from utbot_executor.memory_compressor import compress_memory
from utbot_executor.parser import ExecutionRequest, ExecutionResponse, ExecutionFailResponse, ExecutionSuccessResponse
from utbot_executor.ut_tracer import UtTracer, UtCoverageSender
from utbot_executor.utils import (
    suppress_stdout as __suppress_stdout,
    get_instructions,
    filter_instructions,
    TraceMode, UtInstruction,
)

__all__ = ['PythonExecutor']


def _update_states(init_memory_dump: MemoryDump, before_memory_dump: MemoryDump) -> MemoryDump:
    for id_, obj in before_memory_dump.objects.items():
        if id_ in init_memory_dump.objects:
            init_memory_dump.objects[id_].comparable = obj.comparable
        else:
            init_memory_dump.objects[id_] = obj
    return init_memory_dump


def _load_objects(objs: List[Any]) -> MemoryDump:
    serializer = PythonSerializer()
    serializer.clear_visited()
    for obj in objs:
        serializer.write_object_to_memory(obj)
    return serializer.memory


class PythonExecutor:
    def __init__(self, coverage_hostname: str, coverage_port: int, trace_mode: TraceMode, send_coverage: bool):
        self.coverage_hostname = coverage_hostname
        self.coverage_port = coverage_port
        self.trace_mode = trace_mode
        self.send_coverage = send_coverage

    @staticmethod
    def add_syspaths(syspaths: Iterable[str]):
        for path in syspaths:
            if path not in sys.path:
                sys.path.insert(0, path)

    @staticmethod
    def add_imports(imports: Iterable[str]):
        for module in imports:
            for i in range(1, module.count('.') + 2):
                submodule_name = '.'.join(module.split('.', maxsplit=i)[:i])
                logging.debug("Submodule #%d: %s", i, submodule_name)
                if submodule_name not in globals():
                    try:
                        globals()[submodule_name] = importlib.import_module(submodule_name)
                    except ModuleNotFoundError:
                        logging.warning("Import submodule %s failed", submodule_name)
                logging.debug("Submodule #%d: OK", i)

    def run_function(self, request: ExecutionRequest) -> ExecutionResponse:
        logging.debug("Prepare to run function `%s`", request.function_name)
        try:
            memory_dump = deserialize_memory_objects(request.serialized_memory)
            loader = DumpLoader(memory_dump)
        except Exception as _:
            logging.debug("Error \n%s", traceback.format_exc())
            return ExecutionFailResponse("fail", traceback.format_exc())
        logging.debug("Dump loader have been created")

        try:
            logging.debug("Imports: %s", request.imports)
            logging.debug("Syspaths: %s", request.syspaths)
            self.add_syspaths(request.syspaths)
            self.add_imports(request.imports)
            loader.add_syspaths(request.syspaths)
            loader.add_imports(request.imports)
        except Exception as _:
            logging.debug("Error \n%s", traceback.format_exc())
            return ExecutionFailResponse("fail", traceback.format_exc())
        logging.debug("Imports have been added")

        try:
            function = getattr_by_path(
                    importlib.import_module(request.function_module),
                    request.function_name
                    )
            if not isinstance(function, types.FunctionType):
                return ExecutionFailResponse(
                        "fail",
                        f"Invalid function path {request.function_module}.{request.function_name}"
                        )
            logging.debug("Function initialized")
            args = [loader.load_object(PythonId(arg_id)) for arg_id in request.arguments_ids]
            logging.debug("Arguments: %s", args)
            kwargs = {name: loader.load_object(PythonId(kwarg_id)) for name, kwarg_id in request.kwarguments_ids.items()}
            logging.debug("Kwarguments: %s", kwargs)
        except Exception as _:
            logging.debug("Error \n%s", traceback.format_exc())
            return ExecutionFailResponse("fail", traceback.format_exc())
        logging.debug("Arguments have been created")

        try:
            state_init_memory = _load_objects(args + list(kwargs.values()))
            state_init = _update_states(loader.reload_id(), state_init_memory)
            serialized_state_init = serialize_memory_dump(state_init)

            _coverage_sender = UtCoverageSender(
                request.coverage_id,
                self.coverage_hostname,
                self.coverage_port,
                send_coverage=self.send_coverage,
            )

            value = _run_calculate_function_value(
                function,
                args,
                kwargs,
                request.filepath,
                serialized_state_init,
                tracer=UtTracer(
                    pathlib.Path(request.filepath),
                    [sys.prefix, sys.exec_prefix],
                    _coverage_sender,
                    self.trace_mode,
                ),
            )
        except Exception as _:
            logging.debug("Error \n%s", traceback.format_exc())
            return ExecutionFailResponse("fail", traceback.format_exc())
        logging.debug("Value have been calculated: %s", value)
        return value


def _serialize_state(
        args: List[Any],
        kwargs: Dict[str, Any],
        result: Any = None,
        ) -> Tuple[List[PythonId], Dict[str, PythonId], PythonId, MemoryDump, str]:
    """Serialize objects from args, kwargs and result.

    Returns: tuple of args ids, kwargs ids, result id and serialized memory."""

    all_arguments = args + list(kwargs.values()) + [result]

    ids, memory, serialized_memory = serialize_objects_dump(all_arguments, True)
    return (
            ids[:len(args)],
            dict(zip(kwargs.keys(), ids[len(args):len(args)+len(kwargs)])),
            ids[-1],
            copy.deepcopy(memory),
            serialized_memory,
            )


def _run_calculate_function_value(
        function: types.FunctionType,
        args: List[Any],
        kwargs: Dict[str, Any],
        fullpath: str,
        state_init: str,
        tracer: UtTracer,
    ) -> ExecutionResponse:
    """ Calculate function evaluation result.

    Return serialized data: status, coverage info, object ids and memory."""

    _, _, _, state_before, serialized_state_before = _serialize_state(args, kwargs)

    __is_exception = False

    _, __start = inspect.getsourcelines(function)
    __all_code_stmts = filter_instructions(get_instructions(function), tracer.mode)

    __tracer = tracer

    try:
        with __suppress_stdout():
            __result = __tracer.runfunc(function, *args, **kwargs)
    except Exception as __exception:
        __result = __exception
        __is_exception = True
    logging.debug("Function call finished: %s", __result)

    logging.debug("Coverage: %s", __tracer.counts)
    logging.debug("Fullpath: %s", fullpath)
    __stmts_with_def = [UtInstruction(__start, 0, True)] + list(__tracer.counts.keys())
    __missed_filtered = [x for x in __all_code_stmts if x not in __stmts_with_def]
    logging.debug("Covered lines: %s", __stmts_with_def)
    logging.debug("Missed lines: %s", __missed_filtered)

    __str_statements = [x.serialize() for x in __stmts_with_def]
    __str_missed_statements = [x.serialize() for x in __missed_filtered]

    args_ids, kwargs_ids, result_id, state_after, serialized_state_after = _serialize_state(args, kwargs, __result)
    ids = args_ids + list(kwargs_ids.values())
    diff_ids = compress_memory(ids, state_before, state_after)

    return ExecutionSuccessResponse(
            status="success",
            is_exception=__is_exception,
            statements=__str_statements,
            missed_statements=__str_missed_statements,
            state_init=state_init,
            state_before=serialized_state_before,
            state_after=serialized_state_after,
            diff_ids=diff_ids,
            args_ids=args_ids,
            kwargs_ids=kwargs_ids,
            result_id=result_id,
            )
