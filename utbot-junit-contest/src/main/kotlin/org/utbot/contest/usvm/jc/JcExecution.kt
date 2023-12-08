package org.utbot.contest.usvm.jc

import org.jacodb.api.JcType
import org.jacodb.api.JcTypedMethod
import org.usvm.api.JcCoverage
import org.usvm.instrumentation.testcase.UTest
import org.usvm.instrumentation.testcase.api.UTestExecutionResult
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestInst

/**
 * [uTestExecutionResultWrappers] may contain either:
 *  - both concrete and symbolic results in priority decreasing order
 *  - single symbolic or concrete result if we discarded one of the result kinds
 */
data class JcExecution(
    val method: JcTypedMethod,
    val uTest: UTest,
    val uTestExecutionResultWrappers: Sequence<UTestResultWrapper>,
    val coverage: JcCoverage
)

sealed interface UTestResultWrapper

class UTestConcreteExecutionResult(
    val uTestExecutionResult: UTestExecutionResult
) : UTestResultWrapper

class UTestSymbolicExceptionResult(
    val exceptionType: JcType
) : UTestResultWrapper

class UTestSymbolicSuccessResult(
    val initStatements: List<UTestInst>,
    val result: UTestExpression
) : UTestResultWrapper
