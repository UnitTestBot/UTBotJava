import random

from utbot_executor.config import CoverageConfig, HostConfig
from utbot_executor.executor import PythonExecutor
from utbot_executor.parser import ExecutionRequest, ExecutionSuccessResponse, MemoryMode
from utbot_executor.utils import TraceMode


def test_execution():
    executor = PythonExecutor(
        CoverageConfig(HostConfig("localhost", random.randint(10 ** 5, 10 ** 6)), TraceMode.Instructions, True), False)
    id_ = '1500926645'
    serialized_arg = (r'{"objects":{"1500926644":{"strategy":"repr","id":"1500926644","typeinfo":{"module":"builtins",'
                      r'"kind":"int"},"comparable":true,"value":"170141183460469231731687303715749887999"},'
                      r'"1500926652":{"strategy":"list","id":"1500926652","typeinfo":{"module":"builtins",'
                      r'"kind":"list"},"comparable":true,"items":["1500926644"]},"1500926650":{"strategy":"repr",'
                      r'"id":"1500926650","typeinfo":{"module":"builtins","kind":"str"},"comparable":true,'
                      r'"value":"\"x\""},"1500926646":{"strategy":"repr","id":"1500926646","typeinfo":{'
                      r'"module":"builtins","kind":"int"},"comparable":true,"value":"1"},"1500926651":{'
                      r'"strategy":"dict","id":"1500926651","typeinfo":{"module":"builtins","kind":"dict"},'
                      r'"comparable":true,"items":{"1500926650":"1500926646"}},"1500926653":{"strategy":"list",'
                      r'"id":"1500926653","typeinfo":{"module":"builtins","kind":"list"},"comparable":true,'
                      r'"items":[]},"1500926654":{"strategy":"dict","id":"1500926654","typeinfo":{'
                      r'"module":"builtins","kind":"dict"},"comparable":true,"items":{}},"1500926645":{'
                      r'"strategy":"reduce","id":"1500926645","typeinfo":{"module":"my_func","kind":"A"},'
                      r'"comparable":true,"constructor":{"module":"my_func","kind":"A"},"args":"1500926652",'
                      r'"state":"1500926651","listitems":"1500926653","dictitems":"1500926654"}}}')
    request = ExecutionRequest(
        'f',
        'my_func',
        ['my_func'],
        ['./'],
        [id_],
        {},
        serialized_arg,
        MemoryMode.REDUCE,
        'my_func.py',
        '0x1',
    )
    response = executor.run_reduce_function(request)

    assert isinstance(response, ExecutionSuccessResponse)

    assert response.status == "success"
    assert response.is_exception is False
