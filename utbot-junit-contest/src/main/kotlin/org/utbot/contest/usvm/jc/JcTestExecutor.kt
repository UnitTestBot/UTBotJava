package org.utbot.contest.usvm.jc

import kotlinx.coroutines.runBlocking
import org.jacodb.api.JcClassType
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcField
import org.jacodb.api.JcMethod
import org.jacodb.api.JcType
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.ext.findTypeOrNull
import org.jacodb.api.ext.methods
import org.jacodb.api.ext.objectType
import org.jacodb.api.ext.toType
import org.jacodb.approximation.JcEnrichedVirtualField
import org.jacodb.approximation.JcEnrichedVirtualMethod
import org.usvm.UConcreteHeapRef
import org.usvm.UExpr
import org.usvm.UHeapRef
import org.usvm.UIndexedMethodReturnValue
import org.usvm.UMockSymbol
import org.usvm.api.JcCoverage
import org.usvm.api.JcTest
import org.usvm.api.util.JcTestStateResolver
import org.usvm.collection.field.UFieldLValue
import org.usvm.instrumentation.executor.UTestConcreteExecutor
import org.usvm.instrumentation.testcase.UTest
import org.usvm.instrumentation.testcase.api.UTestAllocateMemoryCall
import org.usvm.instrumentation.testcase.api.UTestExecutionExceptionResult
import org.usvm.instrumentation.testcase.api.UTestExecutionSuccessResult
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestMethodCall
import org.usvm.instrumentation.testcase.api.UTestMockObject
import org.usvm.instrumentation.testcase.api.UTestNullExpression
import org.usvm.instrumentation.testcase.api.UTestStaticMethodCall
import org.usvm.machine.JcContext
import org.usvm.machine.state.JcMethodResult
import org.usvm.machine.JcMocker
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

        val mocker = state.memory.mocker as JcMocker
//        val staticMethodMocks = mocker.statics TODO global mocks?????????????????????????
        val methodMocks = mocker.symbols

        val resolvedMethodMocks = methodMocks.entries.groupBy({ model.eval(it.key) }, { it.value })
            .mapValues { it.value.flatten() }

        val memoryScope = MemoryScope(ctx, model, model, stringConstants, resolvedMethodMocks, method)

        val uTest = memoryScope.createUTest()

        val execResult = runBlocking {
            runner.executeAsync(uTest)
        }

        // sometimes symbolic result is preferable that concolic: e.g. if concrete times out
        val preferableResult =
            if (execResult !is UTestExecutionSuccessResult && execResult !is UTestExecutionExceptionResult) {
            val symbolicResult = state.methodResult
            when (symbolicResult) {
                is JcMethodResult.JcException -> UTestSymbolicExceptionResult(symbolicResult.type)
                is JcMethodResult.Success -> {
                    val resultScope = MemoryScope(ctx, model, state.memory, stringConstants, resolvedMethodMocks, method)
                    val resultExpr = resultScope.resolveExpr(symbolicResult.value, method.returnType)
                    val resultInitializer = resultScope.decoderApi.initializerInstructions()
                    UTestSymbolicSuccessResult(resultInitializer, resultExpr)
                }
                JcMethodResult.NoCall -> UTestConcreteExecutionResult(execResult)
            }
        } else {
            UTestConcreteExecutionResult(execResult)
        }

        val coverage = resolveCoverage(method, state)

        return JcExecution(method, uTest, preferableResult, coverage)
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
        private val resolvedMethodMocks: Map<UHeapRef, List<UMockSymbol<*>>>,
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

        override fun resolveObject(ref: UConcreteHeapRef, heapRef: UHeapRef, type: JcClassType): UTestExpression {
            if (ref !in resolvedMethodMocks) {
                return super.resolveObject(ref, heapRef, type)
            }

            val mocks = resolvedMethodMocks.getValue(ref)

            val fieldValues = mutableMapOf<JcField, UTestExpression>()
            val methods = mutableMapOf<JcMethod, List<UTestExpression>>()

            val instance = UTestMockObject(type, fieldValues, methods)
            saveResolvedRef(ref.address, instance)

            val mockedMethodValues = mutableMapOf<JcMethod, MutableList<UIndexedMethodReturnValue<JcMethod, *>>>()
            mocks.filterIsInstance<UIndexedMethodReturnValue<JcMethod, *>>().forEach { mockValue ->
                var method = mockValue.method

                // Find original method
                if (method is JcEnrichedVirtualMethod) {
                    method = method.enclosingClass.methods
                        .filter { it !is JcEnrichedVirtualMethod }
                        .singleOrNull { it.name == method.name && it.description == method.description }
                        ?: return@forEach
                }

                mockedMethodValues.getOrPut(method) { mutableListOf() }.add(mockValue)
            }

            mockedMethodValues.forEach { (method, values) ->
                val mockedValueType = requireNotNull(ctx.cp.findTypeOrNull(method.returnType)) {
                    "No such type found: ${method.returnType}"
                }

                methods[method] = values
                    .sortedBy { it.callIndex }
                    .map { resolveExpr(it, mockedValueType) }
            }

            val fields = generateSequence(type.jcClass) { it.superClass }
                .map { it.toType() }
                .flatMap { it.declaredFields }
                .filter { !it.isStatic }
                .filterNot { it.field is JcEnrichedVirtualField }

            for (field in fields) {
                val lvalue = UFieldLValue(ctx.typeToSort(field.fieldType), heapRef, field.field)
                val fieldValue = resolveLValue(lvalue, field.fieldType)

                fieldValues[field.field] = fieldValue
            }

            return instance
        }
    }
}
