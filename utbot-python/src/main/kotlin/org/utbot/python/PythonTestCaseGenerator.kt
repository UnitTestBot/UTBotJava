package org.utbot.python

import org.utbot.framework.plugin.api.UtExecution

object PythonTestCaseGenerator { // : TestCaseGenerator() ?
    fun generate(method: PythonMethod, testSourceRoot: String, projectRoot: String): PythonTestCase {
        val engine = PythonEngine(method, testSourceRoot, projectRoot)
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