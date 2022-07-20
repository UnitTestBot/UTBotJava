package org.utbot.python

import org.utbot.framework.plugin.api.UtExecution

object PythonTestCaseGenerator {
    lateinit var testSourceRoot: String
    lateinit var directoriesForSysPath: List<String>
    lateinit var moduleToImport: String
    lateinit var pythonPath: String

    fun init(
        testSourceRoot: String,
        directoriesForSysPath: List<String>,
        moduleToImport: String,
        pythonPath: String
    ) {
        this.testSourceRoot = testSourceRoot
        this.directoriesForSysPath = directoriesForSysPath
        this.moduleToImport = moduleToImport
        this.pythonPath = pythonPath
    }

    fun generate(method: PythonMethod): PythonTestCase {
        val engine = PythonEngine(
            method,
            testSourceRoot,
            directoriesForSysPath,
            moduleToImport,
            pythonPath
        )
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