package org.utbot.python.framework.api.python.util

import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonBoolModel
import org.utbot.python.framework.api.python.PythonListModel
import org.utbot.python.framework.api.python.PythonTupleModel
import org.utbot.python.framework.api.python.PythonDictModel
import org.utbot.python.framework.api.python.PythonSetModel

// none annotation can be used in code only since Python 3.10
val pythonNoneClassId = PythonClassId("types.NoneType")
val pythonObjectClassId = PythonClassId("builtins.object")
val pythonAnyClassId = NormalizedPythonAnnotation("typing.Any")
val pythonIntClassId = PythonClassId("builtins.int")
val pythonFloatClassId = PythonClassId("builtins.float")
val pythonComplexClassId = PythonClassId("builtins.complex")
val pythonStrClassId = PythonClassId("builtins.str")
val pythonBoolClassId = PythonBoolModel.classId
val pythonRangeClassId = PythonClassId("builtins.range")
val pythonListClassId = PythonListModel.classId
val pythonTupleClassId = PythonTupleModel.classId
val pythonDictClassId = PythonDictModel.classId
val pythonSetClassId = PythonSetModel.classId