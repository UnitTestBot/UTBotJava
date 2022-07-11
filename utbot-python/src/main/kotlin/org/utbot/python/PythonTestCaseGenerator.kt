package org.utbot.python/*
class PythonTestCaseGenerator { // : TestCaseGenerator() ?
    fun generate(method: PythonMethod): PythonTestCase {
        engine = PythonEngine(method)
        val executions = mutableListOf<UtExecution>()
        engine.fuzz(method).collect {
            when (it) {
                is UtExecution -> executions += it
                else -> ()
            }
        }
        // TODO: make async

        return PythonTestCase(method, executions)
    }
}
 */