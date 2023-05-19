package org.utbot.instrumentation.instrumentation.execution

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
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
import org.utbot.instrumentation.instrumentation.execution.phases.PhasesController
import org.utbot.instrumentation.instrumentation.execution.phases.start
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor
import java.security.ProtectionDomain
import java.util.*
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

class UtConcreteExecutionResult(
    val stateAfter: EnvironmentModels,
    val result: UtExecutionResult,
    val coverage: Coverage
) {
    override fun toString(): String = buildString {
        appendLine("UtConcreteExecutionResult(")
        appendLine("stateAfter=$stateAfter")
        appendLine("result=$result")
        appendLine("coverage=$coverage)")
    }
}

private val logger = getLogger<UtExecutionInstrumentation>()

// TODO if possible make it non singleton
object UtExecutionInstrumentation : Instrumentation<UtConcreteExecutionResult> {
    private val delegateInstrumentation = InvokeInstrumentation()

    var instrumentationContext = InstrumentationContext()

    private val traceHandler = TraceHandler()
    private val pathsToUserClasses = mutableSetOf<String>()

    override fun init(pathsToUserClasses: Set<String>) {
        super.init(pathsToUserClasses)
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
    ): UtConcreteExecutionResult {
        if (parameters !is UtConcreteExecutionData) {
            throw IllegalArgumentException("Argument parameters must be of type UtConcreteExecutionData, but was: ${parameters?.javaClass}")
        }
        val (stateBefore, instrumentations, timeout) = parameters // smart cast to UtConcreteExecutionData

        val methodId = clazz.singleExecutableId(methodSignature)
        val returnClassId = methodId.returnType

        return PhasesController(
            instrumentationContext,
            traceHandler,
            delegateInstrumentation,
            timeout
        ).computeConcreteExecutionResult {
            try {
                val (params, statics, cache) = this.executePhaseInTimeout(valueConstructionPhase) {
                    val params = constructParameters(stateBefore)
                    val statics = constructStatics(stateBefore)

                    // here static methods and instances are mocked
                    mock(instrumentations)

                    Triple(params, statics, getCache())
                }

                // invariants:
                // 1. phase must always complete if started as static reset relies on it
                // 2. phase must be fast as there are no incremental changes
                postprocessingPhase.setStaticFields(preparationPhase.start {
                    val result = setStaticFields(statics)
                    resetTrace()
                    result
                })

                // invocation
                val concreteResult = executePhaseInTimeout(invocationPhase) {
                    invoke(clazz, methodSignature, params.map { it.value })
                }

                // statistics collection
                val coverage = executePhaseInTimeout(statisticsCollectionPhase) {
                    getCoverage(clazz)
                }

                // model construction
                val (executionResult, stateAfter) = executePhaseInTimeout(modelConstructionPhase) {
                    configureConstructor {
                        this.cache = cache
                        strategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(
                            pathsToUserClasses,
                            cache
                        )
                    }

                    val executionResult = convertToExecutionResult(concreteResult, returnClassId)

                    val stateAfterParametersWithThis = constructParameters(params)
                    val stateAfterStatics = constructStatics(stateBefore, statics)
                    val (stateAfterThis, stateAfterParameters) = if (stateBefore.thisInstance == null) {
                        null to stateAfterParametersWithThis
                    } else {
                        stateAfterParametersWithThis.first() to stateAfterParametersWithThis.drop(1)
                    }
                    val stateAfter = EnvironmentModels(stateAfterThis, stateAfterParameters, stateAfterStatics)

                    executionResult to stateAfter
                }

                UtConcreteExecutionResult(
                    stateAfter,
                    executionResult,
                    coverage
                )
            } finally {
                postprocessingPhase.start {
                    resetStaticFields()
                    valueConstructionPhase.resetMockMethods()
                }
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
