package org.utbot.python.engine

import org.utbot.framework.plugin.api.UtError
import org.utbot.python.framework.api.python.PythonUtExecution

class ExecutionStorage {
    val fuzzingExecutions: MutableList<PythonUtExecution> = mutableListOf()
    val fuzzingErrors: MutableList<UtError> = mutableListOf()

    fun saveFuzzingExecution(feedback: ExecutionFeedback) {
        when (feedback) {
            is ValidExecution -> {
                synchronized(fuzzingExecutions) {
                    fuzzingExecutions += feedback.utFuzzedExecution
                }
            }
            is InvalidExecution -> {
                synchronized(fuzzingErrors) {
                    fuzzingErrors += feedback.utError
                }
            }
            else -> {}
        }
    }
}
