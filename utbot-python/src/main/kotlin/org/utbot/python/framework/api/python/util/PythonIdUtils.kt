package org.utbot.python.framework.api.python.util

import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation

// none annotation can be used in code only since Python 3.10
val pythonNoneClassId = PythonClassId("types.NoneType")
val pythonObjectClassId = PythonClassId("builtins.object")
val pythonAnyClassId = NormalizedPythonAnnotation("typing.Any")
val pythonIntClassId = PythonClassId("builtins.int")
val pythonFloatClassId = PythonClassId("builtins.float")
val pythonComplexClassId = PythonClassId("builtins.complex")
val pythonStrClassId = PythonClassId("builtins.str")
val pythonBoolClassId = PythonClassId("builtins.bool")
val pythonRangeClassId = PythonClassId("builtins.range")
val pythonListClassId = PythonClassId("builtins.list")
val pythonTupleClassId = PythonClassId("builtins.tuple")
val pythonDictClassId = PythonClassId("builtins.dict")
val pythonSetClassId = PythonClassId("builtins.set")
val pythonBytearrayClassId = PythonClassId("builtins.bytearray")
val pythonBytesClassId = PythonClassId("builtins.bytes")
val pythonExceptionClassId = PythonClassId("builtins.Exception")
