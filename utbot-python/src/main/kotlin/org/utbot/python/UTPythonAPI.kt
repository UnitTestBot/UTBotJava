package org.utbot.python

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedConcreteValue
import java.nio.file.Path

data class PythonArgument(val name: String, val type: ClassId?)

interface PythonMethod {
    val name: String
    val returnType: ClassId?
    val arguments: List<PythonArgument>
    fun asString(): String
    fun getConcreteValues(): List<FuzzedConcreteValue>
}

sealed class PythonResult(val parameters: List<UtModel>)

class PythonError(val utError: UtError, parameters: List<UtModel>): PythonResult(parameters)
class PythonExecution(val utExecution: UtExecution, parameters: List<UtModel>): PythonResult(parameters)

data class PythonTestSet(
    val method: PythonMethod,
    val executions: List<PythonExecution>,
    val errors: List<PythonError>
)