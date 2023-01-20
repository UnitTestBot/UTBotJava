package org.utbot.instrumentation.instrumentation.execution

import java.security.ProtectionDomain
import java.util.IdentityHashMap
import kotlin.reflect.jvm.javaMethod
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
import org.utbot.instrumentation.instrumentation.execution.constructors.ConstructOnlyUserClassesOrCachedObjectsStrategy
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.instrumentation.instrumentation.execution.mock.InstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.phases.PhasesController
import org.utbot.instrumentation.instrumentation.execution.phases.start
import org.utbot.framework.plugin.api.util.singleExecutableId
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor

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

object UtExecutionInstrumentation : Instrumentation<UtConcreteExecutionResult> {
    private val delegateInstrumentation = InvokeInstrumentation()

    private val instrumentationContext = InstrumentationContext()

    private val traceHandler = TraceHandler()
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
            delegateInstrumentation
        ).computeConcreteExecutionResult {
            var currentlyElapsed = 0L
            val (params, statics, cache) = valueConstructionPhase.start {
                val (result, elapsed) = invokeWithTimeoutWithUtContext(timeout) {
                    val params = constructParameters(stateBefore)
                    val statics = constructStatics(stateBefore)

                    mock(instrumentations)

                    Triple(params, statics, getCache())
                }
                currentlyElapsed += elapsed
                result
            }

            // preparation
            val savedStatics = preparationContext.start {
                val savedStatics = setStaticFields(statics)
                resetTrace()
                savedStatics
            }

            try {
                // invocation
                val concreteResult = invocationContext.start {
                    val (result, elapsed) = invokeWithTimeoutWithUtContext(timeout - currentlyElapsed) {
                        invoke(clazz, methodSignature, params.map { it.value })
                    }
                    currentlyElapsed += elapsed
                    result
                }

                // statistics collection
                val coverage = statisticsCollectionContext.start {
                    val (result, elapsed) = invokeWithTimeoutWithUtContext(timeout - currentlyElapsed) {
                        getCoverage(clazz)
                    }
                    currentlyElapsed += elapsed
                    result
                }

                // model construction
                val (executionResult, stateAfter) = modelConstructionContext.start {
                    val (result, elapsed) = invokeWithTimeoutWithUtContext(timeout - currentlyElapsed) {
                        configureConstructor {
                            this.cache = cache
                            strategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(
                                UtExecutionInstrumentation.pathsToUserClasses,
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
                    currentlyElapsed += elapsed
                    result
                }

                UtConcreteExecutionResult(
                    stateAfter,
                    executionResult,
                    coverage
                )
            } finally {
                // postprocessing
                postprocessingContext.start {
                    resetStaticFields(savedStatics)
                }
            }
        }
    }

    override fun getStaticField(fieldId: FieldId): Result<UtModel> =
        delegateInstrumentation.getStaticField(fieldId).map { value ->
            val cache = IdentityHashMap<Any, UtModel>()
            val strategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(
                pathsToUserClasses, cache
            )
            UtModelConstructor(cache, strategy).run {
                construct(value, fieldId.type)
            }
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
