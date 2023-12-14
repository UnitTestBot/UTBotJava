import random

from utbot_executor.config import CoverageConfig, HostConfig
from utbot_executor.executor import PythonExecutor
from utbot_executor.parser import (
    ExecutionRequest,
    parse_request,
    ExecutionSuccessResponse,
)
from utbot_executor.utils import TraceMode

random.seed(239)


def _generate_host_config() -> HostConfig:
    return HostConfig("localhost", random.randint(10**5, 10**6))


def _generate_coverage_config() -> CoverageConfig:
    return CoverageConfig(_generate_host_config(), TraceMode.Instructions, True)


def _read_request() -> ExecutionRequest:
    with open("example_input.json", "r") as fin:
        text = "\n".join(fin.readlines())
    return parse_request(text)


def test_python_executor():
    executor = PythonExecutor(_generate_coverage_config(), False)
    request = _read_request()
    response = executor.run_function(request)
    assert isinstance(response, ExecutionSuccessResponse)
