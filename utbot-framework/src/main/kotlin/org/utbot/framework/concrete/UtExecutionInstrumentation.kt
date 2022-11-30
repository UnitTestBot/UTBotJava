package org.utbot.framework.concrete

import java.security.ProtectionDomain
import java.util.IdentityHashMap
import kotlin.reflect.jvm.javaMethod
import org.utbot.framework.UtSettings
import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.concrete.constructors.ConstructOnlyUserClassesOrCachedObjectsStrategy
import org.utbot.framework.concrete.constructors.UtModelConstructor
import org.utbot.framework.concrete.mock.InstrumentationContext
import org.utbot.framework.concrete.phases.PhasesController
import org.utbot.framework.concrete.phases.start
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtModel
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
 * By default is initialized from [UtSettings.concreteExecutionTimeoutInInstrumentedProcess]
 */
data class UtConcreteExecutionData(
    val stateBefore: EnvironmentModels,
    val instrumentation: List<UtInstrumentation>,
    val timeout: Long = UtSettings.concreteExecutionTimeoutInInstrumentedProcess
)

class UtConcreteExecutionResult(
    val stateAfter: EnvironmentModels,
    val result: UtExecutionResult,
    val coverage: Coverage
) {
    private fun collectAllModels(): List<UtModel> {
        val allModels = listOfNotNull(stateAfter.thisInstance).toMutableList()
        allModels += stateAfter.parameters
        allModels += stateAfter.statics.values
        allModels += listOfNotNull((result as? UtExecutionSuccess)?.model)
        return allModels
    }

    private fun updateWithAssembleModels(
        assembledUtModels: IdentityHashMap<UtModel, UtModel>
    ): UtConcreteExecutionResult {
        val toAssemble: (UtModel) -> UtModel = { assembledUtModels.getOrDefault(it, it) }

        val resolvedStateAfter = EnvironmentModels(
            stateAfter.thisInstance?.let { toAssemble(it) },
            stateAfter.parameters.map { toAssemble(it) },
            stateAfter.statics.mapValues { toAssemble(it.value) }
        )
        val resolvedResult =
            (result as? UtExecutionSuccess)?.model?.let { UtExecutionSuccess(toAssemble(it)) } ?: result

        return UtConcreteExecutionResult(
            resolvedStateAfter,
            resolvedResult,
            coverage
        )
    }

    /**
     * Tries to convert all models from [UtExecutionResult] to [UtAssembleModel] if possible.
     *
     * @return [UtConcreteExecutionResult] with converted models.
     */
    fun convertToAssemble(packageName: String): UtConcreteExecutionResult {
        val allModels = collectAllModels()

        val modelsToAssembleModels = AssembleModelGenerator(packageName).createAssembleModels(allModels)
        return updateWithAssembleModels(modelsToAssembleModels)
    }

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
        this.pathsToUserClasses.clear()
        this.pathsToUserClasses += pathsToUserClasses
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
            // construction
            val (params, statics, cache) = valueConstructionContext.start {
                val params = constructParameters(stateBefore)
                val statics = constructStatics(stateBefore)

                mock(instrumentations)

                Triple(params, statics, getCache())
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
                    invoke(clazz, methodSignature, params.map { it.value }, timeout)
                }

                // statistics collection
                val coverage = statisticsCollectionContext.start {
                    getCoverage(clazz)
                }

                // model construction
                val (executionResult, stateAfter) = modelConstructionContext.start {
                    configureConstructor {
                        this.cache = cache
                        strategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(pathsToUserClasses, cache)
                    }

                    val executionResult = convertToExecutionResult(concreteResult, returnClassId)

                    val stateAfterParametersWithThis = constructParameters(params)
                    val stateAfterStatics = constructStatics(statics.keys/* + traceHandler.computePutStatics()*/)
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
