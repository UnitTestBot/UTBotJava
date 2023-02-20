package org.utbot.python

import org.parsers.python.ast.Block
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.*
import org.utbot.python.typing.MypyAnnotations

data class PythonArgument(val name: String, val annotation: String?)

class PythonMethodHeader(
    val name: String,
    val moduleFilename: String,
    val containingPythonClassId: PythonClassId?
)

class PythonMethod(
    val name: String,
    val moduleFilename: String,
    val containingPythonClassId: PythonClassId?,
    val codeAsString: String,
    var definition: PythonFunctionDefinition,
    val ast: Block
) {
    fun methodSignature(): String = "$name(" + arguments.joinToString(", ") {
        "${it.name}: ${it.annotation ?: pythonAnyClassId.name}"
    } + ")"

    /*
    Check that the first argument is `self` of `cls`.
    TODO: Now we think that all class methods has `self` argument! We should support `@property` decorator
     */
    val hasThisArgument: Boolean
        get() = containingPythonClassId != null

    val arguments: List<PythonArgument>
        get() {
            val paramNames = definition.meta.args.map { it.name }
            return (definition.type.arguments zip paramNames).map {
                PythonArgument(it.second, it.first.pythonTypeRepresentation())
            }
        }

    val thisObjectName: String?
        get() = if (hasThisArgument) arguments[0].name else null

    val argumentsNames: List<String>
        get() = arguments.map { it.name }.drop(if (hasThisArgument) 1 else 0)
}

data class PythonTestSet(
    val method: PythonMethod,
    val executions: List<UtExecution>,
    val errors: List<UtError>,
    val mypyReport: List<MypyAnnotations.MypyReportLine>,
    val classId: PythonClassId? = null,
)

data class FunctionArguments(
    val thisObject: PythonTreeModel?,
    val thisObjectName: String?,
    val arguments: List<PythonTreeModel>,
    val names: List<String?>,
) {
    val allArguments: List<PythonTreeModel> = (listOf(thisObject) + arguments).filterNotNull()
}