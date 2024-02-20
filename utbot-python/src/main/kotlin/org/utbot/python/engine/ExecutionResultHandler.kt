package org.utbot.python.engine

import mu.KotlinLogging
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.coverage.PyInstruction
import org.utbot.python.coverage.buildCoverage
import org.utbot.python.engine.utils.transformModelList
import org.utbot.python.evaluation.PythonEvaluationSuccess
import org.utbot.python.evaluation.serialization.toPythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utpython.types.general.UtType
import org.utpython.types.pythonTypeRepresentation
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo

private val logger = KotlinLogging.logger {  }

object ExecutionResultHandler {
    fun handleTimeoutResult(
        method: PythonMethod,
        arguments: List<PythonTreeModel>,
        coveredInstructions: List<PyInstruction>,
        summary: List<DocStatement>? = null,
    ): ExecutionFeedback {
        val hasThisObject = method.hasThisArgument
        val (beforeThisObject, beforeModelList) = if (hasThisObject) {
            arguments.first() to arguments.drop(1)
        } else {
            null to arguments
        }
        val executionResult = UtTimeoutException(TimeoutException("Execution is too long"))
        val testMethodName = suggestExecutionName(method, executionResult)
        val coverage = Coverage(coveredInstructions)
        val utFuzzedExecution = PythonUtExecution(
            stateInit = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            stateBefore = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            stateAfter = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            diffIds = emptyList(),
            result = executionResult,
            coverage = coverage,
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary,
            arguments = method.argumentsWithoutSelf
        )
        return ValidExecution(utFuzzedExecution)
    }

    fun handleSuccessResult(
        method: PythonMethod,
        configuration: PythonTestGenerationConfig,
        types: List<UtType>,
        evaluationResult: PythonEvaluationSuccess,
        summary: List<DocStatement>? = null,
    ): ExecutionFeedback {
        val hasThisObject = method.hasThisArgument

        val resultModel = evaluationResult.stateAfter.getById(evaluationResult.resultId).toPythonTree(evaluationResult.stateAfter)

        if (evaluationResult.isException && (resultModel.type.name in configuration.prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
            val errorMessage = "Evaluation with prohibited exception. Substituted types: ${
                types.joinToString { it.pythonTypeRepresentation() }
            }. Exception type: ${resultModel.type.name}"

            logger.debug { errorMessage }
            return TypeErrorFeedback(errorMessage)
        }
        val executionResult =
            if (evaluationResult.isException) {
                UtImplicitlyThrownException(Throwable(resultModel.type.toString()), false)
            }
            else {
                UtExecutionSuccess(PythonTreeModel(resultModel))
            }
        val testMethodName = suggestExecutionName(method, executionResult)

        val (thisObject, initModelList) = transformModelList(hasThisObject, evaluationResult.stateInit, evaluationResult.modelListIds)
        val (beforeThisObject, beforeModelList) = transformModelList(hasThisObject, evaluationResult.stateBefore, evaluationResult.modelListIds)
        val (afterThisObject, afterModelList) = transformModelList(hasThisObject, evaluationResult.stateAfter, evaluationResult.modelListIds)

        val utFuzzedExecution = PythonUtExecution(
            stateInit = EnvironmentModels(thisObject, initModelList, emptyMap(), executableToCall = null),
            stateBefore = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            stateAfter = EnvironmentModels(afterThisObject, afterModelList, emptyMap(), executableToCall = null),
            diffIds = evaluationResult.diffIds,
            result = executionResult,
            coverage = buildCoverage(evaluationResult.coveredStatements, evaluationResult.missedStatements),
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary,
            arguments = method.argumentsWithoutSelf,
        )
        return ValidExecution(utFuzzedExecution)
    }

    private fun suggestExecutionName(
        method: PythonMethod,
        executionResult: UtExecutionResult
    ): TestSuggestedInfo {
        val testSuffix = when (executionResult) {
            is UtExecutionSuccess -> {
                // can be improved
                method.name
            }
            is UtExecutionFailure -> "${method.name}_with_exception"
            else -> method.name
        }
        val testName = "test_$testSuffix"
        return TestSuggestedInfo(
            testName,
            testName,
        )
    }

}