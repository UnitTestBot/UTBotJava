package org.utbot.python

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtExecution

data class PythonArgument(val name: String, val type: ClassId?)

interface PythonMethod {
    val name: String
    val returnType: ClassId?
    val arguments: List<PythonArgument>
    fun asString(): String
}

data class PythonTestCase(
    val method: PythonMethod,
    val executions: List<UtExecution>
)