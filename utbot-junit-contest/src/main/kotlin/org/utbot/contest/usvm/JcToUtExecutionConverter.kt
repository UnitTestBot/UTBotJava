package org.utbot.contest.usvm

import mu.KotlinLogging
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.cfg.JcInst
import org.jacodb.api.ext.jcdbSignature
import org.usvm.instrumentation.testcase.api.UTestExecutionExceptionResult
import org.usvm.instrumentation.testcase.api.UTestExecutionFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionInitFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionResult
import org.usvm.instrumentation.testcase.api.UTestExecutionState
import org.usvm.instrumentation.testcase.api.UTestExecutionSuccessResult
import org.usvm.instrumentation.testcase.api.UTestExecutionTimedOutResult
import org.usvm.instrumentation.testcase.descriptor.Descriptor2ValueConverter
import org.usvm.instrumentation.testcase.descriptor.UTestExceptionDescriptor
import org.usvm.instrumentation.util.enclosingClass
import org.usvm.instrumentation.util.enclosingMethod
import org.utbot.contest.usvm.executor.JcExecution
import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzer.IdGenerator

private val logger = KotlinLogging.logger {}

class JcToUtExecutionConverter(
    private val jcExecution: JcExecution,
    private val idGenerator: IdGenerator<Int>,
    private val instructionIdProvider: InstructionIdProvider,
) {
    //TODO usvm-sbft: obtain in somehow from [JcExecution] or somewhere else
    private val testClassId: ClassId = objectClassId

    private val toValueConverter = Descriptor2ValueConverter(utContext.classLoader)

    private var modelConverter: JcToUtModelConverter

    init {
        val utilMethodProvider = TestClassUtilMethodProvider(testClassId)
        val instToModelConverter = UTestInst2UtModelConverter(idGenerator, utilMethodProvider)
        val uTestProcessResult = instToModelConverter.processUTest(jcExecution.uTest)

        modelConverter = JcToUtModelConverter(idGenerator, uTestProcessResult.exprToModelCache)
    }

    fun convert(): UtExecution? {
        val coverage = convertCoverage(getTrace(jcExecution.uTestExecutionResult), jcExecution.method.enclosingType.jcClass)
        // TODO usvm-sbft: fill up instrumentation with data from UTest
        val instrumentation = emptyList<UtInstrumentation>()

        val utUsvmExecution: UtUsvmExecution = when (val executionResult = jcExecution.uTestExecutionResult) {
            is UTestExecutionSuccessResult -> UtUsvmExecution(
                stateBefore = convertState(executionResult.initialState, jcExecution.method, modelConverter),
                stateAfter = convertState(executionResult.resultState, jcExecution.method, modelConverter),
                // TODO usvm-sbft: ask why `UTestExecutionSuccessResult.result` is nullable
                result = UtExecutionSuccess(executionResult.result?.let { modelConverter.convert(it) } ?: UtVoidModel),
                coverage = coverage,
                instrumentation = instrumentation,
            )
            is UTestExecutionExceptionResult -> {
                UtUsvmExecution(
                    stateBefore = convertState(executionResult.initialState, jcExecution.method, modelConverter),
                    stateAfter = convertState(executionResult.resultState, jcExecution.method, modelConverter),
                    result = createExecutionFailureResult(
                        executionResult.cause,
                        jcExecution.method,
                    ),
                    coverage = coverage,
                    instrumentation = instrumentation,
                )
            }

            is UTestExecutionInitFailedResult -> {
                logger.warn(convertException(executionResult.cause)) {
                    "Execution failed before method under test call"
                }
                null
            }

            is UTestExecutionFailedResult -> {
                logger.error(convertException(executionResult.cause)) {
                    "Concrete execution failed"
                }
                null
            }

            is UTestExecutionTimedOutResult -> {
                // TODO usvm-sbft
                null
            }
        } ?: return null

        return utUsvmExecution
    }

    private fun convertException(exceptionDescriptor: UTestExceptionDescriptor): Throwable =
        toValueConverter.buildObjectFromDescriptor(exceptionDescriptor.dropStaticFields(
            cache = mutableMapOf()
        )) as Throwable

    private fun getTrace(executionResult: UTestExecutionResult): List<JcInst>? = when (executionResult) {
        is UTestExecutionExceptionResult -> executionResult.trace
        is UTestExecutionInitFailedResult -> executionResult.trace
        is UTestExecutionSuccessResult -> executionResult.trace
        is UTestExecutionFailedResult -> emptyList()
        is UTestExecutionTimedOutResult -> emptyList()
    }

    private fun convertState(
        state: UTestExecutionState,
        method: JcTypedMethod,
        modelConverter: JcToUtModelConverter,
        ): EnvironmentModels {
        val thisInstance =
            if (method.isStatic) null
            else modelConverter.convert(state.instanceDescriptor ?: error("Unexpected null instanceDescriptor"))
        val parameters = state.argsDescriptors.map { modelConverter.convert(it ?: error("Unexpected null argDescriptor")) }
        val statics = state.statics
            .entries
            .associate { (jcField, uTestDescr) ->
                jcField.fieldId to modelConverter.convert(uTestDescr)
            }
        val executableId: ExecutableId = method.method.toExecutableId()
        return EnvironmentModels(thisInstance, parameters, statics, executableId)
    }

    private fun createExecutionFailureResult(
        exceptionDescriptor: UTestExceptionDescriptor,
        jcTypedMethod: JcTypedMethod,
    ): UtExecutionFailure {
        val exception = convertException(exceptionDescriptor)
        val fromNestedMethod = exception.stackTrace.firstOrNull()?.let { stackTraceElement ->
            stackTraceElement.className != jcTypedMethod.enclosingType.jcClass.name ||
                    stackTraceElement.methodName != jcTypedMethod.name
        } ?: false
        return if (exceptionDescriptor.raisedByUserCode) {
            UtExplicitlyThrownException(exception, fromNestedMethod)
        } else {
            UtImplicitlyThrownException(exception, fromNestedMethod)
        }
    }

    private fun convertCoverage(jcCoverage: List<JcInst>?, jcClass: JcClassOrInterface) = Coverage(
        coveredInstructions = jcCoverage.orEmpty().map {
            val methodSignature = it.enclosingMethod.jcdbSignature
            Instruction(
                internalName = it.enclosingClass.name.replace('.', '/'),
                methodSignature = methodSignature,
                lineNumber = it.lineNumber,
                id = instructionIdProvider.provideInstructionId(methodSignature, it.location.index)
            )
        },
        // TODO usvm-sbft: maybe add cache here
        // TODO usvm-sbft: make sure static initializers are included into instructions count
        //  I assume they are counted as part of `<clinit>` method
        instructionsCount = jcClass.declaredMethods.sumOf { it.instList.size.toLong() }
    )
}