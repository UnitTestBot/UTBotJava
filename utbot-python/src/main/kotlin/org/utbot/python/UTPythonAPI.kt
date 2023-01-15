package org.utbot.python

import org.parsers.python.ast.Block
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.PythonCallableTypeDescription
import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.pythonDescription
import org.utbot.python.newtyping.pythonTypeRepresentation
import org.utbot.python.typing.MypyAnnotations

data class PythonArgument(val name: String, val annotation: String?)

class PythonMethodDescription(
    val name: String,
    val moduleFilename: String,
    val containingPythonClassId: PythonClassId?
)

class PythonMethod(
    val name: String,
    val moduleFilename: String,
    val containingPythonClassId: PythonClassId?,
    val codeAsString: String,
    var type: FunctionType,
    val ast: Block
) {
    fun methodSignature(): String = "$name(" + arguments.joinToString(", ") {
        "${it.name}: ${it.annotation ?: pythonAnyClassId.name}"
    } + ")"
    val arguments: List<PythonArgument>
        get() {
            val paramNames = (type.pythonDescription() as PythonCallableTypeDescription).argumentNames
            return (type.arguments zip paramNames).map { PythonArgument(it.second, it.first.pythonTypeRepresentation()) }
        }
}

data class PythonTestSet(
    val method: PythonMethod,
    val executions: List<UtExecution>,
    val errors: List<UtError>,
    val mypyReport: List<MypyAnnotations.MypyReportLine>,
    val classId: PythonClassId? = null,
)