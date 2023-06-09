package org.utbot.instrumentation.instrumentation.execution

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.singleExecutableId
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.execution.constructors.ConstructOnlyUserClassesOrCachedObjectsStrategy
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.instrumentation.instrumentation.execution.mock.InstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.ndd.NonDeterministicClassVisitor
import org.utbot.instrumentation.instrumentation.execution.ndd.NonDeterministicDetector
import org.utbot.instrumentation.instrumentation.execution.phases.ConstructedData
import org.utbot.instrumentation.instrumentation.execution.phases.PhasesController
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor
import java.security.ProtectionDomain
import kotlin.reflect.jvm.javaMethod

/**
 * Consists of the data needed to execute the method concretely. Also includes method arguments stored in models.
 *
 * @property [stateBefore] is necessary for construction of parameters of a concrete call.
 * @property [instrumentation] is necessary for mocking static methods and new instances.
 * @property [timeout] is timeout for specific concrete execution (in milliseconds).
 * By default is initialized from [UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis]
 */
data class UtConcreteExecutionData(
    val stateBefore: EnvironmentModels,
    val instrumentation: List<UtInstrumentation>,
    val timeout: Long
)

data class UtConcreteExecutionResult(
    val stateAfter: EnvironmentModels,
    val result: UtExecutionResult,
    val coverage: Coverage,
    val newInstrumentation: List<UtInstrumentation>? = null,
) {
    override fun toString(): String = buildString {
        appendLine("UtConcreteExecutionResult(")
        appendLine("stateAfter=$stateAfter")
        appendLine("result=$result")
        appendLine("coverage=$coverage)")
    }
}

// TODO if possible make it non singleton
object UtExecutionInstrumentation : Instrumentation<UtConcreteExecutionResult> {
    private val delegateInstrumentation = InvokeInstrumentation()

    var instrumentationContext = InstrumentationContext()

    private val traceHandler = TraceHandler()
    private val ndDetector = NonDeterministicDetector()
    private val pathsToUserClasses = mutableSetOf<String>()

    override fun init(pathsToUserClasses: Set<String>) {
        UtExecutionInstrumentation.pathsToUserClasses.clear()
        UtExecutionInstrumentation.pathsToUserClasses += pathsToUserClasses
    }

    /**
     * Ignores [arguments], because concrete arguments will be constructed
     * from models passed via [parameters].
     *
     * Argument [parameters] must be of type [UtConcreteExecutionData].
     */
    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): UtConcreteExecutionResult =
        invoke(clazz, methodSignature, arguments, parameters, additionalPhases = { it })

    fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?,
        additionalPhases: PhasesController.(UtConcreteExecutionResult) -> UtConcreteExecutionResult
    ): UtConcreteExecutionResult {
        if (parameters !is UtConcreteExecutionData) {
            throw IllegalArgumentException("Argument parameters must be of type UtConcreteExecutionData, but was: ${parameters?.javaClass}")
        }
        val (stateBefore, instrumentations, timeout) = parameters // smart cast to UtConcreteExecutionData

        return PhasesController(
            instrumentationContext,
            traceHandler,
            delegateInstrumentation,
            timeout
        ).computeConcreteExecutionResult {
            try {
                // some preparation actions for concrete execution
                var constructedData: ConstructedData
                try {
                    constructedData = applyPreprocessing(parameters)
                } catch (t: Throwable) {
                    return UtConcreteExecutionResult(MissingState, UtConcreteExecutionProcessedFailure(t), Coverage())
                }

                val (params, statics, cache) = constructedData

                // invocation
                val concreteResult = executePhaseInTimeout(invocationPhase) {
                    invoke(clazz, methodSignature, params.map { it.value })
                }

                // statistics collection
                val (coverage, ndResults) = executePhaseInTimeout(statisticsCollectionPhase) {
                    getCoverage(clazz) to getNonDeterministicResults()
                }

                // model construction
                val (executionResult, stateAfter, newInstrumentation) = executePhaseInTimeout(modelConstructionPhase) {
                    configureConstructor {
                        this.cache = cache
                        strategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(
                            pathsToUserClasses,
                            cache
                        )
                    }

                    val ndStatics = constructStaticInstrumentation(ndResults.statics)
                    val ndNews = constructNewInstrumentation(ndResults.news, ndResults.calls)
                    val newInstrumentation = mergeInstrumentations(instrumentations, ndStatics, ndNews)

                    val returnType = clazz.singleExecutableId(methodSignature).returnType
                    val executionResult = convertToExecutionResult(concreteResult,returnType)

                    val stateAfterParametersWithThis = constructParameters(params)
                    val stateAfterStatics = constructStatics(stateBefore, statics)
                    val (stateAfterThis, stateAfterParameters) = if (stateBefore.thisInstance == null) {
                        null to stateAfterParametersWithThis
                    } else {
                        stateAfterParametersWithThis.first() to stateAfterParametersWithThis.drop(1)
                    }
                    val stateAfter = EnvironmentModels(stateAfterThis, stateAfterParameters, stateAfterStatics)

                    Triple(executionResult, stateAfter, newInstrumentation)
                }

                additionalPhases(UtConcreteExecutionResult(
                    stateAfter,
                    executionResult,
                    coverage,
                    newInstrumentation
                ))
            } finally {
                // restoring data after concrete execution
                applyPostprocessing()
            }
        }
    }

    override fun getStaticField(fieldId: FieldId): Result<UtModel> =
        delegateInstrumentation.getStaticField(fieldId).map { value ->
            UtModelConstructor.createOnlyUserClassesConstructor(pathsToUserClasses)
                .construct(value, fieldId.type)
        }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray {
        val instrumenter = Instrumenter(classfileBuffer, loader)

        traceHandler.registerClass(className)
        instrumenter.visitInstructions(traceHandler.computeInstructionVisitor(className))

        instrumenter.visitClass { writer ->
            NonDeterministicClassVisitor(writer, ndDetector)
        }

        val mockClassVisitor = instrumenter.visitClass { writer ->
            MockClassVisitor(
                writer,
                InstrumentationContext.MockGetter::getMock.javaMethod!!,
                InstrumentationContext.MockGetter::checkCallSite.javaMethod!!,
                InstrumentationContext.MockGetter::hasMock.javaMethod!!
            )
        }

        mockClassVisitor.signatureToId.forEach { (method, id) ->
            instrumentationContext.methodSignatureToId += method to id
        }

        return instrumenter.classByteCode
    }
}
