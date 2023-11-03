package org.utbot.contest.usvm

import mu.KotlinLogging
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.cfg.JcInst
import org.jacodb.api.cfg.JcThrowInst
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
import org.utbot.framework.plugin.api.mapper.UtModelDeepMapper
import org.utbot.framework.plugin.api.mapper.mapStateBeforeModels
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.utContext

private val logger = KotlinLogging.logger {}

class JcToUtExecutionConverter(
    private val instructionIdProvider: InstructionIdProvider,
) {
    //TODO: obtain in somehow from [JcExecution] or somewhere else
    private val testClassId: ClassId = objectClassId
    private val utilMethodProvider = TestClassUtilMethodProvider(testClassId)

    private val instConverter = UTestInst2UtModelConverter(utilMethodProvider)
    private val toValueConverter = Descriptor2ValueConverter(utContext.classLoader)

    fun convert(jcExecution: JcExecution): UtExecution? {
        val instToModelCache = instConverter.processUTest(jcExecution.uTest)
        val modelConverter = JcToUtModelConverter(instToModelCache)

        val coverage = convertCoverage(getTrace(jcExecution.uTestExecutionResult), jcExecution.method.enclosingType.jcClass)
        // TODO usvm-sbft: fill up instrumentation with data from UTest
        val instrumentation = emptyList<UtInstrumentation>()

        val utUsvmExecution: UtUsvmExecution = when (val executionResult = jcExecution.uTestExecutionResult) {
            is UTestExecutionSuccessResult -> UtUsvmExecution(
                stateBefore = convertState(executionResult.initialState, jcExecution.method, modelConverter),
                stateAfter = convertState(executionResult.resultState, jcExecution.method, modelConverter),
                result = UtExecutionSuccess(modelConverter.convert(executionResult.result)),
                coverage = coverage,
                instrumentation = instrumentation,
            )

            is UTestExecutionExceptionResult -> toUserRaisedException(executionResult.cause)?.let { exception ->
                UtUsvmExecution(
                    stateBefore = convertState(executionResult.initialState, jcExecution.method, modelConverter),
                    stateAfter = convertState(executionResult.resultState, jcExecution.method, modelConverter),
                    result = createExecutionFailureResult(
                        exception,
                        jcExecution.method,
                        executionResult.trace
                    ),
                    coverage = coverage,
                    instrumentation = instrumentation,
                )
            }

            is UTestExecutionInitFailedResult -> {
                toUserRaisedException(executionResult.cause)?.let { e ->
                    logger.warn(e) { "Execution failed before method under test call" }
                }
                null
            }

            is UTestExecutionFailedResult -> {
                toUserRaisedException(executionResult.cause)?.let { e ->
                    // TODO usvm-sbft
                    null
                }
            }

            is UTestExecutionTimedOutResult -> {
                // TODO usvm-sbft
                null
            }
        } ?: return null

        return utUsvmExecution.mapModels(UtModelDeepMapper {
            instToModelCache[
                toValueConverter.cache.getValue(
                    utModelConstructor.cache.getValue(it)
                ).origin
            ] ?: it
        })
        // TODO usvm-sbft: deep map UtExecution to substitute models build from UTest
    }

    private fun toUserRaisedException(exceptionDescriptor: UTestExceptionDescriptor): Throwable? {
        val exception =
            toValueConverter.buildObjectFromDescriptor(exceptionDescriptor) as Throwable
        return if (exceptionDescriptor.raisedByUserCode) exception
        else {
            logger.error(exception) { "Concrete execution failed" }
            null
        }
    }

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
            else modelConverter.convert(state.instanceDescriptor)
        val parameters = state.argsDescriptors.map { modelConverter.convert(it) }
        val statics = state.statics
            .entries
            .associate { (jcField, uTestDescr) ->
                jcField.fieldId to modelConverter.convert(uTestDescr)
            }
        val executableId: ExecutableId = method.method.toExecutableId()
        return EnvironmentModels(thisInstance, parameters, statics, executableId)
    }

    private fun createExecutionFailureResult(
        exception: Throwable,
        jcTypedMethod: JcTypedMethod,
        jcCoverage: List<JcInst>?
    ): UtExecutionFailure {
        // TODO usvm-sbft: test that exceptions are correctly classified
        val lastJcInst = jcCoverage.orEmpty().lastOrNull()
            ?: return UtImplicitlyThrownException(exception, fromNestedMethod = false)
        val fromNestedMethod = lastJcInst.enclosingMethod != jcTypedMethod.method
        return if (lastJcInst is JcThrowInst)
            UtExplicitlyThrownException(exception, fromNestedMethod)
        else
            UtImplicitlyThrownException(exception, fromNestedMethod)
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