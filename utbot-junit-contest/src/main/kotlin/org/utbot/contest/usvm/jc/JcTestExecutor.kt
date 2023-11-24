package org.utbot.contest.usvm.jc

import kotlinx.coroutines.runBlocking
import org.jacodb.api.JcClassType
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcType
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.ext.objectType
import org.usvm.UConcreteHeapRef
import org.usvm.UExpr
import org.usvm.api.JcCoverage
import org.usvm.api.JcTest
import org.usvm.api.util.JcTestStateResolver
import org.usvm.instrumentation.executor.UTestConcreteExecutor
import org.usvm.instrumentation.testcase.UTest
import org.usvm.instrumentation.testcase.api.UTestAllocateMemoryCall
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestMethodCall
import org.usvm.instrumentation.testcase.api.UTestNullExpression
import org.usvm.instrumentation.testcase.api.UTestStaticMethodCall
import org.usvm.machine.JcContext
import org.usvm.machine.state.JcState
import org.usvm.machine.state.localIdx
import org.usvm.memory.ULValue
import org.usvm.memory.UReadOnlyMemory
import org.usvm.memory.URegisterStackLValue
import org.usvm.model.UModelBase

/**
 * A class, responsible for resolving a single [JcExecution] for a specific method from a symbolic state.
 *
 * Uses concrete execution
 */
// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
class JcTestExecutor(
    val classpath: JcClasspath,
    private val runner: UTestConcreteExecutor
) {
    /**
     * Resolves a [JcTest] from a [method] from a [state].
     */
    fun execute(method: JcTypedMethod, state: JcState, stringConstants: Map<String, UConcreteHeapRef>): JcExecution {
        val model = state.models.first()

        val ctx = state.ctx

        val memoryScope = MemoryScope(ctx, model, model, stringConstants, method)

        val uTest = memoryScope.createUTest()

        val execResult = runBlocking {
            runner.executeAsync(uTest)
        }

        val coverage = resolveCoverage(method, state)

        return JcExecution(method, uTest, execResult, coverage)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun resolveCoverage(method: JcTypedMethod, state: JcState): JcCoverage {
        // TODO: extract coverage
        return JcCoverage(emptyMap())
    }

    /**
     * An actual class for resolving objects from [UExpr]s.
     *
     * @param model a model to which compose expressions.
     * @param memory a read-only memory to read [ULValue]s from.
     */
    private class MemoryScope(
        ctx: JcContext,
        model: UModelBase<JcType>,
        memory: UReadOnlyMemory<JcType>,
        stringConstants: Map<String, UConcreteHeapRef>,
        method: JcTypedMethod,
    ) : JcTestStateResolver<UTestExpression>(ctx, model, memory, stringConstants, method) {

        override val decoderApi = JcTestExecutorDecoderApi(ctx)

        fun createUTest(): UTest {
            val thisInstance = if (!method.isStatic) {
                val ref = URegisterStackLValue(ctx.addressSort, idx = 0)
                resolveLValue(ref, method.enclosingType)
            } else {
                UTestNullExpression(ctx.cp.objectType)
            }

            val parameters = method.parameters.mapIndexed { idx, param ->
                val registerIdx = method.method.localIdx(idx)
                val ref = URegisterStackLValue(ctx.typeToSort(param.type), registerIdx)
                resolveLValue(ref, param.type)
            }

            val initStmts = decoderApi.initializerInstructions()
            val callExpr = if (method.isStatic) {
                UTestStaticMethodCall(method.method, parameters)
            } else {
                UTestMethodCall(thisInstance, method.method, parameters)
            }
            return UTest(initStmts, callExpr)
        }

        override fun allocateClassInstance(type: JcClassType): UTestExpression =
            UTestAllocateMemoryCall(type.jcClass)

        // todo: looks incorrect
        override fun allocateString(value: UTestExpression): UTestExpression = value
    }
}
