package org.utbot.framework.plugin.api.python.util

import org.utbot.framework.plugin.api.python.*

// none annotation can be used in code only since Python 3.10
val pythonNoneClassId = PythonClassId("types.NoneType")
val pythonAnyClassId = NormalizedPythonAnnotation("typing.Any")
val pythonIntClassId = PythonClassId("builtins.int")
val pythonFloatClassId = PythonClassId("builtins.float")
val pythonStrClassId = PythonClassId("builtins.str")
val pythonBoolClassId = PythonBoolModel.classId
val pythonRangeClassId = PythonClassId("builtins.range")
val pythonListClassId = PythonListModel.classId
val pythonTupleClassId = PythonTupleModel.classId
val pythonDictClassId = PythonDictModel.classId
val pythonSetClassId = PythonSetModel.classId