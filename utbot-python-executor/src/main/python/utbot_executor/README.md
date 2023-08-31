# UtBot Executor

Util for python code execution and state serialization.

## Installation

You can install module from [PyPI](https://pypi.org/project/utbot-executor/):

```bash
python -m pip install utbot-executor
```

## Usage

### From console with socket listener

Run with your `<hostname>` and `<port>` for socket connection
```bash
$ python -m utbot_executor <hostname> <port> <logfile> [<loglevel DEBUG | INFO | ERROR>] <coverage_hostname> <coverage_port>
```

### Request format
```json
{
  "functionName": "f",
  "functionModule": "my_module.submod1",
  "imports": ["sys", "math", "json"],
  "syspaths": ["/home/user/my_project/"],
  "argumentsIds": ["1", "2"],
  "kwargumentsIds": ["4", "5"],
  "serializedMemory": "string",
  "filepath": ["/home/user/my_project/my_module/submod1.py"],
  "coverageId": "1"
}
```

* `functionName` - name of the tested function
* `functionModule` - name of the module of the tested function
* `imports` - all modules which need to run function with current arguments
* `syspaths` - all syspaths which need to import modules (usually it is a project root)
* `argumentsIds` - list of argument's ids
* `kwargumentsIds` - list of keyword argument's ids
* `serializedMemory` - serialized memory throw `deep_serialization` algorithm
* `filepath` - path to the tested function's containing file
* `coverageId` - special id witch will be used for sending information about covered lines

### Response format:

If execution is successful:
```json
{
        "status": "success",
        "isException": false,
        "statements": [1, 2, 3],
        "missedStatements": [4, 5],
        "stateInit": "string",
        "stateBefore": "string",
        "stateAfter": "string",
        "diffIds": ["3", "4"],
        "argsIds": ["1", "2", "3"],
        "kwargs": ["4", "5", "6"],
        "resultId": "7"
}
```

* `status` - always "success"
* `isException` - boolean value, if it is `true`, execution ended with an exception
* `statements` - list of the numbers of covered rows
* `missedStatements` - list of numbers of uncovered rows
* `stateInit` - serialized states from request
* `stateBefore` - serialized states of arguments before execution
* `stateAfter` - serialized states of arguments after execution
* `diffIds` - ids of the objects which have been changed
* `argsIds` - ids of the function's arguments
* `kwargsIds` - ids of the function's keyword arguments
* `resultId` - id of the returned value

or error format if there was exception in running algorith:

```json
{
        "status": "fail",
        "exception": "stacktrace"
}
```
* `status` - always "fail"
* `exception` - string representation of the exception stack trace

### Submodule `deep_serialization`

JSON serializer and deserializer for python objects

#### States memory json-format

```json
{
  "objects": {
    "id": {
      "id": "1",
      "strategy": "strategy name",
      "typeinfo": {
        "module": "builtins",
        "kind": "int"
      },
      "comparable": true,
      
      // iff strategy is 'repr'
      "value": "1",

      // iff strategy is 'list' or 'dict'
      "items": ["3", "2"],

      // iff strategy = 'reduce'
      "constructor": "mymod.A.__new__",
      "args": ["mymod.A"],
      "state": {"a": "4", "b": "5"},
      "listitems": ["7", "8"],
      "dictitems": {"ka": "10"}
    }
  }
}
```


## Source

GitHub [repository](https://github.com/tamarinvs19/utbot_executor)
