package org.utbot.language.ts.framework.codegen.model.constructor.tree

import org.utbot.language.ts.framework.api.ts.TsClassId
import java.security.AccessControlException
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.CgMethodConstructor
import org.utbot.framework.codegen.model.tree.CgClassId
import org.utbot.framework.codegen.model.tree.CgTestMethod
import org.utbot.framework.codegen.model.tree.CgTestMethodType
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.plugin.api.ConcreteExecutionFailureException
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.onFailure
import org.utbot.framework.plugin.api.onSuccess
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.util.isUnit

class TsCgMethodConstructor(ctx: CgContext) : CgMethodConstructor(ctx) {

    override fun assertEquality(expected: CgValue, actual: CgVariable) {
        testFrameworkManager.assertEquals(expected, actual)
    }

    override fun rememberInitialStaticFields(statics: Map<FieldId, UtModel>) {
        for ((field, _) in statics) {
            val declaringClass = field.declaringClass

            // prevValue is nullable if not accessible because of getStaticFieldValue(..) : Any?
            val prevValue = newVar(
                CgClassId(field.type, isNullable = false),
                "prev${field.name.capitalize()}"
            ) {
                declaringClass[field]
            }
            // remember the previous value of a static field to recover it at the end of the test
            prevStaticFieldValues[field] = prevValue
        }
    }

    override fun substituteStaticFields(statics: Map<FieldId, UtModel>, isParametrized: Boolean) {
        for ((field, model) in statics) {
            val declaringClass = field.declaringClass
            val fieldValue = variableConstructor.getOrCreateVariable(model, field.name)
            declaringClass[field] `=` fieldValue
        }
    }

    override fun recoverStaticFields() {
        for ((field, prevValue) in prevStaticFieldValues) {
            field.declaringClass[field] `=` prevValue
        }
    }

    override fun createTestMethod(executableId: ExecutableId, execution: UtExecution): CgTestMethod =
        withTestMethodScope(execution) {
            val testMethodName = nameGenerator.testMethodNameFor(executableId, execution.testMethodName)
            execution.displayName = execution.displayName?.let { "${executableId.name}: $it" }
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
                        val name = paramNames[executableId]?.get(index)
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
        val method = currentExecutable as MethodId
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
            with(currentExecutable) {
                when (this) {
                    is MethodId -> thisInstance[this](*methodArguments.toTypedArray()).intercepted()
                    is ConstructorId -> this(*methodArguments.toTypedArray()).intercepted()
                    else -> throw IllegalStateException()
                }
            }
        }

        if (shouldTestPassWithException(execution, exception)) {
            testFrameworkManager.expectException(TsClassId(exception.message!!)) {
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

        when (exception) {
            is ConcreteExecutionFailureException -> {
                methodType = CgTestMethodType.CRASH
                writeWarningAboutCrash()
            }

            is AccessControlException -> {
                methodType = CgTestMethodType.CRASH
                writeWarningAboutFailureTest(exception)
                return
            }

            else -> {
                methodType = CgTestMethodType.FAILING
                writeWarningAboutFailureTest(exception)
            }
        }

        methodInvocationBlock()
    }
}