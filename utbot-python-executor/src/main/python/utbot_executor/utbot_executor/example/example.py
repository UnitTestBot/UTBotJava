from utbot_executor.executor import PythonExecutor
from utbot_executor.parser import ExecutionRequest, ExecutionSuccessResponse


def test_execution():
    executor = PythonExecutor("", 0)
    id_ = '1500926645'
    serialized_arg = r'{"objects":{"1500926644":{"strategy":"repr","id":"1500926644","typeinfo":{"module":"builtins","kind":"int"},"comparable":true,"value":"170141183460469231731687303715749887999"},"1500926652":{"strategy":"list","id":"1500926652","typeinfo":{"module":"builtins","kind":"list"},"comparable":true,"items":["1500926644"]},"1500926650":{"strategy":"repr","id":"1500926650","typeinfo":{"module":"builtins","kind":"str"},"comparable":true,"value":"\"x\""},"1500926646":{"strategy":"repr","id":"1500926646","typeinfo":{"module":"builtins","kind":"int"},"comparable":true,"value":"1"},"1500926651":{"strategy":"dict","id":"1500926651","typeinfo":{"module":"builtins","kind":"dict"},"comparable":true,"items":{"1500926650":"1500926646"}},"1500926653":{"strategy":"list","id":"1500926653","typeinfo":{"module":"builtins","kind":"list"},"comparable":true,"items":[]},"1500926654":{"strategy":"dict","id":"1500926654","typeinfo":{"module":"builtins","kind":"dict"},"comparable":true,"items":{}},"1500926645":{"strategy":"reduce","id":"1500926645","typeinfo":{"module":"my_func","kind":"A"},"comparable":true,"constructor":{"module":"my_func","kind":"A"},"args":"1500926652","state":"1500926651","listitems":"1500926653","dictitems":"1500926654"}}}'
    request = ExecutionRequest(
        'f',
        'my_func',
        ['my_func'],
        ['/home/vyacheslav/Projects/utbot_executor/utbot_executor/tests'],
        [id_],
        {},
        serialized_arg,
        '/home/vyacheslav/Projects/utbot_executor/utbot_executor/tests/my_func.py',
        '0x1'
    )
    response = executor.run_function(request)

    assert isinstance(response, ExecutionSuccessResponse)

    assert response.status == "success"
    assert response.is_exception is False
    assert response.diff_ids
