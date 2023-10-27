package org.utbot.contest.usvm.executor

import org.jacodb.api.JcTypedMethod
import org.usvm.api.JcCoverage
import org.usvm.instrumentation.testcase.UTest
import org.usvm.instrumentation.testcase.api.UTestExecutionResult

data class JcExecution(
    val method: JcTypedMethod,
    val uTest: UTest,
    val uTestExecutionResult: UTestExecutionResult,
    val coverage: JcCoverage
)
