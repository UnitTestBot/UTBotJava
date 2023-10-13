package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.FunctionType
import org.utbot.python.newtyping.general.UtType

open class PythonDefinition(open val meta: PythonDefinitionDescription, open val type: UtType) {
    override fun toString(): String =
        "${meta.name}: ${type.pythonTypeRepresentation()}"
}

class PythonFunctionDefinition(
    override val meta: PythonFuncItemDescription,  // TODO: consider overloaded function
    override val type: FunctionType
): PythonDefinition(meta, type)

sealed class PythonDefinitionDescription(val name: String)

class PythonVariableDescription(
    name: String,
    val isProperty: Boolean = false,
    val isSelf: Boolean = false
): PythonDefinitionDescription(name)

sealed class PythonFunctionDescription(name: String): PythonDefinitionDescription(name)

class PythonFuncItemDescription(
    name: String,
    val args: List<PythonVariableDescription>
): PythonFunctionDescription(name)

class PythonOverloadedFuncDefDescription(
    name: String,
    val items: List<PythonDefinitionDescription>
): PythonFunctionDescription(name)