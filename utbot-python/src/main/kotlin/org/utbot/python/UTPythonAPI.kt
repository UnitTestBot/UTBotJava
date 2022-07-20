package org.utbot.python

import io.github.danielnaczo.python3parser.model.stmts.compoundStmts.functionStmts.FunctionDef
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.fuzzer.FuzzedConcreteValue
import java.nio.file.Path

data class PythonArgument(val name: String, val type: ClassId?)

interface PythonMethod {
    val name: String
    val returnType: ClassId?
    val arguments: List<PythonArgument>
    fun asString(): String
    fun ast(): FunctionDef
    fun getConcreteValues(): List<FuzzedConcreteValue>
}

data class PythonTestCase(
    val method: PythonMethod,
    val executions: List<UtExecution>
)