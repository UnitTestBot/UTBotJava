package org.utbot.usvm.jc

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jacodb.api.JcClassType
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcField
import org.jacodb.api.JcMethod
import org.jacodb.api.JcType
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.ext.constructors
import org.jacodb.api.ext.findTypeOrNull
import org.jacodb.api.ext.objectType
import org.jacodb.api.ext.toType
import org.jacodb.approximation.JcEnrichedVirtualField
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
import org.usvm.instrumentation.testcase.api.UTestExecutionFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionSuccessResult
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestMethodCall
import org.usvm.instrumentation.testcase.api.UTestMockObject
import org.usvm.instrumentation.testcase.api.UTestNullExpression
import org.usvm.instrumentation.testcase.api.UTestStaticMethodCall
import org.usvm.machine.JcContext
import org.usvm.machine.JcMocker
import org.usvm.machine.state.JcMethodResult
import org.usvm.machine.state.JcState
import org.usvm.machine.state.localIdx
import org.usvm.memory.ULValue
import org.usvm.memory.UReadOnlyMemory
import org.usvm.memory.URegisterStackLValue
import org.usvm.model.UModelBase

private val logger = KotlinLogging.logger {}

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
    fun execute(
        method: JcTypedMethod,
        state: JcState,
        stringConstants: Map<String, UConcreteHeapRef>,
        classConstants: Map<JcType, UConcreteHeapRef>,
        allowSymbolicResult: Boolean
    ): JcExecution? {
        val model = state.models.first()
        val mocker = state.memory.mocker as JcMocker

        val resolvedMethodMocks = mocker.symbols
            .entries
            .groupBy({ model.eval(it.key) }, { it.value })
            .mapValues { it.value.flatten() }

        val uTest = createUTest(method, state, stringConstants, classConstants)

        val concreteResult = runCatching {
            runBlocking {
                UTestConcreteExecutionResult(runner.executeAsync(uTest))
            }
        }
            .onFailure { e -> logger.warn(e) { "Recoverable: runner.executeAsync(uTest) failed on ${method.method}" } }
            .getOrNull()

        val symbolicResult by lazy {
            if (allowSymbolicResult) {
                when (val methodResult = state.methodResult) {
                    is JcMethodResult.JcException -> UTestSymbolicExceptionResult(methodResult.type)
                    is JcMethodResult.Success -> {
                        val resultScope = MemoryScope(
                            state.ctx,
                            model,
                            state.memory,
                            stringConstants,
                            classConstants,
                            resolvedMethodMocks,
                            method
                        )
                        val resultExpr = resultScope.resolveExpr(methodResult.value, method.returnType)
                        val resultInitializer = resultScope.decoderApi.initializerInstructions()
                        UTestSymbolicSuccessResult(resultInitializer, resultExpr)
                    }

                    JcMethodResult.NoCall -> null
                }
            } else null
        }

        val testExecutionResult = concreteResult?.uTestExecutionResult

        // Drop crashed executions
        if (testExecutionResult is UTestExecutionFailedResult) {
            logger.warn { "JVM crash in concrete execution for method ${method.method}, dropping state" }
            return null
        }

        // sometimes symbolic result more preferable than concolic
        val preferableResult =
            if (testExecutionResult is UTestExecutionSuccessResult || testExecutionResult is UTestExecutionExceptionResult) {
                concreteResult
            } else {
                symbolicResult ?: concreteResult
            }

        val coverage = resolveCoverage(method, state)

        return JcExecution(
            method = method,
            uTest = uTest,
            uTestExecutionResultWrappers = sequence {
                preferableResult?.let { yield(it) }
                if (preferableResult !== symbolicResult)
                    symbolicResult?.let { yield(it) }
            },
            coverage = coverage
        )
    }

    fun createUTest(
        method: JcTypedMethod,
        state: JcState,
        stringConstants: Map<String, UConcreteHeapRef>,
        classConstants: Map<JcType, UConcreteHeapRef>,
    ): UTest {
        val model = state.models.first()
        val ctx = state.ctx

        val mocker = state.memory.mocker as JcMocker
        // val staticMethodMocks = mocker.statics TODO global mocks?????????????????????????
        val methodMocks = mocker.symbols

        val resolvedMethodMocks = methodMocks
            .entries
            .groupBy({ model.eval(it.key) }, { it.value })
            .mapValues { it.value.flatten() }

        val memoryScope = MemoryScope(ctx, model, model, stringConstants, classConstants, resolvedMethodMocks, method)

        return memoryScope.createUTest()
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
        classConstants: Map<JcType, UConcreteHeapRef>,
        private val resolvedMethodMocks: Map<UHeapRef, List<UMockSymbol<*>>>,
        method: JcTypedMethod,
    ) : JcTestStateResolver<UTestExpression>(ctx, model, memory, stringConstants, classConstants, method) {

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

        override fun allocateString(value: UTestExpression): UTestExpression {
            val stringConstructor = ctx.stringType.constructors
                .firstOrNull { it.parameters.size == 1 && it.parameters.single().type == value.type }
                ?: error("Can't allocate string from value: $value")
            return decoderApi.invokeMethod(stringConstructor.method, listOf(value))
        }

        override fun resolveObject(ref: UConcreteHeapRef, heapRef: UHeapRef, type: JcClassType): UTestExpression {
            if (ref !in resolvedMethodMocks || type.jcClass.name != "java.util.Random") {
                return super.resolveObject(ref, heapRef, type)
            }

            // Hack: mock only Random

            val mocks = resolvedMethodMocks.getValue(ref)

            val fieldValues = mutableMapOf<JcField, UTestExpression>()
            val methods = mutableMapOf<JcMethod, List<UTestExpression>>()

            val instance = UTestMockObject(type, fieldValues, methods)
            saveResolvedRef(ref.address, instance)

            val mockedMethodValues = mutableMapOf<JcMethod, MutableList<UIndexedMethodReturnValue<JcMethod, *>>>()
            mocks.filterIsInstance<UIndexedMethodReturnValue<JcMethod, *>>().forEach { mockValue ->
                // todo: filter out approximations-only methods
                mockedMethodValues.getOrPut(mockValue.method) { mutableListOf() }.add(mockValue)
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
