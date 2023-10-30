package org.utbot.python.engine

import org.utbot.framework.plugin.api.UtError
import org.utbot.python.framework.api.python.PythonUtExecution

class ExecutionStorage {
    val fuzzingExecutions: MutableList<PythonUtExecution> = mutableListOf()
    val fuzzingErrors: MutableList<UtError> = mutableListOf()

    val symbolicExecutions: MutableList<PythonUtExecution> = mutableListOf()
    val symbolicErrors: MutableList<UtError> = mutableListOf()

    fun saveFuzzingExecution(feedback: ExecutionFeedback) {
        when (feedback) {
            is ValidExecution -> {
                fuzzingExecutions += feedback.utFuzzedExecution
            }
            is InvalidExecution -> {
                fuzzingErrors += feedback.utError
            }
            else -> {}
        }
    }

    fun saveSymbolicExecution(feedback: ExecutionFeedback) {
        when (feedback) {
            is ValidExecution -> {
                symbolicExecutions += feedback.utFuzzedExecution
            }
            is InvalidExecution -> {
                symbolicErrors += feedback.utError
            }
            else -> {}
        }
    }
}