import dataclasses
import enum
import json
from typing import Dict, List, Union, Tuple, Literal


class MemoryMode(enum.StrEnum):
    PICKLE = "PICKLE"
    REDUCE = "REDUCE"


@dataclasses.dataclass
class ExecutionRequest:
    function_name: str
    function_module: str
    imports: List[str]
    syspaths: List[str]
    arguments_ids: List[str]
    kwarguments_ids: Dict[str, str]
    serialized_memory: str
    memory_mode: MemoryMode
    filepath: str
    coverage_id: str

    def get_class_name(self) -> str | None:
        if "." in self.function_name:
            return self.function_name.split(".")[0]
        return None


class ExecutionResponse:
    status: str


@dataclasses.dataclass
class ExecutionSuccessResponse(ExecutionResponse):
    status: str
    is_exception: bool
    statements: List[str]
    missed_statements: List[str]
    state_init: str
    state_before: str
    state_after: str
    diff_ids: List[str]
    args_ids: List[str]
    kwargs_ids: Dict[str, str]
    result_id: str


@dataclasses.dataclass
class ExecutionFailResponse(ExecutionResponse):
    status: str
    exception: str


def as_execution_request(dct: Dict) -> Union[ExecutionRequest, Dict]:
    if set(dct.keys()) == {
            'functionName',
            'functionModule',
            'imports',
            'syspaths',
            'argumentsIds',
            'kwargumentsIds',
            'serializedMemory',
            'memoryMode',
            'filepath',
            'coverageId',
            }:
        return ExecutionRequest(
                dct['functionName'],
                dct['functionModule'],
                dct['imports'],
                dct['syspaths'],
                dct['argumentsIds'],
                dct['kwargumentsIds'],
                dct['serializedMemory'],
                MemoryMode(dct['memoryMode']),
                dct['filepath'],
                dct['coverageId'],
                )
    return dct


def parse_request(request: str) -> ExecutionRequest:
    return json.loads(request, object_hook=as_execution_request)


class ResponseEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ExecutionSuccessResponse):
            return {
                "status": o.status,
                "isException": o.is_exception,
                "statements": o.statements,
                "missedStatements": o.missed_statements,
                "stateInit": o.state_init,
                "stateBefore": o.state_before,
                "stateAfter": o.state_after,
                "diffIds": o.diff_ids,
                "argsIds": o.args_ids,
                "kwargsIds": o.kwargs_ids,
                "resultId": o.result_id,
            }
        if isinstance(o, ExecutionFailResponse):
            return {
                "status": o.status,
                "exception": o.exception
            }
        return json.JSONEncoder.default(self, o)


def serialize_response(response: ExecutionResponse) -> str:
    return json.dumps(response, cls=ResponseEncoder)
