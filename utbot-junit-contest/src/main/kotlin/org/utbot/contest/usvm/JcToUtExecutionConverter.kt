package org.utbot.contest.usvm

import mu.KotlinLogging
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClasspath
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
import org.utbot.common.isPublic
import org.utbot.contest.usvm.executor.JcExecution
import org.utbot.framework.codegen.domain.builtin.UtilMethodProvider
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.mapper.UtModelDeepMapper
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzer.IdGenerator

private val logger = KotlinLogging.logger {}

class JcToUtExecutionConverter(
    private val jcExecution: JcExecution,
    private val jcClasspath: JcClasspath,
    private val idGenerator: IdGenerator<Int>,
    private val instructionIdProvider: InstructionIdProvider,
    private val utilMethodProvider: UtilMethodProvider,
) {
    private val toValueConverter = Descriptor2ValueConverter(utContext.classLoader)

    private var jcToUtModelConverter: JcToUtModelConverter

    init {
        val instToModelConverter = UTestInst2UtModelConverter(idGenerator, jcClasspath, utilMethodProvider)

        instToModelConverter.processUTest(jcExecution.uTest)
        jcToUtModelConverter = JcToUtModelConverter(idGenerator, jcClasspath, instToModelConverter)
    }

    fun convert(): UtExecution? {
        val coverage = convertCoverage(getTrace(jcExecution.uTestExecutionResult), jcExecution.method.enclosingType.jcClass)
        // TODO usvm-sbft: fill up instrumentation with data from UTest
        val instrumentation = emptyList<UtInstrumentation>()

        val utUsvmExecution: UtUsvmExecution = when (val executionResult = jcExecution.uTestExecutionResult) {
            is UTestExecutionSuccessResult -> UtUsvmExecution(
                stateBefore = convertState(executionResult.initialState, EnvironmentStateKind.INITIAL, jcExecution.method),
                stateAfter = convertState(executionResult.resultState, EnvironmentStateKind.FINAL, jcExecution.method),
                // TODO usvm-sbft: ask why `UTestExecutionSuccessResult.result` is nullable
                result = UtExecutionSuccess(executionResult.result?.let {
                    jcToUtModelConverter.convert(it, EnvironmentStateKind.FINAL)
                } ?: UtVoidModel),
                coverage = coverage,
                instrumentation = instrumentation,
            )
            is UTestExecutionExceptionResult -> {
                UtUsvmExecution(
                    stateBefore = convertState(executionResult.initialState, EnvironmentStateKind.INITIAL, jcExecution.method),
                    stateAfter = convertState(executionResult.resultState, EnvironmentStateKind.FINAL, jcExecution.method),
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

        return utUsvmExecution.mapModels(constructAssemblingMapper())
    }

    private fun constructAssemblingMapper(): UtModelDeepMapper = UtModelDeepMapper { model ->
        // TODO usvm-sbft: support constructors with parameters here if it is really required
        // Unfortunately, it is not possible to use [AssembleModelGeneral] as it requires soot being initialized.
        if (model !is UtAssembleModel
            || utilMethodProvider.createInstanceMethodId != model.instantiationCall.statement
            || model.modificationsChain.isNotEmpty()) {
            return@UtModelDeepMapper model
        }

        val instantiatingClassName = (model
            .instantiationCall
            .params
            .single() as UtPrimitiveModel).value.toString()

        val defaultConstructor = ClassId(instantiatingClassName)
            .jClass
            .constructors
            .firstOrNull { it.isPublic && it.parameters.isEmpty() }


        defaultConstructor?.let { ctor ->
            UtAssembleModel(
                id = idGenerator.createId(),
                classId = model.classId,
                modelName = "",
                instantiationCall = UtExecutableCallModel(
                    instance = null,
                    executable = ctor.executableId,
                    params = emptyList(),
                )
            )
        } ?: model
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
        stateKind: EnvironmentStateKind,
        method: JcTypedMethod,
    ): EnvironmentModels {
        val thisInstance =
            if (method.isStatic) null
            else if (method.method.isConstructor) null
            else jcToUtModelConverter.convert(state.instanceDescriptor ?: error("Unexpected null instanceDescriptor"), stateKind)
        val parameters = state.argsDescriptors.map {
            jcToUtModelConverter.convert(it ?: error("Unexpected null argDescriptor"), stateKind)
        }
        val statics = state.statics
            .entries
            .associate { (jcField, uTestDescr) ->
                jcField.fieldId to jcToUtModelConverter.convert(uTestDescr, stateKind)
            }
        val executableId: ExecutableId = method.method.toExecutableId(jcClasspath)
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