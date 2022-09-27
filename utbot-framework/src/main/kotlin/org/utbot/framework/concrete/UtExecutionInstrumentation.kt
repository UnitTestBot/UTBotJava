package org.utbot.framework.concrete

import org.objectweb.asm.Type
import org.utbot.common.StopWatch
import org.utbot.common.ThreadBasedExecutor
import org.utbot.common.withAccessibility
import org.utbot.framework.UtSettings
import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtConcreteExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.singleExecutableId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.api.withReflection
import org.utbot.framework.util.isInaccessibleViaReflection
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.et.EtInstruction
import org.utbot.instrumentation.instrumentation.et.ExplicitThrowInstruction
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor
import java.security.AccessControlException
import java.security.ProtectionDomain
import java.util.IdentityHashMap
import kotlin.reflect.jvm.javaMethod

/**
 * Consists of the data needed to execute the method concretely. Also includes method arguments stored in models.
 *
 * @property [stateBefore] is necessary for construction of parameters of a concrete call.
 * @property [instrumentation] is necessary for mocking static methods and new instances.
 * @property [timeout] is timeout for specific concrete execution (in milliseconds).
 * By default is initialized from [UtSettings.concreteExecutionTimeoutInChildProcess]
 */
data class UtConcreteExecutionData(
    val stateBefore: EnvironmentModels,
    val instrumentation: List<UtInstrumentation>,
    val timeout: Long = UtSettings.concreteExecutionTimeoutInChildProcess
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
    fun convertToAssemble(
        packageName: String
    ): UtConcreteExecutionResult {
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
        withReflection {
        if (parameters !is UtConcreteExecutionData) {
            throw IllegalArgumentException("Argument parameters must be of type UtConcreteExecutionData, but was: ${parameters?.javaClass}")
        }
        val (stateBefore, instrumentations, timeout) = parameters // smart cast to UtConcreteExecutionData
        val parametersModels = listOfNotNull(stateBefore.thisInstance) + stateBefore.parameters

        val methodId = clazz.singleExecutableId(methodSignature)
        val returnClassId = methodId.returnType
        traceHandler.resetTrace()

        return MockValueConstructor(instrumentationContext).use { constructor ->
            val params = try {
                constructor.constructMethodParameters(parametersModels)
            } catch (e: Throwable) {
                if (e.cause is AccessControlException) {
                    return@use UtConcreteExecutionResult(
                        MissingState,
                        UtSandboxFailure(e.cause!!),
                        Coverage()
                    )
                }

                throw e
            }
            val staticFields = constructor
                .constructStatics(
                    stateBefore
                        .statics
                        .filterKeys { !it.isInaccessibleViaReflection }
                )
                .mapValues { (_, value) -> value.value }

            val concreteExecutionResult = withStaticFields(staticFields) {
                val staticMethodsInstrumentation = instrumentations.filterIsInstance<UtStaticMethodInstrumentation>()
                constructor.mockStaticMethods(staticMethodsInstrumentation)
                val newInstanceInstrumentation = instrumentations.filterIsInstance<UtNewInstanceInstrumentation>()
                constructor.mockNewInstances(newInstanceInstrumentation)

                traceHandler.resetTrace()
                val stopWatch = StopWatch()
                val context = UtContext(utContext.classLoader, stopWatch)
                val concreteResult = ThreadBasedExecutor.threadLocal.invokeWithTimeout(timeout, stopWatch) {
                    withUtContext(context) {
                        delegateInstrumentation.invoke(clazz, methodSignature, params.map { it.value })
                    }
                }?.getOrThrow() as? Result<*> ?: Result.failure<Any?>(TimeoutException("Timeout $timeout elapsed"))
                val traceList = traceHandler.computeInstructionList()

                val cache = constructor.objectToModelCache
                val utCompositeModelStrategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(pathsToUserClasses, cache)
                val utModelConstructor = UtModelConstructor(cache, utCompositeModelStrategy)
                utModelConstructor.run {
                    val concreteUtModelResult = concreteResult.fold({
                        UtExecutionSuccess(construct(it, returnClassId))
                    }) {
                        sortOutException(it)
                    }

                    val stateAfterParametersWithThis = params.map { construct(it.value, it.clazz.id) }
                    val stateAfterStatics = (staticFields.keys/* + traceHandler.computePutStatics()*/)
                        .associateWith { fieldId ->
                            fieldId.jField.run {
                                val computedValue = withAccessibility { get(null) }
                                val knownModel = stateBefore.statics[fieldId]
                                val knownValue = staticFields[fieldId]
                                if (knownModel != null && knownValue != null && knownValue == computedValue) {
                                    knownModel
                                } else {
                                    construct(computedValue, fieldId.type)
                                }
                            }
                        }
                    val (stateAfterThis, stateAfterParameters) = if (stateBefore.thisInstance == null) {
                        null to stateAfterParametersWithThis
                    } else {
                        stateAfterParametersWithThis.first() to stateAfterParametersWithThis.drop(1)
                    }
                    val stateAfter = EnvironmentModels(stateAfterThis, stateAfterParameters, stateAfterStatics)
                    UtConcreteExecutionResult(
                        stateAfter,
                        concreteUtModelResult,
                        traceList.toApiCoverage(
                            traceHandler.processingStorage.getInstructionsCount(
                                Type.getInternalName(clazz)
                            )
                        )
                    )
                }
            }

            concreteExecutionResult
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

    private fun sortOutException(exception: Throwable): UtExecutionFailure {
        if (exception is TimeoutException) {
            return UtTimeoutException(exception)
        }
        if (exception is AccessControlException ||
            exception is ExceptionInInitializerError && exception.exception is AccessControlException) {
            return UtSandboxFailure(exception)
        }
        // there also can be other cases, when we need to wrap internal exception... I suggest adding them on demand

        val instrs = traceHandler.computeInstructionList()
        val isNested = if (instrs.isEmpty()) {
            false
        } else {
            instrs.first().callId != instrs.last().callId
        }
        return if (instrs.isNotEmpty() && instrs.last().instructionData is ExplicitThrowInstruction) {
            UtExplicitlyThrownException(exception, isNested)
        } else {
            UtImplicitlyThrownException(exception, isNested)
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

    private fun <T> withStaticFields(staticFields: Map<FieldId, Any?>, block: () -> T): T {
        val savedFields = mutableMapOf<FieldId, Any?>()
        try {
            staticFields.forEach { (fieldId, value) ->
                fieldId.jField.run {
                    withAccessibility {
                        savedFields[fieldId] = get(null)
                        set(null, value)
                    }
                }
            }
            return block()
        } finally {
            savedFields.forEach { (fieldId, value) ->
                fieldId.jField.run {
                    withAccessibility {
                        set(null, value)
                    }
                }
            }
        }
    }
}

/**
 * Transforms a list of internal [EtInstruction]s to a list of api [Instruction]s.
 */
private fun List<EtInstruction>.toApiCoverage(instructionsCount: Long? = null): Coverage =
    Coverage(
        map { Instruction(it.className, it.methodSignature, it.line, it.id) },
        instructionsCount
    )
