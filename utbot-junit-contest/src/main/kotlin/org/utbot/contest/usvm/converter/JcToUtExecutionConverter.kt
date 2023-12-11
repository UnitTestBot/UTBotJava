package org.utbot.contest.usvm.converter

import mu.KotlinLogging
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.cfg.JcInst
import org.jacodb.api.ext.jcdbSignature
import org.usvm.instrumentation.testcase.api.UTestExecutionExceptionResult
import org.usvm.instrumentation.testcase.api.UTestExecutionFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionInitFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionState
import org.usvm.instrumentation.testcase.api.UTestExecutionSuccessResult
import org.usvm.instrumentation.testcase.api.UTestExecutionTimedOutResult
import org.usvm.instrumentation.testcase.api.UTestSetStaticFieldStatement
import org.usvm.instrumentation.testcase.descriptor.Descriptor2ValueConverter
import org.usvm.instrumentation.testcase.descriptor.UTestExceptionDescriptor
import org.usvm.instrumentation.util.enclosingClass
import org.usvm.instrumentation.util.enclosingMethod
import org.utbot.common.isPublic
import org.utbot.contest.usvm.jc.JcExecution
import org.utbot.contest.usvm.jc.UTestConcreteExecutionResult
import org.utbot.contest.usvm.jc.UTestResultWrapper
import org.utbot.contest.usvm.jc.UTestSymbolicExceptionResult
import org.utbot.contest.usvm.jc.UTestSymbolicSuccessResult
import org.utbot.framework.codegen.domain.builtin.UtilMethodProvider
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.mapper.UtModelDeepMapper
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzer.IdGenerator
import java.util.IdentityHashMap

private val logger = KotlinLogging.logger {}

class JcToUtExecutionConverter(
    private val jcExecution: JcExecution,
    private val jcClasspath: JcClasspath,
    private val idGenerator: IdGenerator<Int>,
    private val instructionIdProvider: InstructionIdProvider,
    private val utilMethodProvider: UtilMethodProvider,
) {
    private val toValueConverter = Descriptor2ValueConverter(utContext.classLoader)

    private val instToModelConverter = UTestInstToUtModelConverter(jcExecution.uTest, jcClasspath, idGenerator, utilMethodProvider)
    private var jcToUtModelConverter = JcToUtModelConverter(idGenerator, jcClasspath, instToModelConverter)
    private var uTestProcessResult = instToModelConverter.processUTest()

    fun convert() = jcExecution.uTestExecutionResultWrappers.firstNotNullOfOrNull { result ->
        runCatching { convert(result) }
            .onFailure { e ->
                logger.warn(e) {
                    "Recoverable: JcToUtExecutionConverter.convert(${jcExecution.method.method}) " +
                            "failed for ${result::class.java}"
                }
            }
            .getOrNull()
    } ?: error("Failed to construct UtExecution for all uTestExecutionResultWrappers on ${jcExecution.method.method}")

    private fun convert(uTestResultWrapper: UTestResultWrapper): UtExecution? {
        val coverage = convertCoverage(getTrace(uTestResultWrapper), jcExecution.method.enclosingType.jcClass)

        val utUsvmExecution: UtUsvmExecution = when (uTestResultWrapper) {
            is UTestSymbolicExceptionResult -> {
                UtUsvmExecution(
                    stateBefore = constructStateBeforeFromUTest(),
                    stateAfter = MissingState,
                    result = createExecutionFailureResult(
                        exceptionDescriptor = UTestExceptionDescriptor(
                            type = uTestResultWrapper.exceptionType,
                            message = "",
                            stackTrace = emptyList(),
                            raisedByUserCode = true,
                        ),
                        jcTypedMethod = jcExecution.method,
                    ),
                    coverage = coverage,
                    instrumentation = uTestProcessResult.instrumentation,
                )
            }

            is UTestSymbolicSuccessResult -> {
                uTestResultWrapper.initStatements.forEach { instToModelConverter.processInst(it) }
                instToModelConverter.processInst(uTestResultWrapper.result)

                val resultUtModel = instToModelConverter.findModelByInst(uTestResultWrapper.result)

                UtUsvmExecution(
                    stateBefore = constructStateBeforeFromUTest(),
                    stateAfter = MissingState,
                    result = UtExecutionSuccess(resultUtModel),
                    coverage = coverage,
                    instrumentation = uTestProcessResult.instrumentation,
                )
            }

            is UTestConcreteExecutionResult ->
                when (val executionResult = uTestResultWrapper.uTestExecutionResult) {
                    is UTestExecutionSuccessResult -> UtUsvmExecution(
                        stateBefore = convertState(executionResult.initialState, EnvironmentStateKind.INITIAL, jcExecution.method),
                        stateAfter = convertState(executionResult.resultState, EnvironmentStateKind.FINAL, jcExecution.method),
                        // TODO usvm-sbft: ask why `UTestExecutionSuccessResult.result` is nullable
                        result = UtExecutionSuccess(executionResult.result?.let {
                            jcToUtModelConverter.convert(it, EnvironmentStateKind.FINAL)
                        } ?: UtVoidModel),
                        coverage = coverage,
                        instrumentation = uTestProcessResult.instrumentation,
                    )

                    is UTestExecutionExceptionResult -> {
                        UtUsvmExecution(
                            stateBefore = convertState(executionResult.initialState, EnvironmentStateKind.INITIAL, jcExecution.method),
                            stateAfter = convertState(executionResult.resultState, EnvironmentStateKind.FINAL, jcExecution.method),
                            result = createExecutionFailureResult(executionResult.cause, jcExecution.method),
                            coverage = coverage,
                            instrumentation = uTestProcessResult.instrumentation,
                        )
                    }

                    is UTestExecutionInitFailedResult -> {
                        logger.warn(convertException(executionResult.cause)) {
                            "Execution failed before method under test call on ${jcExecution.method.method}"
                        }
                        null
                    }

                    is UTestExecutionFailedResult -> {
                        logger.error(convertException(executionResult.cause)) {
                            "Concrete execution failed on ${jcExecution.method.method}"
                        }
                        null
                    }

                    is UTestExecutionTimedOutResult -> {
                        logger.warn { "Timeout on ${jcExecution.method.method}" }
                        UtUsvmExecution(
                            stateBefore = constructStateBeforeFromUTest(),
                            stateAfter = MissingState,
                            result = UtTimeoutException(TimeoutException("Concrete execution timed out")),
                            coverage = coverage,
                            instrumentation = uTestProcessResult.instrumentation,
                        )
                    }
                }
        } ?: return null

        return utUsvmExecution
            .mapModels(jcToUtModelConverter.utCyclicReferenceModelResolver)
            .mapModels(constructAssemblingMapper())
            .mapModels(constructAssembleToCompositeModelMapper())
            .mapModels(constructConstArrayModelMapper())
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

    private fun constructConstArrayModelMapper(): UtModelDeepMapper = UtModelDeepMapper { model ->
        if (model is UtArrayModel) {
            val storeGroups = model.stores.entries.groupByTo(IdentityHashMap()) { it.value }
            val mostCommonStore = storeGroups.maxByOrNull { it.value.size } ?: return@UtModelDeepMapper model
            if (mostCommonStore.value.size > 1) {
                model.constModel = mostCommonStore.key
                mostCommonStore.value.forEach { (index, _) -> model.stores.remove(index) }
            }
        }
        model
    }

    private fun constructAssembleToCompositeModelMapper(): UtModelDeepMapper = UtModelDeepMapper { model ->
        if (model is UtAssembleModel
            && utilMethodProvider.createInstanceMethodId == model.instantiationCall.statement
            && model.modificationsChain.all {
                utilMethodProvider.setFieldMethodId == (it as? UtStatementCallModel)?.statement
            }
        ) {
            UtCompositeModel(
                id = model.id,
                classId = model.classId,
                isMock = false,
                fields = model.modificationsChain.associateTo(mutableMapOf()) {
                    // `setFieldMethodId` call example for reference:
                    // setField(outputStream, "java.io.ByteArrayOutputStream", "buf", buf);

                    val params = (it as UtStatementCallModel).params
                    val fieldId = FieldId(
                        declaringClass = ClassId((params[1] as UtPrimitiveModel).value as String),
                        name = ((params[2] as UtPrimitiveModel).value as String)
                    )
                    // We prefer `model.origin?.fields?.get(fieldId)` over `params[3]`, because
                    //   - `model.origin?.fields?.get(fieldId)` is created from concrete execution initial state
                    //   - `params[3]` is created from jcMachine output, which could be a bit off
                    fieldId to (model.origin?.fields?.get(fieldId) ?: params[3])
                }
            )
        } else {
            model
        }
    }

    private fun convertException(exceptionDescriptor: UTestExceptionDescriptor): Throwable =
        toValueConverter.buildObjectFromDescriptor(exceptionDescriptor.dropStaticFields(
            cache = mutableMapOf()
        )) as Throwable

    /**
     * Gets trace from execution result if it is present.
     *
     * Otherwise, (e.g. we use symbolic result if concrete fails),
     * minimization will take just 'UtSettings.maxUnknownCoverageExecutionsPerMethodPerResultType' executions.
     */
    private fun getTrace(executionResult: UTestResultWrapper): List<JcInst>? = when (executionResult) {
        is UTestConcreteExecutionResult -> when (val res = executionResult.uTestExecutionResult) {
            is UTestExecutionExceptionResult -> res.trace
            is UTestExecutionInitFailedResult -> res.trace
            is UTestExecutionSuccessResult -> res.trace
            is UTestExecutionFailedResult -> emptyList()
            is UTestExecutionTimedOutResult -> emptyList()
        }
        is UTestSymbolicExceptionResult -> emptyList()
        is UTestSymbolicSuccessResult -> emptyList()
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

    private fun constructStateBeforeFromUTest(): EnvironmentModels {
        val uTest = jcExecution.uTest
        val method = jcExecution.method
        val thisInstance =
            if (method.isStatic) null
            else if (method.method.isConstructor) null
            else instToModelConverter.findModelByInst(uTest.callMethodExpression.instance ?: error("Unexpected null instance expression"))
        val parameters = uTest.callMethodExpression.args.map {
            instToModelConverter.findModelByInst(it)
        }
        val statics = uTest.initStatements.filterIsInstance<UTestSetStaticFieldStatement>()
            .associate {
                it.field.fieldId to instToModelConverter.findModelByInst(it.value)
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