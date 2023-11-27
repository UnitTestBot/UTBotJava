package org.utbot.contest.usvm.jc

import org.jacodb.api.JcType
import org.jacodb.api.JcTypedMethod
import org.usvm.api.JcCoverage
import org.usvm.instrumentation.testcase.UTest
import org.usvm.instrumentation.testcase.api.UTestExecutionResult
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestInst

data class JcExecution(
    val method: JcTypedMethod,
    val uTest: UTest,
    val uTestExecutionResult: UTestResult,
    val coverage: JcCoverage
)

sealed interface UTestResult

class UTestConcreteExecutionResult(val uTestExecutionResult: UTestExecutionResult) : UTestResult

class UTestSymbolicExceptionResult(val exceptionType: JcType) : UTestResult

class UTestSymbolicSuccessResult(
    val initStatements: List<UTestInst>,
    val result: UTestExpression
) : UTestResult
