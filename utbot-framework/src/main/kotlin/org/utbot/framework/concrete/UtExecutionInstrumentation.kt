package org.utbot.framework.concrete

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.utbot.common.StopWatch
import org.utbot.common.ThreadBasedExecutor
import org.utbot.common.withAccessibility
import org.utbot.common.withRemovedFinalModifier
import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.field
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.singleExecutableId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.et.EtInstruction
import org.utbot.instrumentation.instrumentation.et.ExplicitThrowInstruction
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor
import java.security.ProtectionDomain
import java.util.IdentityHashMap
import kotlin.reflect.jvm.javaMethod

object UtConcreteExecutionResultSerializer : KSerializer<UtConcreteExecutionResult> {
    override fun deserialize(decoder: Decoder): UtConcreteExecutionResult {
        return decoder.decodeStructure(descriptor) {
            var stateAfter: EnvironmentModels? = null
            var result: UtExecutionResult? = null
            var coverage: Coverage? = null

            while (true) {
                val index = decodeElementIndex(descriptor)

                when (index) {
                    0 -> stateAfter = decodeSerializableElement(descriptor, 0, serializer<EnvironmentModels>())
                    1 -> result = decodeSerializableElement(descriptor, 1, serializer<UtExecutionResult>())
                    2 -> coverage = decodeSerializableElement(descriptor, 2, serializer<Coverage>())
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unknown index: $index")
                }
            }

            return@decodeStructure UtConcreteExecutionResult(stateAfter!!, result!!, coverage!!)
        }
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UtConcreteExecutionResult") {
        element<EnvironmentModels>("stateAfter")
        element<UtExecutionResult>("result")
        element<Coverage>("coverage")
    }

    override fun serialize(encoder: Encoder, value: UtConcreteExecutionResult) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, serializer(), value.stateAfter)
            encodeSerializableElement(descriptor, 1, serializer(), value.result)
            encodeSerializableElement(descriptor, 2, serializer(), value.coverage)
        }
    }
}

@Serializable(with = UtConcreteExecutionResultSerializer::class)
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
        methodUnderTest: UtMethod<*>
    ): UtConcreteExecutionResult {
        val allModels = collectAllModels()

        val modelsToAssembleModels = AssembleModelGenerator(methodUnderTest).createAssembleModels(allModels)
        return updateWithAssembleModels(modelsToAssembleModels)
    }
}

object UtExecutionInstrumentationSerializer : KSerializer<UtExecutionInstrumentation> {
    @InternalSerializationApi
    @ExperimentalSerializationApi
    override fun deserialize(decoder: Decoder): UtExecutionInstrumentation {
        decoder.beginStructure(descriptor).run { endStructure(descriptor) }
        return UtExecutionInstrumentation
    }

    @InternalSerializationApi
    @ExperimentalSerializationApi
    override val descriptor: SerialDescriptor = buildSerialDescriptor("UtExecutionInstrumentation", StructureKind.OBJECT)

    @InternalSerializationApi
    @ExperimentalSerializationApi
    override fun serialize(encoder: Encoder, value: UtExecutionInstrumentation) {
        encoder.beginStructure(descriptor).run { endStructure(descriptor) }
    }
}

@Serializable(with = UtExecutionInstrumentationSerializer::class)
object UtExecutionInstrumentation : Instrumentation<UtConcreteExecutionResult> {
    private val delegateInstrumentation = InvokeInstrumentation()

    private val instrumentationContext = InstrumentationContext()

    private val traceHandler = TraceHandler()
    private val pathsToUserClasses = mutableSetOf<String>()

    init {

    }

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
            val (stateBefore, instrumentations: List<UtInstrumentation>, timeout) = parameters // smart cast to UtConcreteExecutionData
            val parametersModels = listOfNotNull(stateBefore.thisInstance) + stateBefore.parameters

            val methodId = clazz.singleExecutableId(methodSignature)
            val returnClassId = methodId.returnType
            traceHandler.resetTrace()

            return MockValueConstructor(instrumentationContext).use { constructor ->
                val params = constructor.constructMethodParameters(parametersModels)
                val staticFields = constructor
                    .constructStatics(
                        stateBefore
                            .statics
                            .filterKeys { !it.isInaccessibleViaReflection }
                    )
                    .mapValues { (_, value) -> value.value }

                val concreteExecutionResult = withStaticFields(staticFields) {
                    val staticMethodsInstrumentation =
                        instrumentations.filterIsInstance<UtStaticMethodInstrumentation>()
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
                    val utCompositeModelStrategy =
                        ConstructOnlyUserClassesOrCachedObjectsStrategy(pathsToUserClasses, cache)
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
                                fieldId.field.run { construct(withAccessibility { get(null) }, fieldId.type) }
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
                            traceList.toApiCoverage()
                        )
                    }
                }

                concreteExecutionResult
            }
        }
    }

    private val inaccessibleViaReflectionFields = setOf(
        "security" to "java.lang.System",
        "fieldFilterMap" to "sun.reflect.Reflection",
        "methodFilterMap" to "sun.reflect.Reflection"
    )

    private val FieldId.isInaccessibleViaReflection: Boolean
        get() = (name to declaringClass.name) in inaccessibleViaReflectionFields

    private fun sortOutException(exception: Throwable): UtExecutionFailure {
        if (exception is TimeoutException) {
            return UtTimeoutException(exception)
        }
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
                fieldId.field.run {
                    withRemovedFinalModifier {
                        savedFields[fieldId] = get(null)
                        set(null, value)
                    }
                }
            }
            return block()
        } finally {
            savedFields.forEach { (fieldId, value) ->
                fieldId.field.run {
                    withRemovedFinalModifier {
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
private fun List<EtInstruction>.toApiCoverage(): Coverage =
    Coverage(
        map { Instruction(it.className, it.methodSignature, it.line, it.id) }
    )
