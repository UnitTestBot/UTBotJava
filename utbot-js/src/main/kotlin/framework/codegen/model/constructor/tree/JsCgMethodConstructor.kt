package framework.codegen.model.constructor.tree

import framework.api.js.JsClassId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgTestMethod
import org.utbot.framework.codegen.domain.models.CgTestMethodType
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.tree.CgMethodConstructor
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.onFailure
import org.utbot.framework.plugin.api.onSuccess
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.util.isUnit

class JsCgMethodConstructor(ctx: CgContext) : CgMethodConstructor(ctx) {

    override fun assertEquality(expected: CgValue, actual: CgVariable) {
        testFrameworkManager.assertEquals(expected, actual)
    }

    override fun createTestMethod(testSet: CgMethodTestSet, execution: UtExecution): CgTestMethod =
        withTestMethodScope(execution) {
            val testMethodName = nameGenerator.testMethodNameFor(testSet.executableUnderTest, execution.testMethodName)
            execution.displayName = execution.displayName?.let { "${testSet.executableUnderTest.name}: $it" }
            testMethod(testMethodName, execution.displayName) {
                val statics = currentExecution!!.stateBefore.statics
                rememberInitialStaticFields(statics)
                val mainBody = {
                    substituteStaticFields(statics)
                    // build this instance
                    thisInstance = execution.stateBefore.thisInstance?.let {
                        variableConstructor.getOrCreateVariable(it)
                    }
                    // build arguments
                    for ((index, param) in execution.stateBefore.parameters.withIndex()) {
                        val name = paramNames[execution.executableToCall ?: testSet.executableUnderTest]?.get(index)
                        methodArguments += variableConstructor.getOrCreateVariable(param, name)
                    }
                    recordActualResult()
                    generateResultAssertions()
                    generateFieldStateAssertions()
                }

                if (statics.isNotEmpty()) {
                    +tryBlock {
                        mainBody()
                    }.finally {
                        recoverStaticFields()
                    }
                } else {
                    mainBody()
                }
            }
        }

    override fun generateResultAssertions() {
        emptyLineIfNeeded()
        val currentExecution = currentExecution!!
        val method = currentExecutableToCall as MethodId
        // build assertions
        currentExecution.result
            .onSuccess { result ->
                methodType = CgTestMethodType.SUCCESSFUL
                if (result.isUnit() || method.returnType == voidClassId) {
                    +thisInstance[method](*methodArguments.toTypedArray())
                } else {
                    resultModel = result
                    val expected = variableConstructor.getOrCreateVariable(result, "expected")
                    assertEquality(expected, actual)
                }
            }
            .onFailure { exception ->
                processExecutionFailure(currentExecution, exception)
            }
    }

    private fun processExecutionFailure(execution: UtExecution, exception: Throwable) {
        val methodInvocationBlock = {
            with(currentExecutableToCall) {
                when (this) {
                    is MethodId -> thisInstance[this](*methodArguments.toTypedArray()).intercepted()
                    is ConstructorId -> this(*methodArguments.toTypedArray()).intercepted()
                    else -> throw IllegalStateException()
                }
            }
        }

        if (shouldTestPassWithException(execution, exception)) {
            testFrameworkManager.expectException(JsClassId(exception.message!!)) {
                methodInvocationBlock()
            }
            methodType = CgTestMethodType.SUCCESSFUL

            return
        }

        if (shouldTestPassWithTimeoutException(execution, exception)) {
            writeWarningAboutTimeoutExceeding()
            testFrameworkManager.expectTimeout(hangingTestsTimeout.timeoutMs) {
                methodInvocationBlock()
            }
            methodType = CgTestMethodType.TIMEOUT

            return
        }

        methodType = CgTestMethodType.FAILING
        writeWarningAboutFailureTest(exception)

        methodInvocationBlock()
    }
}
