package org.utbot.python.framework.external

import org.utbot.python.PythonMethod
import org.utpython.types.pythonTypeName

class PythonTestMethodInfo(
    val methodName: PythonObjectName,
    val moduleFilename: String,
    val containingClassName: PythonObjectName? = null
)

fun PythonMethod.toPythonMethodInfo() = PythonTestMethodInfo(
    PythonObjectName(this.name),
    this.moduleFilename,
    this.containingPythonClass?.let { PythonObjectName(it.pythonTypeName()) }
)
