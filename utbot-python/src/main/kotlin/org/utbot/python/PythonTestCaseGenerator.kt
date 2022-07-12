package org.utbot.python

import PythonEngine
import org.utbot.framework.plugin.api.UtExecution

class PythonTestCaseGenerator { // : TestCaseGenerator() ?
    fun generate(method: PythonMethod): PythonTestCase {
        val engine = PythonEngine(method)
        val executions = mutableListOf<UtExecution>()

        engine.fuzzing().forEach {
            when (it) {
                is UtExecution -> executions += it
                else -> Unit
            }
        }

        return PythonTestCase(method, executions)
    }
}