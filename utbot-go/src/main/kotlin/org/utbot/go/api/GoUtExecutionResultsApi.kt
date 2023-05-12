package org.utbot.go.api

import org.utbot.go.framework.api.go.GoUtModel

interface GoUtExecutionResult

interface GoUtExecutionCompleted : GoUtExecutionResult {
    val models: List<GoUtModel>
}

data class GoUtExecutionSuccess(override val models: List<GoUtModel>) : GoUtExecutionCompleted

data class GoUtExecutionWithNonNilError(override val models: List<GoUtModel>) : GoUtExecutionCompleted

data class GoUtPanicFailure(val panicValue: GoUtModel, val panicValueIsErrorMessage: Boolean) : GoUtExecutionResult

data class GoUtTimeoutExceeded(val timeoutMillis: Long) : GoUtExecutionResult

class ExecutionResults(testCase: GoUtFuzzedFunctionTestCase, length: Int) {
    private var successfulExecutionTestCaseWithLengthOfParameters: Pair<GoUtFuzzedFunctionTestCase, Int>? = null
    private var executionWithErrorTestCase: Pair<GoUtFuzzedFunctionTestCase, Int>? = null
    private var panicFailureTestCase: Pair<GoUtFuzzedFunctionTestCase, Int>? = null
    private var timeoutExceededTestCase: Pair<GoUtFuzzedFunctionTestCase, Int>? = null

    private fun Pair<GoUtFuzzedFunctionTestCase, Int>?.relax(
        testCase: GoUtFuzzedFunctionTestCase,
        length: Int
    ): Pair<GoUtFuzzedFunctionTestCase, Int> = if (this == null || this.second > length) {
        testCase to length
    } else {
        this
    }

    private fun Pair<GoUtFuzzedFunctionTestCase, Int>?.relax(
        testCaseAndLength: Pair<GoUtFuzzedFunctionTestCase, Int>?,
    ): Pair<GoUtFuzzedFunctionTestCase, Int>? = if (testCaseAndLength != null) {
        this.relax(testCaseAndLength.first, testCaseAndLength.second)
    } else {
        this
    }

    init {
        when (testCase.executionResult) {
            is GoUtExecutionSuccess -> successfulExecutionTestCaseWithLengthOfParameters = testCase to length
            is GoUtExecutionWithNonNilError -> executionWithErrorTestCase = testCase to length
            is GoUtPanicFailure -> panicFailureTestCase = testCase to length
            is GoUtTimeoutExceeded -> timeoutExceededTestCase = testCase to length
        }
    }

    fun update(testCase: GoUtFuzzedFunctionTestCase, length: Int) = when (testCase.executionResult) {
        is GoUtExecutionSuccess -> successfulExecutionTestCaseWithLengthOfParameters =
            successfulExecutionTestCaseWithLengthOfParameters.relax(testCase, length)

        is GoUtExecutionWithNonNilError -> executionWithErrorTestCase =
            executionWithErrorTestCase.relax(testCase, length)

        is GoUtPanicFailure -> panicFailureTestCase = panicFailureTestCase.relax(testCase, length)
        is GoUtTimeoutExceeded -> timeoutExceededTestCase = timeoutExceededTestCase.relax(testCase, length)
        else -> error("${testCase.executionResult.javaClass.name} is not supported")
    }

    fun update(executionResults: ExecutionResults) {
        successfulExecutionTestCaseWithLengthOfParameters =
            successfulExecutionTestCaseWithLengthOfParameters.relax(executionResults.successfulExecutionTestCaseWithLengthOfParameters)
        executionWithErrorTestCase = executionWithErrorTestCase.relax(executionResults.executionWithErrorTestCase)
        panicFailureTestCase = panicFailureTestCase.relax(executionResults.panicFailureTestCase)
        timeoutExceededTestCase = timeoutExceededTestCase.relax(executionResults.timeoutExceededTestCase)
    }

    fun getTestCases(): List<GoUtFuzzedFunctionTestCase> = listOfNotNull(
        successfulExecutionTestCaseWithLengthOfParameters?.first,
        executionWithErrorTestCase?.first,
        panicFailureTestCase?.first,
        timeoutExceededTestCase?.first
    )
}