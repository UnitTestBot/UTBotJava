package org.utbot.framework.concrete

import org.objectweb.asm.Type
import org.utbot.common.StopWatch
import org.utbot.common.ThreadBasedExecutor
import org.utbot.common.withAccessibility
import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.util.isInaccessibleViaReflection
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.et.ExplicitThrowInstruction
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor
import java.security.AccessControlException
import java.security.ProtectionDomain
import java.util.*
import kotlin.reflect.jvm.javaMethod

class UtFuzzingConcreteExecutionResult(
    val result: UtExecutionResult,
    val coverage: Coverage
) {}


object UtFuzzingExecutionInstrumentation : Instrumentation<UtFuzzingConcreteExecutionResult> {
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
    ): UtFuzzingConcreteExecutionResult {

        if (parameters !is UtConcreteExecutionData) {
            throw IllegalArgumentException("Argument parameters must be of type UtConcreteExecutionData, but was: ${parameters?.javaClass}")
        }
        val (stateBefore, instrumentations, timeout) = parameters // smart cast to UtConcreteExecutionData
        val parametersModels = listOfNotNull(stateBefore.thisInstance) + stateBefore.parameters
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
                val staticMethodsInstrumentation = instrumentations.filterIsInstance<UtStaticMethodInstrumentation>()
                constructor.mockStaticMethods(staticMethodsInstrumentation)
                val newInstanceInstrumentation = instrumentations.filterIsInstance<UtNewInstanceInstrumentation>()
                constructor.mockNewInstances(newInstanceInstrumentation)

                traceHandler.resetTrace()
                val stopWatch = StopWatch()
                val context = UtContext(utContext.classLoader, stopWatch)
                val concreteResult =
                    try {
                        ThreadBasedExecutor.threadLocal.invokeWithTimeout(timeout, stopWatch) {
                            withUtContext(context) {
                                delegateInstrumentation.invoke(clazz, methodSignature, params.map { it.value })
                            }
                        }?.getOrThrow() as? Result<*> ?: Result.failure<Any?>(TimeoutException("Timeout $timeout elapsed"))
                    } catch (e: Throwable) {
                        null
                    }
                val traceList = traceHandler.computeInstructionList()

                val cache = constructor.objectToModelCache
                val utCompositeModelStrategy =
                    ConstructOnlyUserClassesOrCachedObjectsStrategy(pathsToUserClasses, cache)
                val utModelConstructor = UtModelConstructor(cache, utCompositeModelStrategy)
                utModelConstructor.run {
                    val concreteUtModelResult = concreteResult?.fold({
                        UtExecutionSuccessConcrete(it)
                    }) {
                        sortOutException(it)
                    } ?: UtExecutionSuccessConcrete(null)
                    UtFuzzingConcreteExecutionResult(
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
        if (exception is AccessControlException) {
            return UtSandboxFailure(exception)
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
