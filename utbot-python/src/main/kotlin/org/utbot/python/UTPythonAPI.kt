package org.utbot.python

import org.parsers.python.ast.Block
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.utils.isNamed

data class PythonArgument(
    val name: String,
    val annotation: String?,
    val isNamed: Boolean = false,
)

class PythonMethodHeader(
    val name: String,
    val moduleFilename: String,
    val containingPythonClassId: PythonClassId?
)

class PythonMethod(
    val name: String,
    val moduleFilename: String,
    val containingPythonClass: CompositeType?,
    val codeAsString: String,
    var definition: PythonFunctionDefinition,
    val ast: Block
) {

    fun methodSignature(): String = "$name(" + arguments.joinToString(", ") {
        "${it.name}: ${it.annotation ?: pythonAnyClassId.name}"
    } + ")"

    /*
    Check that the first argument is `self` of `cls`.
    TODO: We should support `@property` decorator
     */
    val hasThisArgument: Boolean
        get() = containingPythonClass != null && definition.meta.args.any { it.isSelf }

    val arguments: List<PythonArgument>
        get() {
            val meta = definition.type.pythonDescription() as PythonCallableTypeDescription
            return (definition.type.arguments).mapIndexed { index, type ->
                PythonArgument(
                    meta.argumentNames[index]!!,
                    type.pythonTypeRepresentation(),  // TODO: improve pythonTypeRepresentation
                    isNamed(meta.argumentKinds[index])
                )
            }
        }

    val argumentsWithoutSelf: List<PythonArgument>
        get() = if (hasThisArgument) arguments.drop(1) else arguments

    val thisObjectName: String?
        get() = if (hasThisArgument) arguments[0].name else null

    val argumentsNames: List<String>
        get() = arguments.map { it.name }.drop(if (hasThisArgument) 1 else 0)
}

data class PythonTestSet(
    val method: PythonMethod,
    val executions: List<UtExecution>,
    val errors: List<UtError>,
    val executionsNumber: Int = 0,
    val clustersInfo: List<Pair<UtClusterInfo?, IntRange>> = listOf(null to executions.indices)
)

data class FunctionArguments(
    val thisObject: PythonTreeModel?,
    val thisObjectName: String?,
    val arguments: List<PythonTreeModel>,
    val names: List<String?>,
) {
    val allArguments: List<PythonTreeModel> = (listOf(thisObject) + arguments).filterNotNull()
}