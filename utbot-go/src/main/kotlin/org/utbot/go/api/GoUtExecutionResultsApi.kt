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
    private var executionWithErrorTestCaseWithLengthOfParameters: Pair<GoUtFuzzedFunctionTestCase, Int>? = null
    private var panicFailureTestCaseWithLengthOfParameters: Pair<GoUtFuzzedFunctionTestCase, Int>? = null
    private var timeoutExceededTestCaseWithLengthOfParameters: Pair<GoUtFuzzedFunctionTestCase, Int>? = null

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
            is GoUtExecutionWithNonNilError -> executionWithErrorTestCaseWithLengthOfParameters = testCase to length
            is GoUtPanicFailure -> panicFailureTestCaseWithLengthOfParameters = testCase to length
            is GoUtTimeoutExceeded -> timeoutExceededTestCaseWithLengthOfParameters = testCase to length
        }
    }

    fun update(testCase: GoUtFuzzedFunctionTestCase, length: Int) = when (testCase.executionResult) {
        is GoUtExecutionSuccess -> successfulExecutionTestCaseWithLengthOfParameters =
            successfulExecutionTestCaseWithLengthOfParameters.relax(testCase, length)

        is GoUtExecutionWithNonNilError -> executionWithErrorTestCaseWithLengthOfParameters =
            executionWithErrorTestCaseWithLengthOfParameters.relax(testCase, length)

        is GoUtPanicFailure -> panicFailureTestCaseWithLengthOfParameters =
            panicFailureTestCaseWithLengthOfParameters.relax(testCase, length)

        is GoUtTimeoutExceeded -> timeoutExceededTestCaseWithLengthOfParameters =
            timeoutExceededTestCaseWithLengthOfParameters.relax(testCase, length)

        else -> error("${testCase.executionResult.javaClass.name} is not supported")
    }

    fun update(executionResults: ExecutionResults) {
        successfulExecutionTestCaseWithLengthOfParameters =
            successfulExecutionTestCaseWithLengthOfParameters.relax(executionResults.successfulExecutionTestCaseWithLengthOfParameters)
        executionWithErrorTestCaseWithLengthOfParameters =
            executionWithErrorTestCaseWithLengthOfParameters.relax(executionResults.executionWithErrorTestCaseWithLengthOfParameters)
        panicFailureTestCaseWithLengthOfParameters =
            panicFailureTestCaseWithLengthOfParameters.relax(executionResults.panicFailureTestCaseWithLengthOfParameters)
        timeoutExceededTestCaseWithLengthOfParameters =
            timeoutExceededTestCaseWithLengthOfParameters.relax(executionResults.timeoutExceededTestCaseWithLengthOfParameters)
    }

    fun getTestCases(): List<GoUtFuzzedFunctionTestCase> = listOfNotNull(
        successfulExecutionTestCaseWithLengthOfParameters?.first,
        executionWithErrorTestCaseWithLengthOfParameters?.first,
        panicFailureTestCaseWithLengthOfParameters?.first,
        timeoutExceededTestCaseWithLengthOfParameters?.first
    )
}